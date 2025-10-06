package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.item.context.UseOnContext;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class ToggleableLampBlockBehavior extends BukkitBlockBehavior {
    public static final Factory FACTORY = new Factory();
    private final Property<Boolean> litProperty;
    private final Property<Boolean> poweredProperty;
    private final boolean canOpenWithHand;

    public ToggleableLampBlockBehavior(CustomBlock block, Property<Boolean> litProperty, Property<Boolean> poweredProperty, boolean canOpenWithHand) {
        super(block);
        this.litProperty = litProperty;
        this.poweredProperty = poweredProperty;
        this.canOpenWithHand = canOpenWithHand;
    }

    @Override
    public InteractionResult useWithoutItem(UseOnContext context, ImmutableBlockState state) {
        if (!this.canOpenWithHand) {
            return InteractionResult.PASS;
        }
        ToggleableLampBlockBehavior behavior = state.behavior().getAs(ToggleableLampBlockBehavior.class).orElse(null);
        if (behavior == null) return InteractionResult.PASS;
        FastNMS.INSTANCE.method$LevelWriter$setBlock(
                context.getLevel().serverWorld(),
                LocationUtils.toBlockPos(context.getClickedPos()),
                state.cycle(behavior.litProperty).customBlockState().literalObject(),
                2
        );
        Optional.ofNullable(context.getPlayer()).ifPresent(p -> p.swingHand(context.getHand()));
        return InteractionResult.SUCCESS_AND_CANCEL;
    }

    @Override
    public void onPlace(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        if (this.poweredProperty == null) return;
        Object state = args[0];
        Object level = args[1];
        Object pos = args[2];
        Object oldState = args[3];
        if (FastNMS.INSTANCE.method$BlockState$getBlock(oldState) != FastNMS.INSTANCE.method$BlockState$getBlock(state) && CoreReflections.clazz$ServerLevel.isInstance(level)) {
            Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(state);
            if (optionalCustomState.isEmpty()) return;
            checkAndFlip(optionalCustomState.get(), level, pos);
        }
    }

    @Override
    public void neighborChanged(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        if (this.poweredProperty == null) return;
        Object blockState = args[0];
        Object world = args[1];
        if (!CoreReflections.clazz$ServerLevel.isInstance(world)) return;
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return;
        Object blockPos = args[2];
        ImmutableBlockState customState = optionalCustomState.get();
        checkAndFlip(customState, world, blockPos);
    }

    private void checkAndFlip(ImmutableBlockState customState, Object level, Object pos) {
        boolean hasNeighborSignal = FastNMS.INSTANCE.method$SignalGetter$hasNeighborSignal(level, pos);
        boolean isPowered = customState.get(this.poweredProperty);
        if (hasNeighborSignal != isPowered) {
            ImmutableBlockState blockState = customState;
            if (!isPowered) {
                blockState = blockState.cycle(this.litProperty);
            }
            FastNMS.INSTANCE.method$LevelWriter$setBlock(level, pos, blockState.with(this.poweredProperty, hasNeighborSignal).customBlockState().literalObject(), 3);
        }

    }

    @SuppressWarnings("unchecked")
    public static class Factory implements BlockBehaviorFactory {
        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            boolean canOpenWithHand = ResourceConfigUtils.getAsBoolean(ResourceConfigUtils.get(arguments, "can-open-with-hand", "can-toggle-with-hand"), "can-toggle-with-hand");
            Property<Boolean> lit = (Property<Boolean>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("lit"), "warning.config.block.behavior.toggleable_lamp.missing_lit");
            Property<Boolean> powered = (Property<Boolean>) (canOpenWithHand ? block.getProperty("powered") : ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("powered"), "warning.config.block.behavior.toggleable_lamp.missing_powered"));
            return new ToggleableLampBlockBehavior(block, lit, powered, canOpenWithHand);
        }
    }
}
