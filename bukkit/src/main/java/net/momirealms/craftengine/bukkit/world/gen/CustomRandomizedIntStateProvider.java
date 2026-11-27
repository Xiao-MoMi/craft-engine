package net.momirealms.craftengine.bukkit.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.momirealms.craftengine.core.block.DelegatingBlockState;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.property.IntegerProperty;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.util.valueproviders.IntProviderProxy;
import net.momirealms.craftengine.proxy.minecraft.util.valueproviders.IntProvidersProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.StateHolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.properties.IntegerPropertyProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.properties.PropertyProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderProxy;

final class CustomRandomizedIntStateProvider implements CustomStateProvider {
    private static final Codec<Object> VALUES_CODEC = VersionHelper.isOrAbove26_1
            ? IntProvidersProxy.INSTANCE.codec()
            : IntProviderProxy.INSTANCE.codec();
    static final MapCodec<CustomRandomizedIntStateProvider> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockStateProviderProxy.INSTANCE.codec().fieldOf("source").forGetter(provider -> provider.source),
            Codec.STRING.fieldOf("property").forGetter(provider -> provider.propertyName),
            VALUES_CODEC.fieldOf("values").forGetter(provider -> provider.values)
    ).apply(instance, CustomRandomizedIntStateProvider::new));

    private final Object source;
    private final String propertyName;
    private final Object values;
    private Object property;
    private IntegerProperty customProperty;

    private CustomRandomizedIntStateProvider(Object source, String propertyName, Object values) {
        this.source = source;
        this.propertyName = propertyName;
        this.values = values;
    }

    @Override
    public Object getState(Object level, Object random, Object pos) {
        Object provider = VersionHelper.isOrAbove26_3 ? HolderProxy.INSTANCE.value(this.source) : this.source;
        Object state = CustomStateProvider.sample(provider, level, random, pos);
        if (state instanceof DelegatingBlockState holder) {
            ImmutableBlockState customState = holder.blockState();
            if (customState == null || customState.isEmpty()) {
                return state;
            }
            IntegerProperty property = this.customProperty;
            if (property == null || !customState.contains(property)) {
                property = this.findCustomProperty(customState);
                if (property == null) {
                    return state;
                }
                this.customProperty = property;
            }
            int value = IntProviderProxy.INSTANCE.sample(this.values, random);
            return customState.with(property, value).customBlockState().minecraftState();
        }

        Object property = this.property;
        if (property == null || !StateHolderProxy.INSTANCE.hasProperty(state, property)) {
            property = this.findProperty(state);
            if (property == null) {
                return state;
            }
            this.property = property;
        }
        return StateHolderProxy.INSTANCE.setValue(state, property, IntProviderProxy.INSTANCE.sample(this.values, random));
    }

    private IntegerProperty findCustomProperty(ImmutableBlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty integerProperty && property.name().equals(this.propertyName)) {
                return integerProperty;
            }
        }
        return null;
    }

    private Object findProperty(Object state) {
        for (Object property : StateHolderProxy.INSTANCE.getProperties(state)) {
            if (IntegerPropertyProxy.CLASS.isInstance(property) && PropertyProxy.INSTANCE.getName(property).equals(this.propertyName)) {
                return property;
            }
        }
        return null;
    }
}
