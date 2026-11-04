package net.momirealms.craftengine.core.pack.bedrock.item.display;

import com.google.gson.JsonArray;
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
import net.momirealms.craftengine.core.pack.model.definition.Transformation;
import net.momirealms.craftengine.core.pack.model.definition.select.DisplayContextSelectProperty;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
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

    public static JsonObject write(JavaModelResources resources, ItemRegistrationPlan plan, BedrockItemResourceGenerator.Result generated,
                                   Path root, String packName) throws IOException {
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
        if (!items.isEmpty()) {
            assets.finish();
            result.addProperty("identifier", "craftengine:" + name);
            result.add("items", items);
            result.add("appearances", appearances);
        }
        return result;
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
                geometry.add(new Part(part.target(), part.transforms(), JavaModel.Transform.IDENTITY, false));
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
            pose.add(VectorUtils.toJson(-side * transform.translation().x, transform.translation().y, transform.translation().z));
            pose.add(VectorUtils.toJson(-transform.rotation().x, -side * transform.rotation().y, side * transform.rotation().z));
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

    record Part(ItemRenderTarget.Model target, List<Transformation> transforms, JavaModel.Transform display, boolean mirrored) {
    }
}
