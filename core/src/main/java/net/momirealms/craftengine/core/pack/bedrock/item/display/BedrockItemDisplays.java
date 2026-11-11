package net.momirealms.craftengine.core.pack.bedrock.item.display;

import com.google.gson.JsonArray;
import net.momirealms.craftengine.core.plugin.config.Config;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemResourceGenerator;
import net.momirealms.craftengine.core.pack.bedrock.item.GeneratedItemVariant;
import net.momirealms.craftengine.core.pack.bedrock.item.equipment.BedrockItemAttachables;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingCondition;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRegistrationPlan;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelTransform;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
import net.momirealms.craftengine.core.pack.model.definition.Transformation;
import net.momirealms.craftengine.core.pack.model.definition.select.DisplayContextSelectProperty;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tint;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tints;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public final class BedrockItemDisplays {

    private BedrockItemDisplays() {
    }

    public static JsonObject write(JavaModelResources resources,
                                   ItemRegistrationPlan plan,
                                   BedrockItemResourceGenerator.Result generated,
                                   Path root,
                                   String packName) throws IOException {
        String name = "item_display_" + UUID.nameUUIDFromBytes(packName.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
        BedrockDisplayAssets assets = new BedrockDisplayAssets(resources, root, name);
        Map<List<Part>, Integer> indices = new HashMap<>();
        Map<List<Part>, Integer> meshes = new HashMap<>();
        JsonArray appearances = new JsonArray();
        appearances.add(new JsonArray());
        JsonObject items = new JsonObject();
        for (ItemRegistrationPlan.Entry entry : plan.entries()) {
            for (GeneratedItemVariant variant : generated.variants().getOrDefault(entry.bedrockId(), List.of())) {
                if (!variant.generated() || variant.worldView() != null) {
                    continue;
                }
                JsonArray contexts = new JsonArray();
                boolean mapped = false;
                Set<String> reported = new HashSet<>();
                for (ItemDisplayContext context : ItemDisplayContext.values()) {
                    int shape = 0;
                    ItemBindingRule rule = select(entry, variant, context);
                    if (rule != null) {
                        // 每种展示上下文先选 Java 分支，再应用该模型的 display；NONE 使用恒等变换。
                        try {
                            List<Part> parts = parts(resources, rule, context);
                            if (!parts.isEmpty()) {
                                Integer existing = indices.get(parts);
                                if (existing == null) {
                                    existing = appearance(assets, parts, meshes, appearances);
                                    indices.put(parts, existing);
                                }
                                shape = existing;
                            }
                        } catch (UnsupportedOperationException | UncheckedIOException e) {
                            String reason = e.toString();
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
        if (Config.bedrockConvertVanillaItems()) {
            result.add("vanilla", vanilla(resources, assets, indices, meshes, appearances));
            result.addProperty("vanilla_identifier", "craftengine:" + name + "_held");
        }
        result.addProperty("convert_text", Config.bedrockConvertText());
        assets.finish(Config.bedrockConvertVanillaItems());
        result.addProperty("identifier", "craftengine:" + name);
        result.add("items", items);
        result.add("appearances", appearances);
        return result;
    }

    private static JsonObject vanilla(JavaModelResources resources, BedrockDisplayAssets assets, Map<List<Part>, Integer> geometryIndices,
                                      Map<List<Part>, Integer> meshes, JsonArray appearances) throws IOException {
        JsonObject items = new JsonObject();
        Map<JsonArray, Integer> indices = new HashMap<>();
        for (Map.Entry<Key, JsonObject> entry : resources.pack().vanillaItemDefinitions().entrySet()) {
            JsonArray contexts = new JsonArray();
            List<List<Part>> converted = new ArrayList<>(ItemDisplayContext.values().length);
            try {
                for (ItemDisplayContext context : ItemDisplayContext.values()) {
                    converted.add(vanillaParts(resources, entry.getValue().getAsJsonObject("model"), context, List.of()));
                }
            } catch (UnsupportedOperationException | IOException exception) {
                converted.clear();
            }
            for (ItemDisplayContext context : ItemDisplayContext.values()) {
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
                Key model = vanillaModel(entry.getValue().getAsJsonObject("model"), context);
                Map<DisplayPosition, JavaModel.Transform> display = model == null ? Map.of() : resources.models().resolveVanillaDisplay(model);
                JavaModel.Transform transform = context == ItemDisplayContext.NONE ? JavaModel.Transform.IDENTITY
                        : display.getOrDefault(DisplayPosition.valueOf(context.serializedName().toUpperCase(Locale.ROOT)), JavaModel.Transform.IDENTITY);
                boolean mirrored = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
                int side = mirrored ? -1 : 1;
                JsonArray pose = new JsonArray();
                pose.add(0);
                pose.add(VectorUtils.toJson(-side * transform.translation().x + 0.0f, transform.translation().y, transform.translation().z));
                pose.add(VectorUtils.toJson(-transform.rotation().x + 0.0f, -side * transform.rotation().y + 0.0f, side * transform.rotation().z + 0.0f));
                pose.add(VectorUtils.toJson(transform.scale()));
                Integer index = indices.get(pose);
                if (index == null) {
                    index = appearances.size();
                    appearances.add(pose);
                    indices.put(pose, index);
                }
                contexts.add(index);
            }
            items.add(entry.getKey().asString(), contexts);
        }
        return items;
    }

    private static List<Part> vanillaParts(JavaModelResources resources, JsonObject node, ItemDisplayContext context,
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
                throw new UnsupportedOperationException("Empty vanilla display model " + id);
            }
            BedrockModelTransform.uniformScale(BedrockModelTransform.compose(transforms));
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
                    : model.display().get(DisplayPosition.valueOf(context.serializedName().toUpperCase(Locale.ROOT)));
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
            throw new UnsupportedOperationException("Unsupported vanilla display node " + type);
        }
        return vanillaParts(resources, selected, context, transforms);
    }

    @Nullable
    private static Key vanillaModel(JsonObject node, ItemDisplayContext context) {
        String type = node.get("type").getAsString();
        switch (type) {
            case "minecraft:model", "model":
                return Key.of(node.get("model").getAsString());
            case "minecraft:special", "special":
                return Key.of(node.get("base").getAsString());
            case "minecraft:condition", "condition":
                return vanillaModel(node.getAsJsonObject("on_false"), context);
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
                            return vanillaModel(branch.getAsJsonObject("model"), context);
                        }
                    }
                }
                return node.has("fallback") ? vanillaModel(node.getAsJsonObject("fallback"), context) : null;
            case "minecraft:range_dispatch", "range_dispatch":
                return node.has("fallback") ? vanillaModel(node.getAsJsonObject("fallback"), context) : null;
            case "minecraft:composite", "composite":
                JsonArray models = node.getAsJsonArray("models");
                return models.isEmpty() ? null : vanillaModel(models.get(0).getAsJsonObject(), context);
            default:
                return null;
        }
    }

    private static int appearance(BedrockDisplayAssets assets, List<Part> parts, Map<List<Part>, Integer> meshes, JsonArray appearances) throws IOException {
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

    @Nullable
    private static ItemBindingRule select(ItemRegistrationPlan.Entry entry, GeneratedItemVariant variant, ItemDisplayContext context) {
        if (!variant.displayModels().isEmpty()) {
            return variant.displayModels().get(context);
        }
        JsonPrimitive view = new JsonPrimitive(context.serializedName());
        for (ItemBindingRule rule : entry.definition().rules()) {
            if (!BedrockItemAttachables.matchesIdle(rule)) {
                continue;
            }
            boolean matches = true;
            for (ItemBindingCondition condition : rule.conditions()) {
                if (condition instanceof ItemBindingCondition.SelectMatch select && select.property() instanceof DisplayContextSelectProperty) {
                    matches &= select.matches(view);
                } else if (!BedrockItemAttachables.isUseCondition(condition)) {
                    // Geyser 已根据实际物品栈选中注册变体；不能拿其它耐久、染色或组件分支替代它。
                    matches &= variant.rule().conditions().contains(condition);
                }
            }
            if (matches) {
                return rule;
            }
        }
        return null;
    }

    @SuppressWarnings("DeconstructionCanBeUsed")
    private static List<Part> parts(JavaModelResources resources, ItemBindingRule rule, ItemDisplayContext context) {
        List<ItemBindingRule> children = rule.target() instanceof ItemRenderTarget.Composite composite ? composite.parts() : List.of(rule);
        List<Part> parts = new ArrayList<>(children.size());
        DisplayPosition position = context == ItemDisplayContext.NONE ? null : DisplayPosition.valueOf(context.serializedName().toUpperCase(Locale.ROOT));
        for (ItemBindingRule child : children) {
            if (!(child.target() instanceof ItemRenderTarget.Model target)) {
                return List.of();
            }
            JavaModel model;
            try {
                model = target.resolve(resources.models());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            JavaModel.Transform transform = position == null ? JavaModel.Transform.IDENTITY : model.display().get(position);
            boolean mirrored = (position == DisplayPosition.FIRSTPERSON_LEFTHAND || position == DisplayPosition.THIRDPERSON_LEFTHAND);
            parts.add(new Part(target, child.transformations(), transform, mirrored));
        }
        return parts;
    }

    record Part(ItemRenderTarget.Model target, List<Transformation> transforms, JavaModel.Transform display, boolean mirrored, @Nullable JavaModel geometry) {
        Part(ItemRenderTarget.Model target, List<Transformation> transforms, JavaModel.Transform display, boolean mirrored) {
            this(target, transforms, display, mirrored, null);
        }
    }
}
