package net.momirealms.craftengine.core.pack.bedrock.block;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.*;

public final class JavaBlockModelResolver {
    private final JavaResourcePack resources;
    private final JavaModelResolver models;
    private final Map<Key, JsonObject> definitions = new HashMap<>();
    private final Map<Key, JsonObject> vanillaDefinitions = new HashMap<>();
    private final Map<Key, Boolean> changedBlocks = new HashMap<>();
    private final Map<JsonElement, ModelVariant> preferredVariants = new IdentityHashMap<>();

    public JavaBlockModelResolver(JavaModelResources resources) {
        this.resources = resources.pack();
        this.models = resources.models();
    }

    public List<Part> resolve(BlockStateWrapper state) throws IOException {
        Key block = state.ownerId();
        JsonObject definition = this.definition(block, false);
        if (definition == null) {
            throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.missing", block.asString()));
        }
        String identifier = state.getAsString();
        List<JsonElement> selected = select(definition, identifier, properties(identifier), null);
        if (selected.isEmpty() && definition.has("variants")) {
            throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.no_match", identifier));
        }
        List<Part> parts = new ArrayList<>();
        for (JsonElement value : selected) {
            if (value.isJsonArray() && value.getAsJsonArray().size() != 1) {
                throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.weighted"));
            }
            parts.add(this.part(this.preferredVariant(value)));
        }
        return parts;
    }

    boolean mayHaveVanillaOverride(Key block) throws IOException {
        Boolean changed = this.changedBlocks.get(block);
        if (changed == null) {
            JsonObject vanilla = this.definition(block, true);
            changed = this.resources.hasCustomResource(blockStatePath(block)) || this.hasChangedModels(vanilla);
            this.changedBlocks.put(block, changed);
        }
        return changed;
    }

    @Nullable VanillaModelSelection selectVanillaOverride(BlockStateWrapper state, @Nullable String ignoredSelector) throws IOException {
        String id = state.getAsString();
        Map<String, String> properties = properties(id);
        List<JsonElement> vanilla = select(this.definition(state.ownerId(), true), id, properties, null);
        JsonObject definition = this.definition(state.ownerId(), false);
        if (definition == null) {
            return null;
        }
        List<JsonElement> effective = select(definition, id, properties, ignoredSelector);
        // 资源包可以只定义部分状态，未命中的状态继承原版方块状态定义
        if (effective.isEmpty()) {
            effective = vanilla;
        }
        boolean changed = effective.size() != vanilla.size();
        for (int i = 0; !changed && i < effective.size(); i++) {
            ModelVariant model = this.preferredVariant(effective.get(i));
            changed = !model.equals(this.preferredVariant(vanilla.get(i))) || this.models.hasChangedModel(model.id());
        }
        if (!changed) {
            return null;
        }
        List<ModelVariant> models = new ArrayList<>(effective.size());
        for (JsonElement value : effective) {
            models.add(this.preferredVariant(value));
        }
        return new VanillaModelSelection(models);
    }

    List<Part> resolveVanillaOverride(VanillaModelSelection selection) throws IOException {
        List<Part> parts = new ArrayList<>(selection.variants.size());
        for (ModelVariant variant : selection.variants) {
            parts.add(this.part(variant));
        }
        return parts;
    }

    public static final class VanillaModelSelection {
        private final List<ModelVariant> variants;

        private VanillaModelSelection(List<ModelVariant> variants) {
            this.variants = variants;
        }
    }

    private boolean hasChangedModels(@Nullable JsonObject definition) throws IOException {
        if (definition == null) {
            return false;
        }
        if (definition.has("variants")) {
            for (JsonElement value : definition.getAsJsonObject("variants").asMap().values()) {
                if (this.models.hasChangedModel(this.preferredVariant(value).id())) {
                    return true;
                }
            }
        }
        if (definition.has("multipart")) {
            for (JsonElement part : definition.getAsJsonArray("multipart")) {
                if (this.models.hasChangedModel(this.preferredVariant(part.getAsJsonObject().get("apply")).id())) {
                    return true;
                }
            }
        }
        return false;
    }

    private @Nullable JsonObject definition(Key block, boolean vanilla) throws IOException {
        Map<Key, JsonObject> cache = vanilla ? this.vanillaDefinitions : this.definitions;
        JsonObject definition = cache.get(block);
        if (definition == null && !cache.containsKey(block)) {
            String path = blockStatePath(block);
            definition = vanilla ? this.resources.readVanillaJson(path) : this.resources.readJson(path);
            cache.put(block, definition);
        }
        return definition;
    }

    private static String blockStatePath(Key block) {
        return "assets/" + block.namespace() + "/blockstates/" + block.value() + ".json";
    }

    private static Map<String, String> properties(String identifier) {
        int start = identifier.indexOf('[');
        if (start < 0) {
            return Map.of();
        }
        Map<String, String> properties = new HashMap<>();
        for (String property : identifier.substring(start + 1, identifier.length() - 1).split(",")) {
            String[] pair = property.split("=", 2);
            properties.put(pair[0], pair[1]);
        }
        return properties;
    }

    private static List<JsonElement> select(@Nullable JsonObject definition, String identifier, Map<String, String> properties,
                                            @Nullable String ignoredSelector) {
        if (definition == null) {
            return List.of();
        }
        if (definition.has("variants")) {
            JsonElement selected = null;
            for (Map.Entry<String, JsonElement> variant : definition.getAsJsonObject("variants").entrySet()) {
                if (variant.getKey().equals(ignoredSelector) || !matchesVariant(variant.getKey(), properties)) {
                    continue;
                }
                if (selected != null) {
                    throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.multiple", identifier));
                }
                selected = variant.getValue();
            }
            if (selected != null) {
                return List.of(selected);
            }
            if (!definition.has("multipart")) {
                return List.of();
            }
        }
        if (definition.has("multipart")) {
            List<JsonElement> parts = new ArrayList<>();
            for (JsonElement value : definition.getAsJsonArray("multipart")) {
                JsonObject part = value.getAsJsonObject();
                if (!part.has("when") || matches(part.getAsJsonObject("when"), properties)) {
                    parts.add(part.get("apply"));
                }
            }
            return parts;
        }
        throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.definition", identifier));
    }

    private static boolean matchesVariant(String selector, Map<String, String> properties) {
        if (selector.isEmpty()) {
            return true;
        }
        for (String property : selector.split(",")) {
            String[] pair = property.split("=", 2);
            if (!pair[1].equals(properties.get(pair[0]))) {
                return false;
            }
        }
        return true;
    }

    private static boolean matches(JsonObject condition, Map<String, String> properties) {
        for (Map.Entry<String, JsonElement> entry : condition.entrySet()) {
            if (entry.getKey().equals("OR") || entry.getKey().equals("AND")) {
                boolean and = entry.getKey().equals("AND");
                boolean result = and;
                for (JsonElement child : entry.getValue().getAsJsonArray()) {
                    boolean matched = matches(child.getAsJsonObject(), properties);
                    if (and != matched) {
                        result = matched;
                        break;
                    }
                }
                if (!result) {
                    return false;
                }
            } else {
                String actual = properties.get(entry.getKey());
                if (actual == null) {
                    throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.property", entry.getKey()));
                }
                String expected = entry.getValue().getAsString();
                boolean negate = expected.startsWith("!");
                boolean matched = false;
                for (String alternative : (negate ? expected.substring(1) : expected).split("\\|")) {
                    if (alternative.equals(actual)) {
                        matched = true;
                        break;
                    }
                }
                if (matched == negate) {
                    return false;
                }
            }
        }
        return true;
    }

    private ModelVariant preferredVariant(JsonElement value) {
        return this.preferredVariants.computeIfAbsent(value, JavaBlockModelResolver::readPreferredVariant);
    }

    private static ModelVariant readPreferredVariant(JsonElement value) {
        if (value.isJsonArray()) {
            if (value.getAsJsonArray().isEmpty()) {
                throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.weighted"));
            }
            int highestWeight = Integer.MIN_VALUE;
            JsonElement preferred = value.getAsJsonArray().get(0);
            for (JsonElement option : value.getAsJsonArray()) {
                JsonObject object = option.getAsJsonObject();
                int weight = object.has("weight") ? object.get("weight").getAsInt() : 1;
                if (weight > highestWeight) {
                    highestWeight = weight;
                    preferred = option;
                }
            }
            value = preferred;
        }
        JsonObject selected = value.getAsJsonObject();
        int x = selected.has("x") ? selected.get("x").getAsInt() : 0;
        int y = selected.has("y") ? selected.get("y").getAsInt() : 0;
        int z = selected.has("z") ? selected.get("z").getAsInt() : 0;
        boolean uvLock = selected.has("uvlock") && selected.get("uvlock").getAsBoolean();
        return new ModelVariant(Key.of(selected.get("model").getAsString()), x, y, z, uvLock);
    }

    private Part part(ModelVariant variant) throws IOException {
        int x = variant.x();
        int y = variant.y();
        int z = variant.z();
        if (x % 90 != 0 || y % 90 != 0 || z % 90 != 0) {
            throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.rotation"));
        }
        Matrix4f rotation = new Matrix4f().rotationZYX((float) Math.toRadians(-z), (float) Math.toRadians(-y), (float) Math.toRadians(-x));
        JavaModel model = this.models.resolve(variant.id());
        if (model.kind() != JavaModel.GeometryKind.ELEMENTS) {
            throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.model_renderer", model.id().asString()));
        }
        Matrix4f transform = new Matrix4f().translation(0.5f, 0.5f, 0.5f).mul(rotation).translate(-0.5f, -0.5f, -0.5f);
        return new Part(variant.uvLock() ? lockUv(model, rotation) : model, transform);
    }

    private record ModelVariant(Key id, int x, int y, int z, boolean uvLock) {
    }

    private static JavaModel lockUv(JavaModel model, Matrix4fc rotation) {
        List<JavaModel.Element> elements = new ArrayList<>(model.elements().size());
        for (JavaModel.Element element : model.elements()) {
            Map<Direction, JavaModel.Face> faces = new EnumMap<>(Direction.class);
            for (Map.Entry<Direction, JavaModel.Face> entry : element.faces().entrySet()) {
                faces.put(entry.getKey(), lockUv(entry.getValue(), entry.getKey(), rotation));
            }
            elements.add(new JavaModel.Element(element.from(), element.to(), faces, element.rotation()));
        }
        return new JavaModel(model.id(), model.kind(), model.textures(), elements, model.display(), model.guiLight());
    }

    private static JavaModel.Face lockUv(JavaModel.Face face, Direction side, Matrix4fc rotation) {
        // Java's UV lock maps the rotated face back into its own face coordinates, around the texture center.
        Matrix4f faceAction = new Matrix4f(rotation).mul(faceFrame(side));
        Vector3f normal = faceAction.transformDirection(new Vector3f(0, 0, 1));
        Direction target = Math.abs(normal.x) > 0.5f ? (normal.x > 0 ? Direction.EAST : Direction.WEST)
                : (Math.abs(normal.y) > 0.5f ? (normal.y > 0 ? Direction.UP : Direction.DOWN)
                : (normal.z > 0 ? Direction.SOUTH : Direction.NORTH));
        Matrix4f inverse = faceFrame(target).invert().mul(faceAction).invert();
        Vector3f[] corners = new Vector3f[4];
        JavaModel.Uv uv = face.uv();
        for (int i = 0; i < 4; i++) {
            int index = (i + face.rotation() / 90) % 4;
            float u = index < 2 ? uv.u1() : uv.u2();
            float v = index == 0 || index == 3 ? uv.v1() : uv.v2();
            corners[i] = inverse.transformPosition(new Vector3f(u - 8, v - 8, 0)).add(8, 8, 0);
        }
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            Vector3f first = corners[(4 - quadrant) % 4];
            Vector3f second = corners[(5 - quadrant) % 4];
            Vector3f opposite = corners[(6 - quadrant) % 4];
            if (Math.abs(first.x - second.x) < 0.0001f && Math.abs(second.y - opposite.y) < 0.0001f) {
                return new JavaModel.Face(face.texture(), new JavaModel.Uv(first.x, first.y, opposite.x, opposite.y), quadrant * 90, face.tintIndex());
            }
        }
        throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.uv"));
    }

    private static Matrix4f faceFrame(Direction side) {
        return switch (side) {
            case SOUTH -> new Matrix4f();
            case EAST -> new Matrix4f().rotationY((float) (Math.PI / 2));
            case WEST -> new Matrix4f().rotationY((float) (-Math.PI / 2));
            case NORTH -> new Matrix4f().rotationY((float) Math.PI);
            case UP -> new Matrix4f().rotationX((float) (-Math.PI / 2));
            case DOWN -> new Matrix4f().rotationX((float) (Math.PI / 2));
        };
    }

    public record Part(JavaModel model, Matrix4fc transform) {
    }
}
