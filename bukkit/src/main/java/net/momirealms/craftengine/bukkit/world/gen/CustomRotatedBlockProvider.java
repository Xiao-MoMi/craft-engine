package net.momirealms.craftengine.bukkit.world.gen;

import com.mojang.serialization.MapCodec;
import net.momirealms.craftengine.core.block.DelegatingBlockState;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.proxy.minecraft.core.AxisProxy;
import net.momirealms.craftengine.proxy.minecraft.util.RandomSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.RotatedPillarBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.StateHolderProxy;

final class CustomRotatedBlockProvider implements CustomStateProvider {
    static final MapCodec<CustomRotatedBlockProvider> CODEC = CustomSimpleStateProvider.CODEC
            .xmap(CustomRotatedBlockProvider::new, provider -> provider.source);

    private final CustomSimpleStateProvider source;

    private CustomRotatedBlockProvider(CustomSimpleStateProvider source) {
        this.source = source;
    }

    @Override
    public Object getState(Object level, Object random, Object pos) {
        Object state = this.source.getState(level, random, pos);
        if (state instanceof DelegatingBlockState holder) {
            ImmutableBlockState customState = holder.blockState();
            if (customState == null || customState.isEmpty()) {
                return state;
            }
            return rotateCustomState(customState, random);
        }
        Object block = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getBlock(state);
        Object defaultState = BlockProxy.INSTANCE.getDefaultBlockState(block);
        return StateHolderProxy.INSTANCE.trySetValue(defaultState, RotatedPillarBlockProxy.AXIS, AxisProxy.INSTANCE.getRandom(random));
    }

    @SuppressWarnings("unchecked")
    private static Object rotateCustomState(ImmutableBlockState state, Object random) {
        Property<?> property = state.owner().value().getProperty("axis");
        if (property != null && property.valueClass() == Direction.Axis.class) {
            Direction.Axis axis = Direction.Axis.VALUES[RandomSourceProxy.INSTANCE.nextInt(random, Direction.Axis.VALUES.length)];
            state = state.with((Property<Direction.Axis>) property, axis);
        }
        return state.customBlockState().minecraftState();
    }
}
