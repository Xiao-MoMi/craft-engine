package net.momirealms.craftengine.core.pack.bedrock.block;

import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

record BlockTint(int color, @Nullable String method) {
    static final BlockTint NONE = new BlockTint(0xffffff, null);
    private static final BlockTint FOLIAGE = new BlockTint(0xffffff, "default_foliage");
    private static final BlockTint ATTACHED_STEM = new BlockTint(0xe0c71c, null);

    static BlockTint forState(BlockStateWrapper state) {
        Key block = state.ownerId();
        if (!block.isMinecraftNamespace()) {
            throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.tint"));
        }
        // Java 按显示方块选择颜色源。绊线等没有颜色源的方块即使模型标了 tintindex 也保留原色。
        return switch (block.value()) {
            case "oak_leaves", "jungle_leaves", "acacia_leaves", "dark_oak_leaves", "mangrove_leaves", "vine" -> FOLIAGE;
            case "attached_melon_stem", "attached_pumpkin_stem" -> ATTACHED_STEM;
            case "melon_stem", "pumpkin_stem" -> {
                int age = state.getProperty("age");
                yield new BlockTint(Color.toDecimal(0, age * 32, 255 - age * 8, age * 4), null);
            }
            case "large_fern", "tall_grass", "fern", "grass", "short_grass", "potted_fern", "bush", "grass_block",
                 "pink_petals", "wildflowers", "spruce_leaves", "birch_leaves", "leaf_litter", "water_cauldron",
                 "water", "bubble_column", "redstone_wire", "sugar_cane", "lily_pad" ->
                    throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.tint"));
            default -> NONE;
        };
    }
}