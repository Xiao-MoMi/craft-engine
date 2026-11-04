package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemBlocks;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record GeneratedItemVariant(Key id,
                                   ItemBindingRule rule,
                                   @Nullable String iconTexture,
                                   @Nullable BedrockItemBlocks.DisplayBlock displayBlock,
                                   boolean attachable,
                                   @Nullable Integer dyeColor,
                                   @Nullable String unsupportedReason,
                                   Map<ItemDisplayContext, ItemBindingRule> displayModels,
                                   @Nullable ItemDisplayContext worldView,
                                   @Nullable Key headId) {

    public GeneratedItemVariant(Key id,
                                ItemBindingRule rule,
                                @Nullable String iconTexture,
                                @Nullable BedrockItemBlocks.DisplayBlock displayBlock,
                                boolean attachable,
                                @Nullable Integer dyeColor,
                                @Nullable String unsupportedReason) {
        this(id, rule, iconTexture, displayBlock, attachable, dyeColor, unsupportedReason, Map.of(), null, null);
    }

    public static GeneratedItemVariant icon(Key id, ItemBindingRule rule, String texture, boolean attachable) {
        return new GeneratedItemVariant(id, rule, texture, null, attachable, null, null);
    }

    public static GeneratedItemVariant block(Key id, ItemBindingRule rule, BedrockItemBlocks.DisplayBlock display, boolean attachable) {
        return new GeneratedItemVariant(id, rule, null, display, attachable, null, null);
    }

    public static GeneratedItemVariant skipped(Key id, ItemBindingRule rule, @Nullable String reason) {
        return new GeneratedItemVariant(id, rule, null, null, false, null, reason);
    }

    public static GeneratedItemVariant worldAlias(GeneratedItemVariant visual, ItemDisplayContext view) {
        return new GeneratedItemVariant(visual.id(), visual.rule(), visual.iconTexture(), visual.displayBlock(), false, visual.dyeColor(), null, Map.of(), view, null);
    }

    public static GeneratedItemVariant displayContextItem(GeneratedItemVariant inventory, Map<ItemDisplayContext, ItemBindingRule> models, @Nullable Key headId) {
        return new GeneratedItemVariant(inventory.id(), inventory.rule(), inventory.iconTexture(), inventory.displayBlock(), true, inventory.dyeColor(), null, models, null, headId);
    }

    public boolean generated() {
        return this.iconTexture != null || this.displayBlock != null;
    }
}
