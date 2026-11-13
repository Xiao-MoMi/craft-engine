package net.momirealms.craftengine.core.pack.bedrock.item.equipment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemGenerationContext;
import net.momirealms.craftengine.core.pack.bedrock.item.GeneratedItemVariant;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRegistrationPlan;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemBlocks;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemIconRenderer;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTexture;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 根据 Java 物品的显示位置规则，生成背包外观、手持和头部的 attachable，以及掉落物、展示框和置物架使用的别名。
 */
final class BedrockDisplayContextItems implements BedrockItemHandler {
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

    BedrockDisplayContextItems(JavaModelResources resources, BedrockItemIconRenderer renderer) {
        this.resources = resources;
        this.renderer = renderer;
    }

    @Override
    public List<GeneratedItemVariant> write(ItemRegistrationPlan.Entry entry, BedrockItemGenerationContext context) throws IOException {
        List<BedrockItemPresentation> plans = BedrockItemPresentation.plan(entry);
        if (plans == null) {
            return null;
        }
        List<GeneratedItemVariant> output = new ObjectArrayList<>();
        boolean headEquipment = isHeadEquipment(entry);
        for (BedrockItemPresentation plan : plans) {
            Key id = plan.id();
            Key headId = headEquipment ? Key.of(id.namespace(), id.value() + "_view_head") : null;
            GeneratedItemVariant item = this.writeVariant(plan, headId, context);
            if (plan.worldAliases()) {
                this.writeWorldAliases(item, context, output);
            }
            output.add(item);
        }
        return output;
    }

    private static boolean isHeadEquipment(ItemRegistrationPlan.Entry entry) {
        JsonElement equipment = entry.properties().components().get(DataComponentKeys.EQUIPPABLE);
        return equipment != null && equipment.isJsonObject() && "head".equals(equipment.getAsJsonObject().get("slot").getAsString());
    }

    private GeneratedItemVariant writeVariant(BedrockItemPresentation plan, @Nullable Key headId,
                                              BedrockItemGenerationContext context) throws IOException {
        EnumMap<ItemDisplayContext, ItemBindingRule> idleModels = plan.idleModels();
        if (headId == null) {
            GeneratedItemVariant nativeItem = plan.equipment().tryWriteSpecialized(plan, this.resources, this.renderer, context);
            if (nativeItem != null) {
                return GeneratedItemVariant.displayContextItem(nativeItem, idleModels, null);
            }
        }
        ItemBindingRule gui = idleModels.get(ItemDisplayContext.GUI);
        Key id = plan.id();
        ItemBindingRule registration = plan.registration();
        GeneratedItemVariant inventory;
        try {
            inventory = this.writeVisual(id, registration, gui, plan.worldAliases() ? DisplayPosition.GUI : null, context);
        } catch (UnsupportedOperationException | IOException e) {
            // 背包外观生成失败时用透明图标完成注册，手持和世界模型仍可独立生成。
            warn(id, ItemDisplayContext.GUI, e);
            inventory = transparent(id, registration, context);
        }

        this.writeEquipment(plan, headId, context);
        return GeneratedItemVariant.displayContextItem(inventory, idleModels, headId);
    }

    private void writeWorldAliases(GeneratedItemVariant item, BedrockItemGenerationContext context, List<GeneratedItemVariant> output) {
        ItemBindingRule gui = item.displayModels().get(ItemDisplayContext.GUI);
        for (ItemDisplayContext view : WORLD_VIEWS) {
            ItemBindingRule rule = item.displayModels().get(view);
            // 世界模型和变换与背包图标相同时可直接复用主物品。三维展示方块仍需按该位置生成姿态。
            if (item.displayBlock() == null && rule.target().equals(gui.target()) && rule.transformations().equals(gui.transformations())) {
                continue;
            }
            Key id = item.id();
            Key alias = Key.of(id.namespace(), id.value() + "_view_" + view.serializedName());
            try {
                GeneratedItemVariant visual = this.writeVisual(alias, item.rule(), rule, DisplayPosition.valueOf(view.name()), context);
                output.add(GeneratedItemVariant.worldAlias(visual, view));
            } catch (UnsupportedOperationException | IOException e) {
                warn(id, view, e);
            }
        }
    }

    private GeneratedItemVariant writeVisual(Key id, ItemBindingRule registration, ItemBindingRule model, @Nullable DisplayPosition view,
                                            BedrockItemGenerationContext context) throws IOException {
        List<ItemBindingRule> parts = modelParts(model);
        if (parts.isEmpty()) {
            return transparent(id, registration, context);
        }
        // 只有所有部件都能画成图标时才合成贴图，否则交给展示方块保留整组模型的变换。
        BedrockTexture icon = this.renderIcon(parts);
        if (icon != null) {
            return writeIcon(id, registration, icon, context);
        }
        BedrockItemBlocks.DisplayBlock block = context.itemDisplayBlocks().write(id, parts, this.resources.models(), this.resources.textures(),
                context.root(), view);
        return GeneratedItemVariant.block(id, registration, block, false);
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
                                BedrockItemGenerationContext context) throws IOException {
        Key id = plan.id();
        BedrockWeaponGeometry mesh = new BedrockWeaponGeometry(this.resources.models(), this.resources.textures());
        JsonArray selectors = new JsonArray();
        Map<Layer, List<String>> layers = collectEquipmentLayers(plan, selectors);
        List<Bedrock3DItemAttachables.Stage> stages = new ObjectArrayList<>(layers.size());
        List<BedrockItemAttachables.View> views = new ObjectArrayList<>(layers.size());
        boolean hasHandModel = false;
        for (Map.Entry<Layer, List<String>> entry : layers.entrySet()) {
            Layer layer = entry.getKey();
            try {
                Bedrock3DItemAttachables.Stage stage = mesh.createStage(layer.part(), String.join(" || ", entry.getValue()));
                stages.add(stage);
                views.add(equippedView(layer.view()));
                hasHandModel |= layer.view() != ItemDisplayContext.HEAD && !stage.cubes().isEmpty();
            } catch (UnsupportedOperationException | IOException e) {
                warn(id, layer.view(), e);
            }
        }
        BedrockEquipmentPose pose = plan.equipment().pose(plan.item());
        pose.scripts().addAll(selectors);
        Bedrock3DItemAttachables.writeDisplayContexts(id, headId, stages, views, mesh.materials(), pose, context.root());
        if (hasHandModel) {
            plan.equipment().registerHandModel(context.playerAnimations(), id, plan.item());
        }
    }

    private static Map<Layer, List<String>> collectEquipmentLayers(BedrockItemPresentation plan, JsonArray selectors) {
        // 同一位置的同一模型部件只生成一次；任一使用条件成立时都应显示它。
        Map<Layer, List<String>> layers = new LinkedHashMap<>();
        for (ItemDisplayContext view : EQUIPMENT_VIEWS) {
            String viewCondition = equippedView(view).condition;
            List<ItemBindingRule> rules = plan.variant().rules().get(view);
            String selector = "variable.ce_" + view.serializedName() + "_stage";
            if (rules.size() > 1) {
                // 每个位置按 Java 顺序选一个分支；被选中的 composite 可同时绘制多个部件。
                selectors.add(BedrockItemAttachables.stageSelection(selector, rules, plan.equipment()::stageCondition));
            }
            for (int i = 0; i < rules.size(); i++) {
                ItemBindingRule rule = rules.get(i);
                try {
                    String stage = rules.size() == 1 ? plan.equipment().stageCondition(rule) : selector + " == " + i;
                    String condition = "(" + viewCondition + ") && (" + stage + ")";
                    for (ItemBindingRule part : modelParts(rule)) {
                        layers.computeIfAbsent(new Layer(view, part), ignored -> new ObjectArrayList<>()).add(condition);
                    }
                } catch (UnsupportedOperationException e) {
                    warn(plan.id(), view, e);
                }
            }
        }
        return layers;
    }

    private record Layer(ItemDisplayContext view, ItemBindingRule part) {
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

    @SuppressWarnings("DeconstructionCanBeUsed")
    private static List<ItemBindingRule> modelParts(ItemBindingRule rule) {
        if (rule.target() == ItemRenderTarget.Builtin.EMPTY) {
            return List.of();
        }
        List<ItemBindingRule> parts = rule.target() instanceof ItemRenderTarget.Composite composite ? composite.parts() : List.of(rule);
        for (ItemBindingRule part : parts) {
            if (!(part.target() instanceof ItemRenderTarget.Model)) {
                throw new UnsupportedOperationException("Unsupported display model at " + part.sourcePath());
            }
        }
        return parts;
    }

    private static GeneratedItemVariant transparent(Key id, ItemBindingRule registration, BedrockItemGenerationContext context) throws IOException {
        return writeIcon(id, registration, new BedrockTexture(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)), context);
    }

    private static GeneratedItemVariant writeIcon(Key id, ItemBindingRule registration, BedrockTexture image, BedrockItemGenerationContext context) throws IOException {
        String path = image.write(context.root(), "textures/" + id.namespace() + '/' + id.value() + "/icon");
        return GeneratedItemVariant.icon(id, registration, path, false);
    }

    private static void warn(Key id, ItemDisplayContext view, Exception e) {
        TranslationManager.warn("resource_pack.bedrock.item.display_skipped", id.asString(), view.serializedName(), e.toString());
    }
}
