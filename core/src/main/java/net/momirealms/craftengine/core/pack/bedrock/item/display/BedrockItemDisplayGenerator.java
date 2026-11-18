package net.momirealms.craftengine.core.pack.bedrock.item.display;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.pack.bedrock.BedrockResourceException;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemResourceGenerator;
import net.momirealms.craftengine.core.pack.bedrock.item.GeneratedItemVariant;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelTransform;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
import net.momirealms.craftengine.core.pack.model.definition.Transformation;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tint;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tints;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.ID_PREFIX;

public final class BedrockItemDisplayGenerator {
    private static final Key TRIDENT_ICON = Key.minecraft("item/trident");
    private static final ItemDisplayContext[] DISPLAY_CONTEXTS = ItemDisplayContext.values();
    private static final DisplayPosition[] DISPLAY_POSITIONS = displayPositions();
    private static final Transformation SPECIAL_MODEL_FLIP = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1, -1, -1), new Quaternionf());

    private BedrockItemDisplayGenerator() {
    }

    private static DisplayPosition[] displayPositions() {
        DisplayPosition[] positions = new DisplayPosition[DISPLAY_CONTEXTS.length];
        for (ItemDisplayContext context : DISPLAY_CONTEXTS) {
            if (context != ItemDisplayContext.NONE) {
                positions[context.ordinal()] = DisplayPosition.valueOf(context.serializedName().toUpperCase(Locale.ROOT));
            }
        }
        return positions;
    }

    public static JsonObject write(JavaModelResources resources,
                                   BedrockItemResourceGenerator.Result generated,
                                   Path root) throws IOException {
        String name = "item_display";
        BedrockDisplayAssets assets = new BedrockDisplayAssets(resources, root, name);
        Map<List<Part>, Integer> indices = new HashMap<>();
        Map<List<Part>, Integer> meshes = new HashMap<>();
        JsonArray appearances = new JsonArray();
        appearances.add(new JsonArray());
        JsonObject items = new JsonObject();
        for (List<GeneratedItemVariant> variants : generated.variants().values()) {
            for (GeneratedItemVariant variant : variants) {
                if (!variant.generated() || variant.worldView() != null) {
                    continue;
                }
                JsonArray contexts = new JsonArray();
                boolean mapped = false;
                Set<String> reported = new HashSet<>();
                for (ItemDisplayContext context : DISPLAY_CONTEXTS) {
                    int shape = 0;
                    ItemBindingRule rule = variant.displayModels().get(context);
                    if (rule != null) {
                        // 每种展示上下文先选 Java 分支，再应用该模型的 display；NONE 使用恒等变换。
                        try {
                            List<Part> parts = parts(resources, rule, context);
                            if (!parts.isEmpty() || rule.target() == ItemRenderTarget.Builtin.EMPTY || (rule.target() instanceof ItemRenderTarget.Composite composite && composite.parts().isEmpty())) {
                                Integer existing = indices.get(parts);
                                if (existing == null) {
                                    existing = appearance(assets, parts, meshes, appearances);
                                    indices.put(parts, existing);
                                }
                                shape = existing;
                            }
                        } catch (BedrockResourceException e) {
                            String reason = e.getLocalizedMessage();
                            if (reported.add(reason)) {
                                TranslationManager.warn("resource_pack.bedrock.item.display_skipped", variant.id().asString(), context.serializedName(), reason);
                            }
                        }
                    }
                    contexts.add(shape);
                    mapped |= shape != 0;
                }
                if (mapped) {
                    items.add(variant.id().asString(), contexts);
                }
            }
        }
        JsonObject result = new JsonObject();
        result.add("vanilla", vanilla(resources, assets, indices, meshes, appearances));
        result.add("blocks", BedrockBlockDisplayGenerator.write(resources, assets, meshes, appearances, Config.bedrockVanillaDisplayBlocks()));
        result.addProperty("vanilla_identifier", ID_PREFIX + name + "_held");
        result.addProperty("convert_text", Config.bedrockConvertText());
        assets.finish();
        result.addProperty("identifier", ID_PREFIX + name);
        result.add("items", items);
        result.add("appearances", appearances);
        if (!assets.customModelDataColors().isEmpty()) {
            result.add("custom_model_data_colors", assets.customModelDataColors());
        }
        return result;
    }

    private static JsonObject vanilla(JavaModelResources resources,
                                      BedrockDisplayAssets assets,
                                      Map<List<Part>, Integer> geometryIndices,
                                      Map<List<Part>, Integer> meshes,
                                      JsonArray appearances) throws IOException {
        JsonObject items = new JsonObject();
        HeldAppearances held = new HeldAppearances();
        Set<Key> selected = Config.bedrockVanillaDisplayItems();
        for (Map.Entry<Key, JsonObject> entry : resources.pack().vanillaItemDefinitions().entrySet()) {
            JsonArray contexts = new JsonArray(DISPLAY_CONTEXTS.length);
            JsonObject model = entry.getValue().getAsJsonObject("model");
            List<List<Part>> converted = List.of();
            try {
                if (selected.contains(entry.getKey())) {
                    converted = new ArrayList<>(DISPLAY_CONTEXTS.length);
                    for (ItemDisplayContext context : DISPLAY_CONTEXTS) {
                        converted.add(vanillaParts(resources, model, context, List.of()));
                    }
                }
            } catch (BedrockResourceException exception) {
                converted = List.of();
            }
            for (ItemDisplayContext context : DISPLAY_CONTEXTS) {
                if (!converted.isEmpty()) {
                    List<Part> parts = converted.get(context.ordinal());
                    Integer index = geometryIndices.get(parts);
                    if (index == null) {
                        index = appearance(assets, parts, meshes, appearances);
                        geometryIndices.put(parts, index);
                    }
                    contexts.add(index);
                    continue;
                }
                contexts.add(heldAppearance(resources, model, context, held, appearances));
            }
            items.add(entry.getKey().asString(), contexts);
        }
        return items;
    }

    private static int heldAppearance(JavaModelResources resources,
                                      JsonObject node,
                                      ItemDisplayContext context,
                                      HeldAppearances held,
                                      JsonArray appearances) throws IOException {
        List<Transformation> transforms = new ArrayList<>();
        Key model = vanillaModel(node, context, transforms, resources.pack().version());
        Map<DisplayPosition, JavaModel.Transform> display = model == null ? Map.of() : resources.models().resolveVanillaDisplay(model);
        DisplayPosition position = DISPLAY_POSITIONS[context.ordinal()];
        JavaModel.Transform transform = position == null ? JavaModel.Transform.IDENTITY : display.getOrDefault(position, JavaModel.Transform.IDENTITY);
        boolean mirrored = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        int side = mirrored ? -1 : 1;
        Map<JavaModel.Transform, Integer> direct = mirrored ? held.mirrored : held.normal;
        boolean hasLocalTransforms = !transforms.isEmpty();
        // 原版模型缓存的 display 对象会复用，常见姿态可以直接复用已经登记的索引
        if (!hasLocalTransforms) {
            Integer cached = direct.get(transform);
            if (cached != null) {
                return cached;
            }
        }
        JsonArray pose = new JsonArray(4);
        pose.add(0);
        if (hasLocalTransforms) {
            float radians = (float) (Math.PI / 180);
            Matrix4f coordinates = new Matrix4f().translation(8, -8, -8).scale(-16, 16, 16);
            Matrix4f local = new Matrix4f(coordinates).mul(BedrockModelTransform.compose(transforms)).mul(new Matrix4f(coordinates).invert());
            Matrix4f matrix = new Matrix4f().translation(-side * transform.translation().x, transform.translation().y, transform.translation().z)
                    .rotateXYZ(transform.rotation().x * radians, -side * transform.rotation().y * radians, -side * transform.rotation().z * radians)
                    .scale(transform.scale()).mul(local);
            Vector3f rotation = BedrockModelTransform.rotationAngles(new Matrix3d().rotation(matrix.getUnnormalizedRotation(new Quaternionf())).transpose()).negate();
            pose.add(VectorUtils.toJson(matrix.getTranslation(new Vector3f())));
            pose.add(VectorUtils.toJson(rotation));
            pose.add(VectorUtils.toJson(matrix.getScale(new Vector3f())));
        } else {
            pose.add(VectorUtils.toJson(-side * transform.translation().x + 0.0f, transform.translation().y, transform.translation().z));
            pose.add(VectorUtils.toJson(-transform.rotation().x + 0.0f, -side * transform.rotation().y + 0.0f, side * transform.rotation().z + 0.0f));
            pose.add(VectorUtils.toJson(transform.scale()));
        }
        Integer index = held.indices.get(pose);
        if (index == null) {
            index = appearances.size();
            appearances.add(pose);
            held.indices.put(pose, index);
        }
        if (!hasLocalTransforms) {
            direct.put(transform, index);
        }
        return index;
    }

    private static List<Part> vanillaParts(JavaModelResources resources,
                                           JsonObject node,
                                           ItemDisplayContext context,
                                           List<Transformation> inherited) throws IOException {
        List<Transformation> transforms = inherited;
        if (node.has("transformation")) {
            transforms = new ArrayList<>(inherited);
            transforms.add(Transformation.fromJson(node.get("transformation")));
        }
        String type = node.get("type").getAsString().replace("minecraft:", "");
        if (type.equals("composite")) {
            List<Part> parts = new ArrayList<>();
            for (JsonElement child : node.getAsJsonArray("models")) {
                parts.addAll(vanillaParts(resources, child.getAsJsonObject(), context, transforms));
            }
            return parts;
        }
        if (type.equals("model") || type.equals("special")) {
            Key id = Key.of(node.get(type.equals("special") ? "base" : "model").getAsString());
            JavaModel model = resources.models().resolve(id);
            List<Tint> tints = new ArrayList<>();
            if (node.has("tints")) {
                for (JsonElement tint : node.getAsJsonArray("tints")) {
                    tints.add(Tints.fromJson(tint.getAsJsonObject()));
                }
            }
            JavaModel geometry = null;
            if (type.equals("special") || model.kind() == JavaModel.GeometryKind.ENTITY) {
                String special = type.equals("special") ? node.getAsJsonObject("model").get("type").getAsString() : id.value().substring("item/".length());
                geometry = JavaDisplaySpecialModels.create(resources, model, special);
            }
            if (geometry == null && model.kind() == JavaModel.GeometryKind.ELEMENTS && model.elements().isEmpty()) {
                throw new BedrockResourceException("resource_pack.bedrock.reason.geometry", id.asString());
            }
            BedrockModelTransform.validate(BedrockModelTransform.compose(transforms));
            JavaModel mesh = geometry == null ? model : geometry;
            if (mesh.kind() == JavaModel.GeometryKind.GENERATED) {
                for (JavaModel.Texture texture : resources.models().resolveLayers(mesh)) {
                    resources.textures().read(texture, TextureUsage.ITEM);
                }
            } else {
                for (JavaModel.Element element : mesh.elements()) {
                    for (JavaModel.Face face : element.faces().values()) {
                        resources.textures().read(face.texture(), TextureUsage.ITEM);
                    }
                }
            }
            JavaModel.Transform pose = context == ItemDisplayContext.NONE ? JavaModel.Transform.IDENTITY
                    : model.display().get(DISPLAY_POSITIONS[context.ordinal()]);
            boolean mirrored = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            return List.of(new Part(new ItemRenderTarget.Model(id, tints), transforms, pose, mirrored, geometry));
        }
        JsonObject selected = null;
        if (type.equals("condition")) {
            selected = node.getAsJsonObject("on_false");
        } else if (type.equals("select")) {
            String property = node.get("property").getAsString();
            String value = switch (property) {
                case "minecraft:display_context", "display_context" -> context.serializedName();
                case "minecraft:main_hand", "main_hand" -> "right";
                default -> null;
            };
            if (value != null) {
                for (JsonElement element : node.getAsJsonArray("cases")) {
                    JsonObject branch = element.getAsJsonObject();
                    JsonElement when = branch.get("when");
                    if ((when.isJsonPrimitive() && when.getAsString().equals(value))
                            || (when.isJsonArray() && when.getAsJsonArray().contains(new JsonPrimitive(value)))) {
                        selected = branch.getAsJsonObject("model");
                        break;
                    }
                }
            }
        }
        if (selected == null) {
            selected = node.getAsJsonObject("fallback");
        }
        if (selected == null) {
            throw new BedrockResourceException("resource_pack.bedrock.reason.render_target", type);
        }
        return vanillaParts(resources, selected, context, transforms);
    }

    @Nullable
    private static Key vanillaModel(JsonObject node, ItemDisplayContext context, List<Transformation> transforms, MinecraftVersion version) {
        if (node.has("transformation")) {
            transforms.add(Transformation.fromJson(node.get("transformation")));
        }
        String type = node.get("type").getAsString();
        switch (type) {
            case "minecraft:model", "model":
                Key model = Key.of(node.get("model").getAsString());
                if (model.equals(TRIDENT_ICON)) {
                    // 图标分支仍由原生三叉戟几何承载，保留其朝上的基准，不能切回未翻转的特殊模型坐标。
                    transforms.add(SPECIAL_MODEL_FLIP);
                }
                return model;
            case "minecraft:special", "special":
                String special = node.getAsJsonObject("model").get("type").getAsString().replace("minecraft:", "");
                if (!special.equals("shield") && !special.equals("trident")) {
                    // 其它原生特殊模型尚未还原到 Java 局部坐标，继续使用原生基准，避免重复应用其内置姿态。
                    transforms.clear();
                } else if (version.isBelow(MinecraftVersion.V26_1)) {
                    transforms.add(SPECIAL_MODEL_FLIP);
                }
                return Key.of(node.get("base").getAsString());
            case "minecraft:condition", "condition":
                return vanillaModel(node.getAsJsonObject("on_false"), context, transforms, version);
            case "minecraft:select", "select":
                String property = node.get("property").getAsString();
                String value = switch (property) {
                    case "minecraft:display_context", "display_context" -> context.serializedName();
                    case "minecraft:main_hand", "main_hand" -> "right";
                    default -> null;
                };
                if (value != null) {
                    for (JsonElement element : node.getAsJsonArray("cases")) {
                        JsonObject branch = element.getAsJsonObject();
                        JsonElement when = branch.get("when");
                        if ((when.isJsonPrimitive() && when.getAsString().equals(value))
                                || (when.isJsonArray() && when.getAsJsonArray().contains(new JsonPrimitive(value)))) {
                            return vanillaModel(branch.getAsJsonObject("model"), context, transforms, version);
                        }
                    }
                }
                return node.has("fallback") ? vanillaModel(node.getAsJsonObject("fallback"), context, transforms, version) : null;
            case "minecraft:range_dispatch", "range_dispatch":
                return node.has("fallback") ? vanillaModel(node.getAsJsonObject("fallback"), context, transforms, version) : null;
            case "minecraft:composite", "composite":
                JsonArray models = node.getAsJsonArray("models");
                return models.isEmpty() ? null : vanillaModel(models.get(0).getAsJsonObject(), context, transforms, version);
            default:
                return null;
        }
    }

    private static int appearance(BedrockDisplayAssets assets, List<Part> parts, Map<List<Part>, Integer> meshes, JsonArray appearances) throws IOException {
        if (parts.isEmpty()) {
            JsonArray pose = new JsonArray();
            pose.add(assets.add(parts));
            appearances.add(pose);
            return appearances.size() - 1;
        }
        Part first = parts.getFirst();
        boolean sharedPose = true;
        for (int i = 1; i < parts.size(); i++) {
            Part part = parts.get(i);
            if (!part.display().equals(first.display()) || part.mirrored() != first.mirrored()) {
                sharedPose = false;
                break;
            }
        }
        // 通常 NONE、GUI 和手持只在模型姿态上不同。把姿态写入映射，共用几何和材质。
        List<Part> geometry = parts;
        if (sharedPose && (first.mirrored() || !first.display().equals(JavaModel.Transform.IDENTITY))) {
            geometry = new ArrayList<>(parts.size());
            for (Part part : parts) {
                geometry.add(new Part(part.target(), part.transforms(), JavaModel.Transform.IDENTITY, false, part.geometry()));
            }
        }
        Integer shape = meshes.get(geometry);
        if (shape == null) {
            shape = assets.add(geometry);
            meshes.put(geometry, shape);
        }
        if (shape == 0) {
            return 0;
        }
        JsonArray pose = new JsonArray();
        pose.add(shape);
        if (sharedPose && !first.display().equals(JavaModel.Transform.IDENTITY)) {
            JavaModel.Transform transform = first.display();
            int side = first.mirrored() ? -1 : 1;
            pose.add(VectorUtils.toJson(-side * transform.translation().x + 0.0f, transform.translation().y, transform.translation().z));
            pose.add(VectorUtils.toJson(-transform.rotation().x + 0.0f, -side * transform.rotation().y + 0.0f, side * transform.rotation().z + 0.0f));
            pose.add(VectorUtils.toJson(transform.scale()));
        }
        appearances.add(pose);
        return appearances.size() - 1;
    }

    @SuppressWarnings("DeconstructionCanBeUsed")
    private static List<Part> parts(JavaModelResources resources, ItemBindingRule rule, ItemDisplayContext context) throws IOException {
        List<ItemBindingRule> children = rule.target() instanceof ItemRenderTarget.Composite composite ? composite.parts() : List.of(rule);
        List<Part> parts = new ArrayList<>(children.size());
        DisplayPosition position = DISPLAY_POSITIONS[context.ordinal()];
        for (ItemBindingRule child : children) {
            if (!(child.target() instanceof ItemRenderTarget.Model target)) {
                return List.of();
            }
            JavaModel model = target.resolve(resources.models());
            JavaModel.Transform transform = position == null ? JavaModel.Transform.IDENTITY : model.display().get(position);
            boolean mirrored = (position == DisplayPosition.FIRSTPERSON_LEFTHAND || position == DisplayPosition.THIRDPERSON_LEFTHAND);
            parts.add(new Part(target, child.transformations(), transform, mirrored));
        }
        return parts;
    }

    private static final class HeldAppearances {
        private final Map<JsonArray, Integer> indices = new HashMap<>();
        private final Map<JavaModel.Transform, Integer> normal = new IdentityHashMap<>();
        private final Map<JavaModel.Transform, Integer> mirrored = new IdentityHashMap<>();
    }

    record Part(ItemRenderTarget.Model target, List<Transformation> transforms, JavaModel.Transform display, boolean mirrored, @Nullable JavaModel geometry) {
        Part(ItemRenderTarget.Model target, List<Transformation> transforms, JavaModel.Transform display, boolean mirrored) {
            this(target, transforms, display, mirrored, null);
        }
    }
}
