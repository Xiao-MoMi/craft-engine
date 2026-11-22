package net.momirealms.craftengine.bukkit.world.gen;

import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderProxy;

interface CustomStateProvider {
    Object getState(Object level, Object random, Object pos);

    static Object sample(Object provider, Object level, Object random, Object pos) {
        if (VersionHelper.isOrAbove26_3) {
            return BlockStateProviderProxy.INSTANCE.getStateInLevelAccessor(provider, level, random, pos);
        }
        if (VersionHelper.isOrAbove26_1) {
            return BlockStateProviderProxy.INSTANCE.getStateInWorldGenLevel(provider, level, random, pos);
        }
        return BlockStateProviderProxy.INSTANCE.getState(provider, random, pos);
    }
}
