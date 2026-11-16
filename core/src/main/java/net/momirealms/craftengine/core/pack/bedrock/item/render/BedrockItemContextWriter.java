package net.momirealms.craftengine.core.pack.bedrock.item.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.pack.bedrock.BedrockResourceException;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemGenerationContext;
import net.momirealms.craftengine.core.pack.bedrock.item.GeneratedItemVariant;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingCondition;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ResolvedItemBinding;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemDisplayBlocks;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockDyedItem;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemIconRenderer;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockItemGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTexture;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tint;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.IOException;
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
    private static final List<ItemDisplayContext> WORLD_VIEWS = List.of(
            ItemDisplayContext.GROUND,
            ItemDisplayContext.FIXED,
            ItemDisplayContext.ON_SHELF
    );
    private final JavaModelResources resources;
    private final BedrockItemIconRenderer renderer;
    private final BedrockEquipmentResources equipmentResources;

    BedrockItemContextWriter(JavaModelResources resources, BedrockItemIconRenderer renderer) {
        this.resources = resources;
        this.renderer = renderer;
        this.equipmentResources = new BedrockEquipmentResources(resources);
    }

    List<GeneratedItemVariant> write(ResolvedItemBinding entry, List<BedrockItemPresentation> plans, BedrockItemGenerationContext context) throws IOException {
        List<GeneratedItemVariant> output = new ObjectArrayList<>();
        boolean hasArmor = BedrockEquipmentResources.hasAsset(entry);
        BedrockEquipmentResources.Appearance armor = this.equipmentResources.resolve(entry, context.root());
        boolean headEquipment = !hasArmor && isHeadEquipment(entry);
        for (BedrockItemPresentation plan : plans) {
            Key id = plan.id();
            try {
                for (List<ItemBindingRule> rules : plan.rulesByContext().values()) {
                    this.validateTints(rules);
                }
            } catch (BedrockResourceException exception) {
                TranslationManager.warn("resource_pack.bedrock.item.variant_skipped", id.asString(), entry.itemModel().asString(), plan.idleRules().get(ItemDisplayContext.GUI).sourcePath(), exception.getLocalizedMessage());
                continue;
            }
            Key headId = headEquipment ? Key.of(id.namespace(), id.value() + "_view_head") : null;
            GeneratedItemVariant item = this.writeVariant(plan, headId, armor, hasArmor, context);
            this.writeWorldAliases(item, plan.separateWorldViews(), context, output);
            if (armor != null) {
                armor.registerAnimals(id, context.animalEquipment());
                for (BedrockEquipmentResources.Layer layer : armor.layers()) {
                    if (layer.slot() == BedrockEquipmentResources.Slot.WINGS && item.generated()) {
                        context.elytraEquipment().register(id, layer.texture());
                    }
                }
            }
            output.add(item);
        }
        return output;
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

    void writeWorldAliases(GeneratedItemVariant item, boolean separateWorldViews, BedrockItemGenerationContext context, List<GeneratedItemVariant> output) throws IOException {
        ItemBindingRule gui = item.displayModels().get(ItemDisplayContext.GUI);
        for (ItemDisplayContext view : WORLD_VIEWS) {
            ItemBindingRule rule = item.displayModels().get(view);
            // 普通流程已在主展示方块中写入常规姿态，只有置物架需要带额外补偿的独立外观。
            if (rule == null || (!separateWorldViews && (view != ItemDisplayContext.ON_SHELF || item.displayBlock() == null))) {
                continue;
            }
            // 世界模型和变换与背包图标相同时可直接复用主物品。三维展示方块仍需按该位置生成姿态。
            if (item.displayBlock() == null && rule.target().equals(gui.target()) && rule.transformations().equals(gui.transformations())) {
                continue;
            }
            this.writeWorldAlias(item, rule, view, context, output);
        }
    }

    void writeWorldAlias(GeneratedItemVariant item,
                         ItemBindingRule rule,
                         ItemDisplayContext view,
                         BedrockItemGenerationContext context,
                         List<GeneratedItemVariant> output) throws IOException {
        Key id = item.id();
        Key alias = Key.of(id.namespace(), id.value() + "_view_" + view.serializedName());
        try {
            GeneratedItemVariant visual = this.writeVisual(alias, item.conditions(), rule, DisplayPosition.valueOf(view.name()), context);
            output.add(GeneratedItemVariant.worldAlias(visual, view));
        } catch (BedrockResourceException e) {
            warn(id, view, e);
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

    private static boolean isHeadEquipment(ResolvedItemBinding entry) {
        JsonElement equipment = entry.clientProperties().components().get(DataComponentKeys.EQUIPPABLE);
        return equipment != null && equipment.isJsonObject() && "head".equals(equipment.getAsJsonObject().get("slot").getAsString());
    }

    private GeneratedItemVariant writeVariant(BedrockItemPresentation plan, @Nullable Key headId, @Nullable BedrockEquipmentResources.Appearance armor, boolean hasArmor,
                                              BedrockItemGenerationContext context) throws IOException {
        Map<ItemDisplayContext, ItemBindingRule> idleRules = plan.idleRules();
        Integer dye = null;
        for (List<ItemBindingRule> rules : plan.rulesByContext().values()) {
            dye = BedrockDyedItem.defaultColor(rules, this.resources.textures());
            if (dye != null) {
                break;
            }
        }
        if (headId == null && !hasArmor && dye == null) {
            // 专用策略返回 null 只表示不适用，随后继续通用生成；真正的资源读写异常向上传递。
            GeneratedItemVariant nativeItem = plan.strategy().tryWriteSpecialized(plan, this.resources, this.renderer, context);
            if (nativeItem != null) {
                return GeneratedItemVariant.displayContextItem(nativeItem, idleRules, null);
            }
        }
        ItemBindingRule gui = idleRules.get(ItemDisplayContext.GUI);
        Key id = plan.id();
        // GUI 模型可能被多个耐久或组件变体共用；注册条件必须取完整变体，不能从 GUI 规则反推。
        List<ItemBindingCondition> conditions = plan.conditions();
        GeneratedItemVariant inventory;
        try {
            if (dye == null) {
                inventory = this.writeVisual(id, conditions, gui, plan.separateWorldViews() ? DisplayPosition.GUI : null, context);
            } else {
                List<ItemBindingRule> parts = modelParts(gui);
                inventory = BedrockDyedItem.writeIcon(id, conditions, parts, this.resources, context, DisplayPosition.GUI, dye);
                if (plan.item().clientBehavior().placesBlock() && !parts.isEmpty()) {
                    BedrockItemDisplayBlocks.DisplayBlock display = context.itemDisplayBlocks().write(id, parts, this.resources.models(), this.resources.textures(), context.root());
                    inventory = new GeneratedItemVariant(id, conditions, inventory.iconTexture(), display, false, dye);
                }
            }
        } catch (BedrockResourceException e) {
            // 背包外观生成失败时用透明图标完成注册，手持和世界模型仍可独立生成。
            warn(id, ItemDisplayContext.GUI, e);
            inventory = transparent(id, conditions, context);
        }

        this.writeEquipment(plan, headId, armor, hasArmor, context);
        return GeneratedItemVariant.displayContextItem(inventory, idleRules, headId);
    }

    private GeneratedItemVariant writeVisual(Key id,
                                             List<ItemBindingCondition> conditions,
                                             ItemBindingRule model,
                                             @Nullable DisplayPosition view,
                                             BedrockItemGenerationContext context) throws IOException {
        List<ItemBindingRule> parts = modelParts(model);
        if (parts.isEmpty()) {
            // 显式空模型仍是有效分支；透明图标让这个变体可以注册，同时保持该场景不可见。
            return transparent(id, conditions, context);
        }
        Integer dye = BedrockDyedItem.defaultColor(parts, this.resources.textures());
        if (dye != null) {
            return BedrockDyedItem.writeIcon(id, conditions, parts, this.resources, context, view == null ? DisplayPosition.GUI : view, dye);
        }
        // 只有所有部件都能画成图标时才合成贴图，否则交给展示方块保留整组模型的变换。
        BedrockTexture icon = this.renderIcon(parts);
        if (icon != null) {
            return writeIcon(id, conditions, icon, context);
        }
        BedrockItemDisplayBlocks.DisplayBlock block = context.itemDisplayBlocks().write(id, parts, this.resources.models(), this.resources.textures(), context.root(), view);
        return GeneratedItemVariant.block(id, conditions, block, false);
    }

    @Nullable
    private BedrockTexture renderIcon(List<ItemBindingRule> parts) throws IOException {
        List<BedrockTexture> images = new ObjectArrayList<>(parts.size());
        for (ItemBindingRule part : parts) {
            if (!part.transformations().isEmpty()) {
                return null;
            }
            BedrockTexture image = this.renderer.render((ItemRenderTarget.Model) part.target());
            if (image == null) {
                return null;
            }
            images.add(image);
        }
        return BedrockTexture.composite(images);
    }

    private void writeEquipment(BedrockItemPresentation plan, @Nullable Key headId,
                                @Nullable BedrockEquipmentResources.Appearance armor, boolean hasArmor,
                                BedrockItemGenerationContext context) throws IOException {
        Key id = plan.id();
        BedrockItemGeometryBuilder mesh = new BedrockItemGeometryBuilder(this.resources.models(), this.resources.textures());
        JsonArray selectors = new JsonArray();
        Map<Layer, List<String>> layers = collectEquipmentLayers(plan, selectors, hasArmor);
        List<BedrockItem3DAttachables.Stage> stages = new ObjectArrayList<>(layers.size());
        boolean hasHandModel = false;
        for (Map.Entry<Layer, List<String>> entry : layers.entrySet()) {
            Layer layer = entry.getKey();
            try {
                BedrockItemGeometry geometry = mesh.build(layer.part(), true);
                stages.add(new BedrockItem3DAttachables.Stage(geometry, String.join(" || ", entry.getValue()), equippedView(layer.view())));
                hasHandModel |= layer.view() != ItemDisplayContext.HEAD && !geometry.cubes().isEmpty();
            } catch (BedrockResourceException e) {
                warn(id, layer.view(), e);
            }
        }
        BedrockAttachablePose pose = plan.strategy().pose(plan.item());
        for (BedrockItemGeometry.Material material : mesh.materials()) {
            if (material.dyeColor() != null) {
                pose.scripts().add(BedrockDyedItem.colorScript());
                break;
            }
        }
        pose.scripts().addAll(selectors);
        BedrockItem3DAttachables.writeDisplayContexts(id, headId, stages, mesh.materials(), pose, armor, context.root());
        if (hasHandModel) {
            plan.strategy().registerHandModel(context.playerAnimations(), id, plan.item());
        }
    }

    private static Map<Layer, List<String>> collectEquipmentLayers(BedrockItemPresentation plan, JsonArray selectors, boolean hasArmor) {
        // 同一位置的同一模型部件只生成一次；任一使用条件成立时都应显示它。
        Map<Layer, List<String>> layers = new LinkedHashMap<>();
        for (ItemDisplayContext view : EQUIPMENT_VIEWS) {
            if (hasArmor && view == ItemDisplayContext.HEAD) {
                continue;
            }
            String viewCondition = equippedView(view).condition;
            List<ItemBindingRule> rules = plan.rulesByContext().get(view);
            String selector = "variable.ce_" + view.serializedName() + "_stage";
            if (rules.size() > 1) {
                // 每个位置按 Java 顺序选一个分支；被选中的 composite 可同时绘制多个部件。
                selectors.add(BedrockItemAttachables.stageSelection(selector, rules, plan.strategy()::stageCondition));
            }
            for (int i = 0; i < rules.size(); i++) {
                ItemBindingRule rule = rules.get(i);
                try {
                    String stage = rules.size() == 1 ? plan.strategy().stageCondition(rule) : selector + " == " + i;
                    String condition = "(" + viewCondition + ") && (" + stage + ")";
                    for (ItemBindingRule part : modelParts(rule)) {
                        layers.computeIfAbsent(new Layer(view, part), ignored -> new ObjectArrayList<>()).add(condition);
                    }
                } catch (BedrockResourceException e) {
                    warn(plan.id(), view, e);
                }
            }
        }
        return layers;
    }

    @SuppressWarnings("DeconstructionCanBeUsed")
    private static List<ItemBindingRule> modelParts(ItemBindingRule rule) {
        if (rule.target() == ItemRenderTarget.Builtin.EMPTY) {
            return List.of();
        }
        List<ItemBindingRule> parts = rule.target() instanceof ItemRenderTarget.Composite composite ? composite.parts() : List.of(rule);
        for (ItemBindingRule part : parts) {
            if (!(part.target() instanceof ItemRenderTarget.Model)) {
                throw new BedrockResourceException("resource_pack.bedrock.reason.composite_child", part.sourcePath());
            }
        }
        return parts;
    }

    static GeneratedItemVariant transparent(Key id, List<ItemBindingCondition> conditions, BedrockItemGenerationContext context) throws IOException {
        return writeIcon(id, conditions, new BedrockTexture(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)), context);
    }

    private static GeneratedItemVariant writeIcon(Key id, List<ItemBindingCondition> conditions, BedrockTexture image, BedrockItemGenerationContext context) throws IOException {
        String path = image.write(context.root(), "textures/" + id.namespace() + '/' + id.value() + "/icon");
        return GeneratedItemVariant.icon(id, conditions, path, false);
    }

    private static void warn(Key id, ItemDisplayContext view, BedrockResourceException e) {
        TranslationManager.warn("resource_pack.bedrock.item.display_skipped", id.asString(), view.serializedName(), e.getLocalizedMessage());
    }
    private record Layer(ItemDisplayContext view, ItemBindingRule part) {
    }

}
