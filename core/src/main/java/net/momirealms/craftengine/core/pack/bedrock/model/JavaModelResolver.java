package net.momirealms.craftengine.core.pack.bedrock.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.pack.Identifier;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.model.ModelJsonKeys;
import net.momirealms.craftengine.core.pack.model.generation.GuiLight;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.ModelUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static net.momirealms.craftengine.core.pack.bedrock.model.JavaModel.*;

public final class JavaModelResolver {
    private static final Key BUILTIN_GENERATED = Key.minecraft("builtin/generated");
    private static final Key BUILTIN_ENTITY = Key.minecraft("builtin/entity");
    private static final DisplayPosition[] DISPLAY_POSITIONS = DisplayPosition.values();

    private final JavaResourcePack pack;
    private final Map<Key, InheritedModel> inheritedModels = new HashMap<>();
    private final Map<Key, JavaModel> models = new HashMap<>();

    public JavaModelResolver(JavaResourcePack pack) {
        this.pack = pack;
    }

    public JavaModel resolve(Key id) throws IOException {
        JavaModel cached = this.models.get(id);
        if (cached != null) {
            return cached;
        }
        try {
            InheritedModel inherited = this.inherit(id, new LinkedHashSet<>());
            Map<String, Texture> textures = new LinkedHashMap<>();
            for (String slot : inherited.textures().keySet()) {
                textures.put(slot, this.resolveTexture("#" + slot, inherited.textures()));
            }
            List<Element> elements = new ArrayList<>();
            for (JsonElement element : inherited.elements()) {
                elements.add(this.readElement(element.getAsJsonObject(), inherited.textures()));
            }
            Map<DisplayPosition, Transform> display = new EnumMap<>(DisplayPosition.class);
            for (DisplayPosition position : DISPLAY_POSITIONS) {
                display.put(position, inherited.display().getOrDefault(position, Transform.IDENTITY));
            }
            JavaModel result = new JavaModel(id, inherited.kind(), elements, textures, display, inherited.ambientOcclusion(), inherited.guiLight());
            this.models.put(id, result);
            return result;
        } catch (IOException | RuntimeException exception) {
            throw new IOException("Cannot resolve Java model " + id + ": " + exception.getMessage(), exception);
        }
    }

    private InheritedModel inherit(Key id, Set<Key> chain) throws IOException {
        InheritedModel cached = this.inheritedModels.get(id);
        if (cached != null) {
            return cached;
        }
        if (!chain.add(id)) {
            throw new IOException("Cyclic model parents: " + chain + " -> " + id);
        }
        try {
            if (id.equals(BUILTIN_GENERATED)) {
                Map<String, JsonElement> textures = Map.of(ModelJsonKeys.PARTICLE, new JsonPrimitive("#layer0"));
                return new InheritedModel(GeometryKind.GENERATED, new JsonArray(), textures, Map.of(), true, GuiLight.FRONT);
            }
            if (id.equals(BUILTIN_ENTITY)) {
                return new InheritedModel(GeometryKind.ENTITY, new JsonArray(), Map.of(), Map.of(), true, GuiLight.SIDE);
            }

            JsonObject json = this.pack.readModel(id);
            InheritedModel parent = null;
            if (json.has(ModelJsonKeys.PARENT)) {
                String parentId = json.get(ModelJsonKeys.PARENT).getAsString();
                if (!parentId.isEmpty()) {
                    parent = this.inherit(Key.of(parentId), chain);
                }
            }

            Map<String, JsonElement> textures = new LinkedHashMap<>(8);
            Map<DisplayPosition, Transform> display = new EnumMap<>(DisplayPosition.class);
            JsonArray elements = null;
            GeometryKind kind = GeometryKind.ELEMENTS;
            boolean ambientOcclusion = true;
            GuiLight guiLight = GuiLight.SIDE;

            if (parent != null) {
                textures.putAll(parent.textures());
                display.putAll(parent.display());
                elements = parent.elements();
                kind = parent.kind();
                ambientOcclusion = parent.ambientOcclusion();
                guiLight = parent.guiLight();
            }

            // Keep texture references unresolved until all child overrides have been applied.
            if (json.has(ModelJsonKeys.TEXTURES)) {
                textures.putAll(json.getAsJsonObject(ModelJsonKeys.TEXTURES).asMap());
            }
            if (json.has(ModelJsonKeys.DISPLAY)) {
                display.putAll(this.readDisplay(json.getAsJsonObject(ModelJsonKeys.DISPLAY)));
            }
            if (json.has(ModelJsonKeys.ELEMENTS)) {
                kind = GeometryKind.ELEMENTS;
                JsonArray ownElements = json.getAsJsonArray(ModelJsonKeys.ELEMENTS);
                if (ownElements != null) {
                    elements = ownElements;
                }
            }
            if (json.has(ModelJsonKeys.AMBIENT_OCCLUSION)) {
                ambientOcclusion = json.get(ModelJsonKeys.AMBIENT_OCCLUSION).getAsBoolean();
            }
            if (json.has(ModelJsonKeys.GUI_LIGHT)) {
                guiLight = GuiLight.valueOf(json.get(ModelJsonKeys.GUI_LIGHT).getAsString().toUpperCase(Locale.ROOT));
            }
            if (elements == null) {
                elements = new JsonArray();
            }

            InheritedModel result = new InheritedModel(kind, elements, textures, display, ambientOcclusion, guiLight);
            this.inheritedModels.put(id, result);
            return result;
        } catch (IOException | RuntimeException exception) {
            throw new IOException("Model " + id + ": " + exception.getMessage(), exception);
        } finally {
            chain.remove(id);
        }
    }

    private Texture resolveTexture(String reference, Map<String, JsonElement> textures) {
        Set<String> chain = new LinkedHashSet<>();
        while (reference.startsWith("#")) {
            String slot = reference.substring(1);
            if (!chain.add(slot)) {
                throw new IllegalArgumentException("Cyclic texture references: " + chain + " -> " + slot);
            }
            JsonElement texture = textures.get(slot);
            if (texture == null) {
                throw new IllegalArgumentException("Missing texture slot #" + slot + " via " + chain);
            }
            if (texture instanceof JsonObject object) {
                return new Texture(this.readTextureIdentifier(object.get(ModelJsonKeys.SPRITE)),
                        object.has(ModelJsonKeys.ATLAS) ? this.readTextureIdentifier(object.get(ModelJsonKeys.ATLAS)) : null,
                        object.has(ModelJsonKeys.FORCE_TRANSLUCENT) && object.get(ModelJsonKeys.FORCE_TRANSLUCENT).getAsBoolean());
            }
            if (!(texture instanceof JsonPrimitive primitive) || !primitive.isString()) {
                throw new IllegalArgumentException("Expected texture string or object for #" + slot);
            }
            reference = texture.getAsString();
        }
        if (!Identifier.isValid(reference) || reference.isEmpty()) {
            throw new IllegalArgumentException("Invalid texture identifier: " + reference);
        }
        return new Texture(Key.of(reference), null, false);
    }

    private Key readTextureIdentifier(JsonElement json) {
        if (!(json instanceof JsonPrimitive primitive) || !primitive.isString()) {
            throw new IllegalArgumentException("Expected texture identifier string: " + json);
        }
        String identifier = primitive.getAsString();
        if (identifier.isEmpty() || !Identifier.isValid(identifier)) {
            throw new IllegalArgumentException("Invalid texture identifier: " + identifier);
        }
        return Key.of(identifier);
    }

    private Element readElement(JsonObject json, Map<String, JsonElement> textures) {
        Vector from = this.readPosition(json, ModelJsonKeys.FROM);
        Vector to = this.readPosition(json, ModelJsonKeys.TO);
        Map<Direction, Face> faces = new EnumMap<>(Direction.class);
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject(ModelJsonKeys.FACES).entrySet()) {
            Direction direction = Direction.valueOf(entry.getKey().toUpperCase(Locale.ROOT));
            JsonObject face = entry.getValue().getAsJsonObject();
            float[] uv = face.has(ModelJsonKeys.UV) ? this.readFloats(face.getAsJsonArray(ModelJsonKeys.UV), 4)
                    : ModelUtils.defaultUv(entry.getKey(), new float[]{from.x(), from.y(), from.z()}, new float[]{to.x(), to.y(), to.z()});
            int rotation = face.has(ModelJsonKeys.ROTATION) ? face.get(ModelJsonKeys.ROTATION).getAsInt() : 0;
            if (rotation < 0 || rotation > 270 || rotation % 90 != 0) {
                throw new IllegalArgumentException("Invalid face rotation " + rotation);
            }
            String cull = face.has(ModelJsonKeys.CULL_FACE) ? face.get(ModelJsonKeys.CULL_FACE).getAsString() : "";
            faces.put(direction, new Face(this.resolveTexture(face.get(ModelJsonKeys.TEXTURE).getAsString(), textures),
                    new Uv(uv[0], uv[1], uv[2], uv[3]), rotation,
                    cull.isEmpty() ? null : Direction.valueOf(cull.toUpperCase(Locale.ROOT)),
                    face.has(ModelJsonKeys.TINT_INDEX) ? face.get(ModelJsonKeys.TINT_INDEX).getAsInt() : -1));
        }
        boolean shade = true;
        Direction shadeDirectionOverride = null;
        if (this.pack.version().isAtOrAbove(MinecraftVersion.V26_3)) {
            if (json.has(ModelJsonKeys.SHADE_DIRECTION_OVERRIDE)) {
                shadeDirectionOverride = Direction.valueOf(json.get(ModelJsonKeys.SHADE_DIRECTION_OVERRIDE).getAsString().toUpperCase(Locale.ROOT));
            }
        } else if (json.has(ModelJsonKeys.SHADE)) {
            shade = json.get(ModelJsonKeys.SHADE).getAsBoolean();
        }
        return new Element(from, to, faces,
                json.has(ModelJsonKeys.ROTATION) ? this.readRotation(json.getAsJsonObject(ModelJsonKeys.ROTATION)) : null,
                shade, shadeDirectionOverride, this.readLightEmission(json));
    }

    private Vector readPosition(JsonObject json, String field) {
        Vector position = this.readVector(json.getAsJsonArray(field));
        if (position.x() < -16 || position.x() > 32 || position.y() < -16 || position.y() > 32 || position.z() < -16 || position.z() > 32) {
            throw new IllegalArgumentException("Element " + field + " must be within [-16, 32]: " + position);
        }
        return position;
    }

    private int readLightEmission(JsonObject json) {
        JsonElement value = json.get(ModelJsonKeys.LIGHT_EMISSION);
        if (value == null) {
            return 0;
        }
        if (!(value instanceof JsonPrimitive primitive) || !primitive.isNumber()) {
            throw new IllegalArgumentException("Expected integer light_emission within [0, 15]: " + value);
        }
        double number = primitive.getAsDouble();
        if (!Double.isFinite(number) || number < 0 || number > 15 || number != Math.floor(number)) {
            throw new IllegalArgumentException("Expected integer light_emission within [0, 15]: " + value);
        }
        return (int) number;
    }

    private Rotation readRotation(JsonObject json) {
        Vector origin = this.readVector(json.getAsJsonArray(ModelJsonKeys.ORIGIN));
        Direction.Axis axis = null;
        Vector angles;
        if (json.has(ModelJsonKeys.AXIS) || json.has(ModelJsonKeys.ANGLE)) {
            axis = Direction.Axis.valueOf(json.get(ModelJsonKeys.AXIS).getAsString().toUpperCase(Locale.ROOT));
            float angle = this.readFloat(json.get(ModelJsonKeys.ANGLE));
            if (this.pack.version().isBelow(MinecraftVersion.V1_21_11) && (angle < -45 || angle > 45)) {
                throw new IllegalArgumentException("Element rotation angle must be within [-45, 45] before Java 1.21.11: " + angle);
            }
            if (this.pack.version().isBelow(MinecraftVersion.V1_21_6) && angle % 22.5f != 0) {
                throw new IllegalArgumentException("Element rotation angle must be a multiple of 22.5 before Java 1.21.6: " + angle);
            }
            angles = switch (axis) {
                case X -> new Vector(angle, 0, 0);
                case Y -> new Vector(0, angle, 0);
                case Z -> new Vector(0, 0, angle);
            };
        } else {
            if (this.pack.version().isBelow(MinecraftVersion.V1_21_11)) {
                throw new IllegalArgumentException("Euler element rotations require Java 1.21.11 or newer");
            }
            if (!json.has(ModelJsonKeys.X) && !json.has(ModelJsonKeys.Y) && !json.has(ModelJsonKeys.Z)) {
                throw new IllegalArgumentException("Missing element rotation angles");
            }
            angles = new Vector(json.has(ModelJsonKeys.X) ? this.readFloat(json.get(ModelJsonKeys.X)) : 0,
                    json.has(ModelJsonKeys.Y) ? this.readFloat(json.get(ModelJsonKeys.Y)) : 0, json.has(ModelJsonKeys.Z) ? this.readFloat(json.get(ModelJsonKeys.Z)) : 0);
        }
        return new Rotation(origin, angles, axis, json.has(ModelJsonKeys.RESCALE) && json.get(ModelJsonKeys.RESCALE).getAsBoolean());
    }

    private Map<DisplayPosition, Transform> readDisplay(JsonObject json) {
        Map<DisplayPosition, Transform> display = new EnumMap<>(DisplayPosition.class);
        for (DisplayPosition position : DISPLAY_POSITIONS) {
            String name = position.name().toLowerCase(Locale.ROOT);
            if (json.has(name)) {
                JsonObject transform = json.getAsJsonObject(name);
                display.put(position, new Transform(
                        transform.has(ModelJsonKeys.ROTATION) ? this.readVector(transform.getAsJsonArray(ModelJsonKeys.ROTATION)) : Vector.ZERO,
                        transform.has(ModelJsonKeys.TRANSLATION) ? this.readVector(transform.getAsJsonArray(ModelJsonKeys.TRANSLATION), 80) : Vector.ZERO,
                        transform.has(ModelJsonKeys.SCALE) ? this.readVector(transform.getAsJsonArray(ModelJsonKeys.SCALE), 4) : Vector.ONE));
            }
        }
        // Java applies the right-hand fallback inside each model, before inheriting parent transforms.
        this.copyRightHand(display, DisplayPosition.FIRSTPERSON_RIGHTHAND, DisplayPosition.FIRSTPERSON_LEFTHAND);
        this.copyRightHand(display, DisplayPosition.THIRDPERSON_RIGHTHAND, DisplayPosition.THIRDPERSON_LEFTHAND);
        return display;
    }

    private void copyRightHand(Map<DisplayPosition, Transform> display, DisplayPosition right, DisplayPosition left) {
        Transform transform = display.get(right);
        if (transform != null) {
            display.putIfAbsent(left, transform);
        }
    }

    private Vector readVector(JsonArray array) {
        return this.readVector(array, Float.POSITIVE_INFINITY);
    }

    private Vector readVector(JsonArray array, float limit) {
        if (array == null || array.size() != 3) {
            throw new IllegalArgumentException("Expected 3 numeric values: " + array);
        }
        return new Vector(Math.clamp(this.readFloat(array.get(0)), -limit, limit),
                Math.clamp(this.readFloat(array.get(1)), -limit, limit),
                Math.clamp(this.readFloat(array.get(2)), -limit, limit));
    }

    private float[] readFloats(JsonArray array, int size) {
        if (array == null || array.size() != size) {
            throw new IllegalArgumentException("Expected " + size + " numeric values: " + array);
        }
        float[] values = new float[size];
        for (int i = 0; i < size; i++) {
            values[i] = this.readFloat(array.get(i));
        }
        return values;
    }

    private float readFloat(JsonElement value) {
        float number = value.getAsFloat();
        if (!Float.isFinite(number)) {
            throw new IllegalArgumentException("Non-finite model number: " + value);
        }
        return number;
    }

    private record InheritedModel(GeometryKind kind,
                                  JsonArray elements,
                                  Map<String, JsonElement> textures,
                                  Map<DisplayPosition, Transform> display,
                                  boolean ambientOcclusion,
                                  GuiLight guiLight) {
    }
}
