package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingCondition;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemDisplayBlocks;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record GeneratedItemVariant(Key id,
                                   List<ItemBindingCondition> conditions,
                                   @Nullable String iconTexture,
                                   @Nullable BedrockItemDisplayBlocks.DisplayBlock displayBlock,
                                   boolean attachable,
                                   @Nullable Integer dyeColor,
                                   Map<ItemDisplayContext, ItemBindingRule> displayModels,
                                   @Nullable ItemDisplayContext worldView,
                                   @Nullable Key headId,
                                   Map<ItemDisplayContext, Integer> tintSources) {

    public GeneratedItemVariant(Key id,
                                List<ItemBindingCondition> conditions,
                                @Nullable String iconTexture,
                                @Nullable BedrockItemDisplayBlocks.DisplayBlock displayBlock,
                                boolean attachable,
                                @Nullable Integer dyeColor,
                                Map<ItemDisplayContext, ItemBindingRule> displayModels,
                                @Nullable ItemDisplayContext worldView,
                                @Nullable Key headId) {
        this(id, conditions, iconTexture, displayBlock, attachable, dyeColor, displayModels, worldView, headId, Map.of());
    }

    public GeneratedItemVariant(Key id,
                                List<ItemBindingCondition> conditions,
                                @Nullable String iconTexture,
                                @Nullable BedrockItemDisplayBlocks.DisplayBlock displayBlock,
                                boolean attachable,
                                @Nullable Integer dyeColor) {
        this(id, conditions, iconTexture, displayBlock, attachable, dyeColor, Map.of(), null, null);
    }

    public GeneratedItemVariant withTintSources(Map<ItemDisplayContext, Integer> sources) {
        return new GeneratedItemVariant(this.id, this.conditions, this.iconTexture, this.displayBlock, this.attachable, this.dyeColor, this.displayModels, this.worldView, this.headId, Map.copyOf(sources));
    }

    public static GeneratedItemVariant icon(Key id, List<ItemBindingCondition> conditions, String texture, boolean attachable) {
        return new GeneratedItemVariant(id, conditions, texture, null, attachable, null);
    }

    public static GeneratedItemVariant block(Key id, List<ItemBindingCondition> conditions, BedrockItemDisplayBlocks.DisplayBlock display, boolean attachable) {
        return new GeneratedItemVariant(id, conditions, null, display, attachable, null);
    }

    public static GeneratedItemVariant worldAlias(GeneratedItemVariant visual, ItemDisplayContext view) {
        // 别名只替换指定世界场景的外观，沿用主变体的注册条件；不再生成自己的手持、头部或展示实体映射
        return new GeneratedItemVariant(visual.id(), visual.conditions(), visual.iconTexture(), visual.displayBlock(), false, visual.dyeColor(), Map.of(), view, null, visual.tintSources());
    }

    public static GeneratedItemVariant displayContextItem(GeneratedItemVariant inventory, Map<ItemDisplayContext, ItemBindingRule> models, @Nullable Key headId) {
        return new GeneratedItemVariant(inventory.id(), inventory.conditions(), inventory.iconTexture(), inventory.displayBlock(), true, inventory.dyeColor(), models, null, headId, inventory.tintSources());
    }

    public boolean generated() {
        return this.iconTexture != null || this.displayBlock != null;
    }
}
