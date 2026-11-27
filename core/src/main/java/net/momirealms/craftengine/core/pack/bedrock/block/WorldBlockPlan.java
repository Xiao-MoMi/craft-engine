package net.momirealms.craftengine.core.pack.bedrock.block;

import net.momirealms.craftengine.core.block.BlockSounds;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.plugin.CraftEngine;

import java.util.List;
import java.util.function.Function;

public record WorldBlockPlan(List<WorldBlockSource> customBlocks, List<VanillaOverride> vanillaOverrides) {
    public WorldBlockPlan {
        customBlocks = List.copyOf(customBlocks);
        vanillaOverrides = List.copyOf(vanillaOverrides);
    }

    public static WorldBlockPlan create(CraftEngine plugin,
                                        JavaBlockModelResolver models,
                                        List<WorldBlockSource> customBlocks,
                                        Function<BlockStateWrapper, VanillaProperties> vanillaProperties) {
        return new WorldBlockPlan(customBlocks, VanillaBlockOverrides.plan(plugin, models, customBlocks, vanillaProperties));
    }

    public record VanillaProperties(WorldBlockProperties components, BlockSounds sounds) {
    }

    public record VanillaOverride(BlockStateWrapper state,
                                  VanillaProperties properties,
                                  JavaBlockModelResolver.VanillaModelSelection model) {
    }
}
