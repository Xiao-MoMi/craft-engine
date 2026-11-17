package net.momirealms.craftengine.core.pack.bedrock.item.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.pack.bedrock.BedrockResourceException;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemGenerationContext;
import net.momirealms.craftengine.core.pack.bedrock.item.GeneratedItemVariant;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingCondition;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemDisplayBlocks;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockDyedItem;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemTint;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemIconRenderer;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTexture;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;

final class BedrockItemVisualWriter {
    private static final List<ItemDisplayContext> WORLD_VIEWS = List.of(
            ItemDisplayContext.GROUND,
            ItemDisplayContext.FIXED,
            ItemDisplayContext.ON_SHELF
    );
    private final JavaModelResources resources;
    private final BedrockItemIconRenderer renderer;

    BedrockItemVisualWriter(JavaModelResources resources, BedrockItemIconRenderer renderer) {
        this.resources = resources;
        this.renderer = renderer;
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

    private void writeWorldAlias(GeneratedItemVariant item,
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

    GeneratedItemVariant writeVisual(Key id,
                                     List<ItemBindingCondition> conditions,
                                     ItemBindingRule model,
                                     @Nullable DisplayPosition view,
                                     BedrockItemGenerationContext context) throws IOException {
        List<ItemBindingRule> parts = modelParts(model);
        if (parts.isEmpty()) {
            // 显式空模型仍是有效分支；透明图标让这个变体可以注册，同时保持该场景不可见。
            return transparent(id, conditions, Map.of(), context);
        }
        BedrockItemTint tint = BedrockItemTint.analyze(parts, this.resources);
        Integer dye = tint.defaultColor();
        if (dye != null) {
            return BedrockDyedItem.writeIcon(id, conditions, parts, this.resources, context, view == null ? DisplayPosition.GUI : view, dye)
                    .withTintSources(Map.of(ItemDisplayContext.GUI, tint.source()));
        }
        // 只有所有部件都能画成图标时才合成贴图，否则交给展示方块保留整组模型的变换。
        BedrockTexture icon = this.renderIcon(parts, true);
        if (icon != null) {
            return GeneratedItemVariant.icon(id, conditions, writeIconTexture(id, icon, context), false);
        }
        BedrockItemDisplayBlocks.DisplayBlock block = context.itemDisplayBlocks().write(id, parts, this.resources.models(), this.resources.textures(), context.root(), view);
        return GeneratedItemVariant.block(id, conditions, block, false);
    }

    @Contract("_, false -> !null")
    @Nullable
    BedrockTexture renderIcon(List<ItemBindingRule> parts, boolean allowBlockFallback) throws IOException {
        List<BedrockTexture> images = new ObjectArrayList<>(parts.size());
        for (ItemBindingRule part : parts) {
            if (allowBlockFallback && !part.transformations().isEmpty()) {
                return null;
            }
            BedrockTexture image = this.renderer.render((ItemRenderTarget.Model) part.target());
            if (image == null) {
                if (allowBlockFallback) {
                    return null;
                }
                throw new BedrockResourceException("resource_pack.bedrock.reason.static_icon", part.sourcePath());
            }
            images.add(image);
        }
        return BedrockTexture.composite(images);
    }

    @SuppressWarnings("DeconstructionCanBeUsed")
    static List<ItemBindingRule> modelParts(ItemBindingRule rule) {
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

    static GeneratedItemVariant transparent(Key id, List<ItemBindingCondition> conditions, Map<ItemDisplayContext, ItemBindingRule> displayModels,
                                            BedrockItemGenerationContext context) throws IOException {
        BedrockTexture image = new BedrockTexture(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
        String texture = writeIconTexture(id, image, context);
        return new GeneratedItemVariant(id, conditions, texture, null, false, null, displayModels, null, null);
    }

    static String writeIconTexture(Key id, BedrockTexture image, BedrockItemGenerationContext context) throws IOException {
        return image.write(context.root(), "textures/" + id.namespace() + '/' + id.value() + "/icon");
    }

    static void warn(Key id, ItemDisplayContext view, BedrockResourceException e) {
        TranslationManager.warn("resource_pack.bedrock.item.display_skipped", id.asString(), view.serializedName(), e.getLocalizedMessage());
    }
}
