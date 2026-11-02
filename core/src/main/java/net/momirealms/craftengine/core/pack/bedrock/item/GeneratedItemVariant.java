package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

public record GeneratedItemVariant(Key id, ItemBindingRule rule, @Nullable String iconTexture,
                                   @Nullable BedrockItemBlocks.DisplayBlock displayBlock, boolean attachable,
                                   @Nullable Integer dyeColor, @Nullable String unsupportedReason) {

    static GeneratedItemVariant icon(Key id, ItemBindingRule rule, String texture, boolean attachable) {
        return new GeneratedItemVariant(id, rule, texture, null, attachable, null, null);
    }

    static GeneratedItemVariant block(Key id, ItemBindingRule rule, BedrockItemBlocks.DisplayBlock display, boolean attachable) {
        return new GeneratedItemVariant(id, rule, null, display, attachable, null, null);
    }

    static GeneratedItemVariant skipped(Key id, ItemBindingRule rule, @Nullable String reason) {
        return new GeneratedItemVariant(id, rule, null, null, false, null, reason);
    }

    public boolean generated() {
        return this.iconTexture != null || this.displayBlock != null;
    }
}
