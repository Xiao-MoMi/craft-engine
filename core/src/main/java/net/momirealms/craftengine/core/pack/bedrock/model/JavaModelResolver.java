package net.momirealms.craftengine.core.pack.bedrock.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemDefinitionBinding;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.model.generation.GuiLight;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.ModelUtils;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static net.momirealms.craftengine.core.pack.AbstractPackManager.BUILTIN_ENTITY;
import static net.momirealms.craftengine.core.pack.AbstractPackManager.BUILTIN_GENERATED;
import static net.momirealms.craftengine.core.pack.bedrock.model.JavaModel.*;
import static net.momirealms.craftengine.core.pack.model.ModelJsonKeys.*;

public final class JavaModelResolver {
    private static final DisplayPosition[] DISPLAY_POSITIONS = DisplayPosition.values();
    private static final InheritedModel GENERATED_MODEL = new InheritedModel(GeometryKind.GENERATED, new JsonArray(), Map.of(PARTICLE, new JsonPrimitive("#layer0")), Map.of(), GuiLight.FRONT);
    private static final InheritedModel ENTITY_MODEL = new InheritedModel(GeometryKind.ENTITY, new JsonArray(), Map.of(), Map.of(), GuiLight.SIDE);
    private final JavaResourcePack pack;
    private final JavaPackTextureSource textures;
    private final Map<Key, InheritedModel> inheritedModels = new HashMap<>();
    private final Map<Key, InheritedModel> vanillaInheritedModels = new HashMap<>();
    private final Map<Key, JavaModel> models = new HashMap<>();
    private JavaModel vanillaIconModel;

    public JavaModelResolver(JavaResourcePack pack) {
        this(pack, new JavaPackTextureSource(pack));
    }

    public JavaModelResolver(JavaResourcePack pack, JavaPackTextureSource textures) {
        this.pack = pack;
        this.textures = textures;
    }

    public JavaModel resolve(Key model) throws IOException {
        JavaModel resolved = this.models.get(model);
        if (resolved == null) {
            resolved = this.resolve(model, false);
            this.models.put(model, resolved);
        }
        return resolved;
    }

    // 看看整个物品定义是否包含3d模型或者是带动态贴图的2d图标
    public RenderType classify(ItemDefinitionBinding definition) throws IOException {
        return this.classify(definition.rules());
    }

    private RenderType classify(List<ItemBindingRule> rules) throws IOException {
        boolean block = false;
        boolean unknown = false;
        // 任意分支含三维模型或动态贴图，整个定义都使用展示方块。
        for (ItemBindingRule rule : rules) {
            switch (rule.target()) {
                case ItemRenderTarget.Model model -> {
                    JavaModel resolved = this.resolve(model.model());
                    if (resolved.kind() != GeometryKind.GENERATED) {
                        block = true;
                    } else {
                        for (Texture layer : this.resolveLayers(resolved)) {
                            TextureData data = this.textures.read(layer, TextureUsage.ITEM);
                            block |= data.animation() != null;
                        }
                    }
                }
                case ItemRenderTarget.Special ignored -> block = true;
                case ItemRenderTarget.Composite composite -> {
                    RenderType type = this.classify(composite.children());
                    block |= type == RenderType.BLOCK;
                    unknown |= type == RenderType.UNKNOWN;
                }
                case ItemRenderTarget.Builtin builtin -> unknown |= builtin != ItemRenderTarget.Builtin.EMPTY;
            }
        }
        return block ? RenderType.BLOCK : (unknown ? RenderType.UNKNOWN : RenderType.ITEM);
    }

    public enum RenderType {
        ITEM, BLOCK, UNKNOWN
    }

    private JavaModel resolve(Key model, boolean vanilla) throws IOException {
        InheritedModel inherited = this.inherit(model, vanilla, new HashSet<>(4));
        List<Element> elements = new ArrayList<>(inherited.elements().size());
        for (JsonElement element : inherited.elements()) {
            elements.add(this.readElement(model, element.getAsJsonObject(), inherited.textures()));
        }
        Map<DisplayPosition, Transform> display = new EnumMap<>(DisplayPosition.class);
        display.putAll(inherited.display());
        for (DisplayPosition position : DISPLAY_POSITIONS) {
            display.putIfAbsent(position, Transform.IDENTITY);
        }
        return new JavaModel(model, inherited.kind(), inherited.textures(), elements, display, inherited.guiLight());
    }

    private InheritedModel inherit(Key model, boolean vanilla, Set<Key> parents) throws IOException {
        if (model.equals(BUILTIN_GENERATED)) {
            return GENERATED_MODEL;
        }
        if (model.equals(BUILTIN_ENTITY)) {
            return ENTITY_MODEL;
        }
        Map<Key, InheritedModel> cache = vanilla ? this.vanillaInheritedModels : this.inheritedModels;
        InheritedModel cached = cache.get(model);
        if (cached != null) {
            return cached;
        }
        if (!parents.add(model)) {
            throw new IOException("Cyclic model parent " + model);
        }
        try {
            JsonObject json = vanilla ? this.pack.readVanillaModel(model) : this.pack.readModel(model);
            String parentId = json.has(PARENT) ? json.get(PARENT).getAsString() : "";
            InheritedModel parent = parentId.isEmpty() ? null : this.inherit(Key.of(parentId), vanilla, parents);
            Map<String, JsonElement> textures = new Object2ObjectArrayMap<>();
            Map<DisplayPosition, Transform> display = new EnumMap<>(DisplayPosition.class);
            GeometryKind kind = GeometryKind.ELEMENTS;
            JsonArray elements = null;
            GuiLight light = GuiLight.SIDE;
            if (parent != null) {
                textures.putAll(parent.textures());
                display.putAll(parent.display());
                kind = parent.kind();
                elements = parent.elements();
                light = parent.guiLight();
            }
            if (json.has(TEXTURES)) {
                // 父链缓存保留 # 引用，等子模型覆写完成后再解析贴图。
                textures.putAll(json.getAsJsonObject(TEXTURES).asMap());
            }
            if (json.has(ELEMENTS)) {
                kind = GeometryKind.ELEMENTS;
                elements = json.getAsJsonArray(ELEMENTS);
            }
            if (json.has(DISPLAY)) {
                display.putAll(this.readDisplay(json.getAsJsonObject(DISPLAY)));
            }
            if (json.has(GUI_LIGHT)) {
                light = GuiLight.valueOf(json.get(GUI_LIGHT).getAsString().toUpperCase(Locale.ROOT));
            }
            if (elements == null) {
                elements = new JsonArray();
            }
            InheritedModel result = new InheritedModel(kind, elements, textures, display, light);
            cache.put(model, result);
            return result;
        } finally {
            parents.remove(model);
        }
    }

    public Texture resolveTexture(Key model, String slot) throws IOException {
        return this.resolveTexture(model, slot, this.resolve(model).textures());
    }

    @Nullable
    public List<Texture> resolveIconLayers(JavaModel model) throws IOException {
        if (model.kind() != GeometryKind.GENERATED) {
            return null;
        }
        if (this.vanillaIconModel == null) {
            this.vanillaIconModel = this.resolve(Key.minecraft("item/generated"), true);
        }
        if (model.guiLight() != this.vanillaIconModel.guiLight()) {
            return null;
        }
        for (Map.Entry<DisplayPosition, Transform> entry : model.display().entrySet()) {
            boolean handheld = switch (entry.getKey()) {
                case FIRSTPERSON_RIGHTHAND, FIRSTPERSON_LEFTHAND, THIRDPERSON_RIGHTHAND, THIRDPERSON_LEFTHAND -> true;
                default -> false;
            };
            if (!handheld && !entry.getValue().equals(this.vanillaIconModel.display().get(entry.getKey()))) {
                return null;
            }
        }
        List<Texture> layers = this.resolveLayers(model);
        for (Texture texture : layers) {
            if (texture.forceTranslucent()) {
                return null;
            }
        }
        return layers;
    }

    public List<Texture> resolveLayers(JavaModel model) throws IOException {
        List<Texture> layers = new ArrayList<>();
        for (int layer = 0; layer < 5; layer++) {
            String slot = "layer" + layer;
            if (!model.textures().containsKey(slot)) {
                break;
            }
            Texture texture = this.resolveTexture(model.id(), slot, model.textures());
            layers.add(texture);
        }
        return layers;
    }

    private Element readElement(Key model, JsonObject json, Map<String, JsonElement> textures) throws IOException {
        Vector3f from = VectorUtils.vector3f(json.getAsJsonArray(FROM));
        Vector3f to = VectorUtils.vector3f(json.getAsJsonArray(TO));
        Map<Direction, Face> faces = new EnumMap<>(Direction.class);
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject(FACES).entrySet()) {
            JsonObject face = entry.getValue().getAsJsonObject();
            JsonArray uv = face.getAsJsonArray(UV);
            Uv coordinates;
            if (uv == null) {
                float[] defaults = ModelUtils.defaultUv(entry.getKey(), from, to);
                coordinates = new Uv(defaults[0], defaults[1], defaults[2], defaults[3]);
            } else {
                coordinates = new Uv(uv.get(0).getAsFloat(), uv.get(1).getAsFloat(), uv.get(2).getAsFloat(), uv.get(3).getAsFloat());
            }
            String reference = face.get(TEXTURE).getAsString();
            Texture texture;
            if (reference.startsWith("#")) {
                texture = this.resolveTexture(model, reference.substring(1), textures);
            } else {
                texture = new Texture(Key.of(reference), false);
            }
            faces.put(Direction.valueOf(entry.getKey().toUpperCase(Locale.ROOT)),
                    new Face(texture, coordinates, face.has(ROTATION) ? face.get(ROTATION).getAsInt() : 0,
                            face.has(TINT_INDEX) ? face.get(TINT_INDEX).getAsInt() : -1));
        }
        return new Element(from, to, faces, json.has(ROTATION) ? this.readRotation(json.getAsJsonObject(ROTATION)) : null);
    }

    private Rotation readRotation(JsonObject json) throws IOException {
        Vector3f angles;
        if (json.has(AXIS)) {
            float angle = json.get(ANGLE).getAsFloat();
            angles = switch (json.get(AXIS).getAsString()) {
                case "x" -> new Vector3f(angle, 0, 0);
                case "y" -> new Vector3f(0, angle, 0);
                case "z" -> new Vector3f(0, 0, angle);
                default -> throw new IOException("Unknown element rotation axis " + json.get(AXIS));
            };
        } else {
            if (this.pack.version().isBelow(MinecraftVersion.V1_21_11)) {
                throw new IOException("Euler element rotations require Java 1.21.11 or newer");
            }
            angles = new Vector3f(json.has(X) ? json.get(X).getAsFloat() : 0,
                    json.has(Y) ? json.get(Y).getAsFloat() : 0, json.has(Z) ? json.get(Z).getAsFloat() : 0);
        }
        return new Rotation(VectorUtils.vector3f(json.getAsJsonArray(ORIGIN)), angles,
                json.has(RESCALE) && json.get(RESCALE).getAsBoolean());
    }

    private Map<DisplayPosition, Transform> readDisplay(JsonObject json) {
        Map<DisplayPosition, Transform> display = new EnumMap<>(DisplayPosition.class);
        for (DisplayPosition position : DISPLAY_POSITIONS) {
            String name = position.name().toLowerCase(Locale.ROOT);
            if (json.has(name)) {
                display.put(position, this.readTransform(json.getAsJsonObject(name)));
            }
        }
        // 左手回退发生在当前模型内，之后才继承父级变换。
        Transform firstPerson = display.get(DisplayPosition.FIRSTPERSON_RIGHTHAND);
        if (firstPerson != null) {
            display.putIfAbsent(DisplayPosition.FIRSTPERSON_LEFTHAND, firstPerson);
        }
        Transform thirdPerson = display.get(DisplayPosition.THIRDPERSON_RIGHTHAND);
        if (thirdPerson != null) {
            display.putIfAbsent(DisplayPosition.THIRDPERSON_LEFTHAND, thirdPerson);
        }
        return display;
    }

    private Transform readTransform(JsonObject json) {
        Vector3f translation = VectorUtils.vector3f(json.getAsJsonArray(TRANSLATION));
        translation.set(Math.clamp(translation.x, -80, 80), Math.clamp(translation.y, -80, 80), Math.clamp(translation.z, -80, 80));
        Vector3f scale = json.has(SCALE) ? VectorUtils.vector3f(json.getAsJsonArray(SCALE)) : new Vector3f(1);
        scale.set(Math.clamp(scale.x, -4, 4), Math.clamp(scale.y, -4, 4), Math.clamp(scale.z, -4, 4));
        return new Transform(VectorUtils.vector3f(json.getAsJsonArray(ROTATION)), translation, scale);
    }

    private Texture resolveTexture(Key model, String slot, Map<String, JsonElement> textures) throws IOException {
        Set<String> references = new HashSet<>();
        String currentSlot = slot;
        while (references.add(currentSlot)) {
            JsonElement texture = textures.get(currentSlot);
            if (texture == null) {
                throw new IOException("Missing texture #" + currentSlot + " in model " + model);
            }
            if (texture.isJsonPrimitive() && texture.getAsString().startsWith("#")) {
                currentSlot = texture.getAsString().substring(1);
                continue;
            }
            if (texture.isJsonObject()) {
                JsonObject object = texture.getAsJsonObject();
                Key sprite = Key.of(object.get(SPRITE).getAsString());
                boolean forceTranslucent = object.has(FORCE_TRANSLUCENT) && object.get(FORCE_TRANSLUCENT).getAsBoolean();
                return new Texture(sprite, forceTranslucent);
            }
            return new Texture(Key.of(texture.getAsString()), false);
        }
        throw new IOException("Cyclic texture reference #" + currentSlot + " in model " + model);
    }

    private record InheritedModel(GeometryKind kind,
                                  JsonArray elements,
                                  Map<String, JsonElement> textures,
                                  Map<DisplayPosition, Transform> display,
                                  GuiLight guiLight) {
    }
}
