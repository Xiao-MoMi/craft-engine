package net.momirealms.craftengine.core.pack.bedrock.item.render;

import com.google.gson.JsonArray;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.pack.bedrock.BedrockResourceException;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemGenerationContext;
import net.momirealms.craftengine.core.pack.bedrock.item.GeneratedItemVariant;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingCondition;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ResolvedItemBinding;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemDisplayBlocks;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockDyedItem;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemTint;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemIconRenderer;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockItemGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tint;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 根据 Java 物品的显示位置规则，生成背包外观、手持和头部的 attachable，以及掉落物、展示框和置物架使用的别名。
 */
final class BedrockItemContextWriter {
    private static final List<ItemDisplayContext> EQUIPMENT_VIEWS = List.of(
            ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
            ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
            ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
            ItemDisplayContext.HEAD
    );
    private final JavaModelResources resources;
    private final BedrockItemIconRenderer renderer;
    private final BedrockEquipmentResources equipmentResources;
    private final BedrockItemVisualWriter visuals;

    BedrockItemContextWriter(JavaModelResources resources, BedrockItemIconRenderer renderer, BedrockItemVisualWriter visuals) {
        this.resources = resources;
        this.renderer = renderer;
        this.visuals = visuals;
        this.equipmentResources = new BedrockEquipmentResources(resources);
    }

    List<GeneratedItemVariant> write(ResolvedItemBinding entry, BedrockItemPresentation.Plan plan, BedrockItemGenerationContext context) throws IOException {
        List<GeneratedItemVariant> output = new ObjectArrayList<>();
        BedrockItemPresentation.ItemFeatures features = plan.features();
        Map<List<ItemBindingRule>, BedrockItemTint> cachedTints = new IdentityHashMap<>();
        boolean needsEquipmentAppearance = features.hasEquipmentAsset();
        BedrockEquipmentResources.Appearance equipmentAppearance = null;
        for (BedrockItemPresentation variant : plan.variants()) {
            Map<ItemDisplayContext, BedrockItemTint> tints;
            try {
                tints = this.resolveTints(variant, cachedTints);
            } catch (BedrockResourceException exception) {
                ItemBindingRule gui = variant.idleRules().get(ItemDisplayContext.GUI);
                TranslationManager.warn("resource_pack.bedrock.item.variant_skipped", variant.id().asString(), entry.itemModel().asString(), gui.sourcePath(), exception.getLocalizedMessage());
                continue;
            }
            Key id = variant.id();
            Key headId = features.usesHeadModel() ? Key.of(id.namespace(), id.value() + "_view_head") : null;
            if (needsEquipmentAppearance) {
                equipmentAppearance = this.equipmentResources.resolve(entry, features.equipmentSlot(), features.equipmentAssetId(), context.root());
                needsEquipmentAppearance = false;
            }
            GeneratedItemVariant item = this.writeVariant(variant, headId, equipmentAppearance, features, tints, context);
            if (features.hasDye()) {
                item = item.withTintSources(tintSources(tints));
            }
            this.visuals.writeWorldAliases(item, variant.separateWorldViews(), context, output);
            registerEquipmentAppearance(id, item, equipmentAppearance, context);
            output.add(item);
        }
        return output;
    }

    private Map<ItemDisplayContext, BedrockItemTint> resolveTints(BedrockItemPresentation plan,
                                                                  Map<List<ItemBindingRule>, BedrockItemTint> cachedTints) throws IOException {
        Map<ItemDisplayContext, BedrockItemTint> tints = new EnumMap<>(ItemDisplayContext.class);
        for (Map.Entry<ItemDisplayContext, List<ItemBindingRule>> view : plan.rulesByContext().entrySet()) {
            List<ItemBindingRule> rules = view.getValue();
            // 背包和掉落等位置共用同一份规则列表时，只解析一次染色源
            BedrockItemTint tint = cachedTints.get(rules);
            if (tint == null) {
                this.validateTints(rules);
                tint = BedrockItemTint.analyze(rules, this.resources);
                cachedTints.put(rules, tint);
            }
            ItemDisplayContext displayContext = view.getKey();
            tints.put(displayContext, tint);
            if (tint.source() == BedrockItemTint.MULTIPLE) {
                TranslationManager.warn("resource_pack.bedrock.item.tint_fallback", plan.id().asString(), displayContext.serializedName());
            }
        }
        return tints;
    }

    private static Map<ItemDisplayContext, Integer> tintSources(Map<ItemDisplayContext, BedrockItemTint> tints) {
        Map<ItemDisplayContext, Integer> sources = new EnumMap<>(ItemDisplayContext.class);
        for (Map.Entry<ItemDisplayContext, BedrockItemTint> entry : tints.entrySet()) {
            BedrockItemTint tint = entry.getValue();
            sources.put(entry.getKey(), tint.dynamic() ? tint.source() : BedrockItemTint.NONE);
        }
        return sources;
    }

    @SuppressWarnings("DeconstructionCanBeUsed")
    private void validateTints(List<ItemBindingRule> rules) throws IOException {
        for (ItemBindingRule rule : rules) {
            if (rule.target() instanceof ItemRenderTarget.Model model) {
                for (Tint tint : model.tints()) {
                    this.resources.textures().resolveTint(tint);
                }
            } else if (rule.target() instanceof ItemRenderTarget.Composite composite) {
                this.validateTints(composite.parts());
            }
        }
    }

    static BedrockItemAttachables.View equippedView(ItemDisplayContext view) {
        return switch (view) {
            case FIRST_PERSON_RIGHT_HAND -> BedrockItemAttachables.View.FIRST_RIGHT;
            case FIRST_PERSON_LEFT_HAND -> BedrockItemAttachables.View.FIRST_LEFT;
            case THIRD_PERSON_RIGHT_HAND -> BedrockItemAttachables.View.THIRD_RIGHT;
            case THIRD_PERSON_LEFT_HAND -> BedrockItemAttachables.View.THIRD_LEFT;
            case HEAD -> BedrockItemAttachables.View.HEAD;
            default -> throw new IllegalArgumentException(view.name());
        };
    }

    private GeneratedItemVariant writeVariant(BedrockItemPresentation plan,
                                              @Nullable Key headId,
                                              @Nullable BedrockEquipmentResources.Appearance equipmentAppearance,
                                              BedrockItemPresentation.ItemFeatures features,
                                              Map<ItemDisplayContext, BedrockItemTint> tints,
                                              BedrockItemGenerationContext context) throws IOException {
        if (headId == null && !features.hasEquipmentAsset() && !features.hasDye()) {
            // 例如弓可以先尝试专用生成器，返回 null 时继续用下方的通用生成流程
            GeneratedItemVariant nativeItem = plan.strategy().tryWriteSpecialized(plan, this.resources, this.renderer, context);
            if (nativeItem != null) {
                return GeneratedItemVariant.displayContextItem(nativeItem, plan.idleRules(), null);
            }
        }

        GeneratedItemVariant inventory = this.writeInventory(plan, tints.get(ItemDisplayContext.GUI).defaultColor(), context);
        this.writeAttachables(plan, headId, equipmentAppearance, features.hasEquipmentAsset(), tints, context);
        // 把各位置选好的闲置规则交给后续的世界物品外观和展示实体生成器
        return GeneratedItemVariant.displayContextItem(inventory, plan.idleRules(), headId);
    }

    private GeneratedItemVariant writeInventory(BedrockItemPresentation plan,
                                                @Nullable Integer defaultDye,
                                                BedrockItemGenerationContext context) throws IOException {
        Key id = plan.id();
        // 两个耐久变体可以共用背包图标，注册时仍需匹配各自的物品栈条件
        List<ItemBindingCondition> conditions = plan.conditions();
        try {
            ItemBindingRule gui = plan.idleRules().get(ItemDisplayContext.GUI);
            if (defaultDye == null) {
                return this.visuals.writeVisual(id, conditions, gui, plan.separateWorldViews() ? DisplayPosition.GUI : null, context);
            }
            List<ItemBindingRule> parts = BedrockItemVisualWriter.modelParts(gui);
            GeneratedItemVariant inventory = BedrockDyedItem.writeIcon(id, conditions, parts, this.resources, context, DisplayPosition.GUI, defaultDye);
            if (plan.item().clientBehavior().placesBlock() && !parts.isEmpty()) {
                BedrockItemDisplayBlocks.DisplayBlock display = context.itemDisplayBlocks().write(id, parts, this.resources.models(), this.resources.textures(), context.root());
                return new GeneratedItemVariant(id, conditions, inventory.iconTexture(), display, false, defaultDye);
            }
            return inventory;
        } catch (BedrockResourceException e) {
            // 例如只有 GUI 规则引用了无法转换的模型时，透明图标保留物品注册，右手和掉落仍可用自己的模型
            BedrockItemVisualWriter.warn(id, ItemDisplayContext.GUI, e);
            return BedrockItemVisualWriter.transparent(id, conditions, Map.of(), context);
        }
    }

    private void writeAttachables(BedrockItemPresentation plan,
                                  @Nullable Key headId,
                                  @Nullable BedrockEquipmentResources.Appearance equipmentAppearance,
                                  boolean hasEquipmentAsset,
                                  Map<ItemDisplayContext, BedrockItemTint> tints,
                                  BedrockItemGenerationContext context) throws IOException {
        JsonArray selectors = new JsonArray();
        Map<Layer, List<String>> layers = collectEquipmentLayers(plan, selectors, hasEquipmentAsset);
        BedrockItemGeometryBuilder mesh = new BedrockItemGeometryBuilder(this.resources.models(), this.resources.textures(), TextureUsage.ITEM, true, false);
        // 第一和第三人称共用模型规则且动态染色模式相同时，可以复用几何，各自的姿态和显示条件仍按场景生成
        // 材质编号由当前 mesh 分配，几何缓存只在本次 attachable 生成中使用
        Map<GeometryKey, BedrockItemGeometry> geometries = new HashMap<>();
        List<BedrockItem3DAttachables.Stage> stages = new ObjectArrayList<>(layers.size());
        boolean hasHandModel = false;
        for (Map.Entry<Layer, List<String>> entry : layers.entrySet()) {
            Layer layer = entry.getKey();
            try {
                boolean dynamic = tints.get(layer.view()).dynamic();
                GeometryKey key = new GeometryKey(layer.part(), dynamic);
                BedrockItemGeometry geometry = geometries.get(key);
                if (geometry == null) {
                    geometry = mesh.build(layer.part(), dynamic);
                    geometries.put(key, geometry);
                }
                stages.add(new BedrockItem3DAttachables.Stage(geometry, String.join(" || ", entry.getValue()), equippedView(layer.view())));
                hasHandModel |= layer.view() != ItemDisplayContext.HEAD && !geometry.cubes().isEmpty();
            } catch (BedrockResourceException e) {
                BedrockItemVisualWriter.warn(plan.id(), layer.view(), e);
            }
        }
        BedrockAttachablePose pose = plan.strategy().pose(plan.item());
        List<BedrockItemGeometry.Material> materials = mesh.materials();
        for (BedrockItemGeometry.Material material : materials) {
            if (material.dyeColor() != null) {
                pose.scripts().add(BedrockDyedItem.colorScript());
                break;
            }
        }
        pose.scripts().addAll(selectors);
        Key id = plan.id();
        BedrockItem3DAttachables.writeDisplayContexts(id, headId, stages, materials, pose, equipmentAppearance, context.root());
        if (hasHandModel) {
            plan.strategy().registerHandModel(context.playerAnimations(), id, plan.item());
        }
    }

    private static void registerEquipmentAppearance(Key id,
                                                    GeneratedItemVariant item,
                                                    @Nullable BedrockEquipmentResources.Appearance equipmentAppearance,
                                                    BedrockItemGenerationContext context) {
        if (equipmentAppearance == null) {
            return;
        }
        equipmentAppearance.registerAnimals(id, context.animalEquipment());
        // 鞘翅贴图绑定到已生成的物品 ID，供客户端切换穿戴外观
        if (item.generated()) {
            for (BedrockEquipmentResources.Layer layer : equipmentAppearance.layers()) {
                if (layer.slot() == BedrockEquipmentResources.Slot.WINGS) {
                    context.elytraEquipment().register(id, layer.texture());
                }
            }
        }
    }

    private static Map<Layer, List<String>> collectEquipmentLayers(BedrockItemPresentation plan, JsonArray selectors, boolean hasEquipmentAsset) {
        // 同一位置的同一模型部件只生成一次，任一对应阶段生效时都显示它
        Map<Layer, List<String>> layers = new LinkedHashMap<>();
        Map<List<ItemBindingRule>, String> expressions = new IdentityHashMap<>();
        for (ItemDisplayContext view : EQUIPMENT_VIEWS) {
            if (hasEquipmentAsset && view == ItemDisplayContext.HEAD) {
                continue;
            }
            List<ItemBindingRule> rules = plan.rulesByContext().get(view);
            String selector = "variable.ce_" + view.serializedName() + "_stage";
            String expression = expressions.get(rules);
            if (rules.size() > 1) {
                // 例如根据是否拉弓选中松弦或拉弦的规则编号，后续渲染条件同时匹配手持场景和编号
                // 共用同一份规则列表的视角复用选择表达式，各自写入自己的阶段变量
                if (expression == null) {
                    expression = BedrockItemAttachables.stageSelectionExpression(rules, plan.strategy()::stageCondition);
                    expressions.put(rules, expression);
                }
                selectors.add(selector + " = " + expression + ';');
            }
            String viewCondition = equippedView(view).condition;
            for (int i = 0; i < rules.size(); i++) {
                ItemBindingRule rule = rules.get(i);
                try {
                    if (rules.size() == 1 && expression == null) {
                        expression = plan.strategy().stageCondition(rule);
                        expressions.put(rules, expression);
                    }
                    String stage = rules.size() == 1 ? expression : selector + " == " + i;
                    String condition = "(" + viewCondition + ") && (" + stage + ")";
                    for (ItemBindingRule part : BedrockItemVisualWriter.modelParts(rule)) {
                        layers.computeIfAbsent(new Layer(view, part), ignored -> new ObjectArrayList<>()).add(condition);
                    }
                } catch (BedrockResourceException e) {
                    BedrockItemVisualWriter.warn(plan.id(), view, e);
                }
            }
        }
        return layers;
    }

    private record GeometryKey(ItemBindingRule rule, boolean dynamic) {
    }

    private record Layer(ItemDisplayContext view, ItemBindingRule part) {
    }
}
