package net.momirealms.craftengine.bukkit.world.gen;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.proxy.minecraft.util.ExtraCodecsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;

import java.util.Map;
import java.util.function.Function;

final class CustomSimpleStateProvider implements CustomStateProvider {
    private static final Codec<CustomSimpleStateProvider> STRING_CODEC = ExtraCodecsProxy.INSTANCE.nonEmptyString()
            .xmap(name -> new CustomSimpleStateProvider(name, Map.of()), provider -> provider.name);
    private static final MapCodec<CustomSimpleStateProvider> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("Name").forGetter(provider -> provider.name),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("Properties", Map.of())
                    .forGetter(provider -> provider.properties)
    ).apply(instance, CustomSimpleStateProvider::new));
    static final Codec<CustomSimpleStateProvider> DIRECT_CODEC = Codec.either(STRING_CODEC, MAP_CODEC.codec()).xmap(
            either -> either.map(Function.identity(), Function.identity()),
            provider -> provider.properties.isEmpty() ? Either.left(provider) : Either.right(provider));
    static final MapCodec<CustomSimpleStateProvider> CODEC = DIRECT_CODEC.fieldOf("state");

    private final String name;
    private final Map<String, String> properties;
    private BlockStateWrapper cached;

    private CustomSimpleStateProvider(String name, Map<String, String> properties) {
        this.name = name;
        this.properties = properties;
    }

    @Override
    public Object getState(Object level, Object random, Object pos) {
        BlockStateWrapper state = this.cached;
        if (state != null) {
            return state.minecraftState();
        }
        state = CraftEngine.instance().blockManager().createBlockState(this.name);
        if (state == null) {
            return BlocksProxy.STONE$defaultState;
        }
        for (Map.Entry<String, String> property : this.properties.entrySet()) {
            state = state.withProperty(property.getKey(), property.getValue());
        }
        this.cached = state;
        return state.minecraftState();
    }
}
