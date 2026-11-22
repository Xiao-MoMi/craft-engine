package net.momirealms.craftengine.bukkit.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.util.random.SimpleWeightedRandomListProxy;
import net.momirealms.craftengine.proxy.minecraft.util.random.WeightedListProxy;

final class CustomWeightedStateProvider implements CustomStateProvider {
    private static final Codec<Object> ENTRIES_CODEC = VersionHelper.isOrAbove1_21_5
            ? WeightedListProxy.INSTANCE.nonEmptyCodec(CustomSimpleStateProvider.DIRECT_CODEC)
            : SimpleWeightedRandomListProxy.INSTANCE.wrappedCodec(CustomSimpleStateProvider.DIRECT_CODEC);
    static final MapCodec<CustomWeightedStateProvider> CODEC = ENTRIES_CODEC
            .xmap(CustomWeightedStateProvider::new, provider -> provider.entries)
            .fieldOf("entries");

    private final Object entries;

    private CustomWeightedStateProvider(Object entries) {
        this.entries = entries;
    }

    @Override
    public Object getState(Object level, Object random, Object pos) {
        Object selected = VersionHelper.isOrAbove1_21_5
                ? WeightedListProxy.INSTANCE.getRandomOrThrow(this.entries, random)
                : SimpleWeightedRandomListProxy.INSTANCE.getRandomValue(this.entries, random).orElseThrow(IllegalStateException::new);
        return ((CustomSimpleStateProvider) selected).getState(level, random, pos);
    }
}
