package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.ImageUtils;
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
        if (!DisplayContextVariants.usesDisplayContext(entry) && !entry.definition().hasPlayerHead()) {
            return null;
        }
        List<DisplayContextVariants.Variant> variants = DisplayContextVariants.collect(entry);
        Key baseId = entry.bedrockId();
        boolean headEquipment = isHeadEquipment(entry);
        List<GeneratedItemVariant> output = new ObjectArrayList<>();
        for (int i = 0; i < variants.size(); i++) {
            Key id = variants.size() == 1 ? baseId : Key.of(baseId.namespace(), baseId.value() + "_variant_" + i);
            Key headId = headEquipment ? Key.of(id.namespace(), id.value() + "_view_head") : null;
            GeneratedItemVariant item = this.writeVariant(id, headId, entry.useBehavior(), variants.get(i), context);
            this.writeWorldAliases(item, context, output);
            output.add(item);
        }
        return output;
    }

    private static boolean isHeadEquipment(ItemRegistrationPlan.Entry entry) {
        JsonElement equipment = entry.properties().components().get(DataComponentKeys.EQUIPPABLE);
        return equipment != null && equipment.isJsonObject() && "head".equals(equipment.getAsJsonObject().get("slot").getAsString());
    }

    private GeneratedItemVariant writeVariant(Key id,
                                              @Nullable Key headId,
                                              ItemBehaviorDescription.Use use,
                                              DisplayContextVariants.Variant variant,
                                              BedrockItemGenerationContext context) throws IOException {
        EnumMap<ItemDisplayContext, ItemBindingRule> idleModels = new EnumMap<>(ItemDisplayContext.class);
        for (ItemDisplayContext view : ItemDisplayContext.values()) {
            idleModels.put(view, variant.idle(view));
        }

        // 用 GUI 的待机模型注册物品，条件取自整个变体，这样切换显示位置时仍能选中同一件物品。
        ItemBindingRule gui = idleModels.get(ItemDisplayContext.GUI);
        ItemBindingRule registration = new ItemBindingRule(variant.conditions(), gui.target(), gui.transformations(), gui.sourcePath());
        GeneratedItemVariant inventory;
        try {
            inventory = this.writeVisual(id, registration, gui, ItemDisplayContext.GUI, context);
        } catch (UnsupportedOperationException | IOException e) {
            // 背包外观生成失败时用透明图标完成注册，手持和世界模型仍可独立生成。
            warn(id, ItemDisplayContext.GUI, e);
            inventory = transparent(id, registration, context);
        }

        this.writeEquipment(id, headId, use, variant, context);
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
                GeneratedItemVariant visual = this.writeVisual(alias, item.rule(), rule, view, context);
                output.add(GeneratedItemVariant.worldAlias(visual, view));
            } catch (UnsupportedOperationException | IOException e) {
                warn(id, view, e);
            }
        }
    }

    private GeneratedItemVariant writeVisual(Key id, ItemBindingRule registration, ItemBindingRule model, ItemDisplayContext view,
                                            BedrockItemGenerationContext context) throws IOException {
        List<ItemBindingRule> parts = modelParts(model);
        if (parts.isEmpty()) {
            return transparent(id, registration, context);
        }
        // 只有所有部件都能画成图标时才合成贴图，否则交给展示方块保留整组模型的变换。
        BufferedImage icon = this.renderIcon(parts);
        if (icon != null) {
            return writeIcon(id, registration, icon, context);
        }
        BedrockItemBlocks.DisplayBlock block = context.itemDisplayBlocks().write(id, parts, this.resources.models(), this.resources.textures(),
                context.root(), DisplayPosition.valueOf(view.name()));
        return GeneratedItemVariant.block(id, registration, block, false);
    }

    @Nullable
    private BufferedImage renderIcon(List<ItemBindingRule> parts) throws IOException {
        List<BufferedImage> images = new ObjectArrayList<>(parts.size());
        for (ItemBindingRule part : parts) {
            if (!part.transformations().isEmpty()) {
                return null;
            }
            BufferedImage image = this.renderer.render((ItemRenderTarget.Model) part.target());
            if (image == null) {
                return null;
            }
            images.add(image);
        }
        return ImageUtils.composite(images);
    }

    private void writeEquipment(Key id, @Nullable Key headId, ItemBehaviorDescription.Use use, DisplayContextVariants.Variant variant,
                                BedrockItemGenerationContext context) throws IOException {
        BedrockWeaponGeometry mesh = new BedrockWeaponGeometry(this.resources.models(), this.resources.textures());
        Map<Layer, List<String>> layers = collectEquipmentLayers(id, variant);
        List<Bedrock3DItemAttachables.Stage> stages = new ObjectArrayList<>(layers.size());
        List<BedrockItemAttachables.View> views = new ObjectArrayList<>(layers.size());
        for (Map.Entry<Layer, List<String>> entry : layers.entrySet()) {
            Layer layer = entry.getKey();
            try {
                stages.add(mesh.createStage(layer.part(), String.join(" || ", entry.getValue())));
                views.add(equippedView(layer.view()));
            } catch (UnsupportedOperationException | IOException e) {
                warn(id, layer.view(), e);
            }
        }
        Bedrock3DItemAttachables.writeDisplayContexts(id, headId, stages, views, mesh.materials(), equipmentScripts(use), context.root());
    }

    private static Map<Layer, List<String>> collectEquipmentLayers(Key id, DisplayContextVariants.Variant variant) {
        // 同一位置的同一模型部件只生成一次；任一使用条件成立时都应显示它。
        Map<Layer, List<String>> layers = new LinkedHashMap<>();
        for (ItemDisplayContext view : EQUIPMENT_VIEWS) {
            String viewCondition = equippedView(view).condition;
            for (ItemBindingRule rule : variant.rules().get(view)) {
                try {
                    String condition = "(" + viewCondition + ") && (" + BedrockItemAttachables.useCondition(rule) + ")";
                    for (ItemBindingRule part : modelParts(rule)) {
                        layers.computeIfAbsent(new Layer(view, part), ignored -> new ObjectArrayList<>()).add(condition);
                    }
                } catch (UnsupportedOperationException e) {
                    warn(id, view, e);
                }
            }
        }
        return layers;
    }

    private static JsonArray equipmentScripts(ItemBehaviorDescription.Use use) {
        JsonArray scripts = new JsonArray();
        scripts.add("variable.ce_using_item = context.item_slot == 'main_hand' && query.is_using_item && query.main_hand_item_use_duration > 0;");
        scripts.add("variable.ce_use_ticks = variable.ce_using_item ? math.max(0, query.main_hand_item_max_duration - query.main_hand_item_use_duration) : 0;");
        scripts.add("variable.ce_remaining_ticks = variable.ce_using_item ? math.max(0, " + use.maximumDuration().value() + " - variable.ce_use_ticks) : 0;");
        return scripts;
    }

    private record Layer(ItemDisplayContext view, ItemBindingRule part) {
    }

    private static BedrockItemAttachables.View equippedView(ItemDisplayContext view) {
        return switch (view) {
            case FIRST_PERSON_RIGHT_HAND -> BedrockItemAttachables.View.FIRST_RIGHT;
            case FIRST_PERSON_LEFT_HAND -> BedrockItemAttachables.View.FIRST_LEFT;
            case THIRD_PERSON_RIGHT_HAND -> BedrockItemAttachables.View.THIRD_RIGHT;
            case THIRD_PERSON_LEFT_HAND -> BedrockItemAttachables.View.THIRD_LEFT;
            case HEAD -> BedrockItemAttachables.View.HEAD;
            default -> throw new IllegalArgumentException(view.name());
        };
    }

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
        return writeIcon(id, registration, new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), context);
    }

    private static GeneratedItemVariant writeIcon(Key id, ItemBindingRule registration, BufferedImage image, BedrockItemGenerationContext context) throws IOException {
        String path = "textures/" + id.namespace() + '/' + id.value() + "/icon";
        ImageUtils.writePng(image, context.root().resolve(path + ".png"));
        return GeneratedItemVariant.icon(id, registration, path, false);
    }

    private static void warn(Key id, ItemDisplayContext view, Exception e) {
        TranslationManager.warn("resource_pack.bedrock.item.display_skipped", id.asString(), view.serializedName(), e.toString());
    }
}
