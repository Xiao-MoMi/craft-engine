package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.SignalGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import org.bukkit.Location;

import java.util.Optional;

public final class ToggleableLampBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<ToggleableLampBlockBehavior> FACTORY = new Factory();
    public final Property<Boolean> litProperty;
    public final Property<Boolean> poweredProperty;
    public final boolean canOpenWithHand;

    private ToggleableLampBlockBehavior(BlockDefinition block,
                                        Property<Boolean> litProperty,
                                        Property<Boolean> poweredProperty,
                                        boolean canOpenWithHand) {
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
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        World world = context.getLevel();
        if (player != null) {
            Location location = new Location((org.bukkit.World) world.platformWorld(), pos.x, pos.y, pos.z);
            if (!BukkitCraftEngine.instance().antiGriefProvider().test((org.bukkit.entity.Player) player.platformPlayer(), Flag.INTERACT, location)) {
                return InteractionResult.SUCCESS_AND_CANCEL;
            }
        }
        ToggleableLampBlockBehavior behavior = state.behavior().getFirst(ToggleableLampBlockBehavior.class);
        if (behavior == null) return InteractionResult.PASS;
        LevelWriterProxy.INSTANCE.setBlock(
                world.minecraftWorld(),
                LocationUtils.toBlockPos(pos),
                state.cycle(behavior.litProperty).customBlockState().minecraftState(),
                2
        );
        Optional.ofNullable(player).ifPresent(p -> p.swingHand(context.getHand()));
        return InteractionResult.SUCCESS_AND_CANCEL;
    }

    @Override
    public void onPlace(Object thisBlock, Object[] args) {
        if (this.poweredProperty == null) return;
        Object state = args[0];
        Object level = args[1];
        Object pos = args[2];
        Object oldState = args[3];
        if (BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getBlock(oldState) != BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getBlock(state) && ServerLevelProxy.CLASS.isInstance(level)) {
            Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(state);
            if (optionalCustomState.isEmpty()) return;
            checkAndFlip(optionalCustomState.get(), level, pos);
        }
    }

    @Override
    public void neighborChanged(Object thisBlock, Object[] args) {
        if (this.poweredProperty == null) return;
        Object blockState = args[0];
        Object world = args[1];
        if (!ServerLevelProxy.CLASS.isInstance(world)) return;
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return;
        Object blockPos = args[2];
        ImmutableBlockState customState = optionalCustomState.get();
        checkAndFlip(customState, world, blockPos);
    }

    private void checkAndFlip(ImmutableBlockState customState, Object level, Object pos) {
        boolean hasNeighborSignal = SignalGetterProxy.INSTANCE.hasNeighborSignal(level, pos);
        boolean isPowered = customState.get(this.poweredProperty);
        if (hasNeighborSignal != isPowered) {
            ImmutableBlockState blockState = customState;
            if (!isPowered) {
                blockState = blockState.cycle(this.litProperty);
            }
            LevelWriterProxy.INSTANCE.setBlock(level, pos, blockState.with(this.poweredProperty, hasNeighborSignal).customBlockState().minecraftState(), 3);
        }
    }

    private static class Factory implements BlockBehaviorFactory<ToggleableLampBlockBehavior> {
        private static final String[] CAN_OPEN_WITH_HAND = new String[] {"can_open_with_hand", "can_toggle_with_hand", "can-open-with-hand", "can-toggle-with-hand"};

        @Override
        public ToggleableLampBlockBehavior create(BlockDefinition block, ConfigSection section) {
            boolean canOpenWithHand = section.getBoolean(CAN_OPEN_WITH_HAND);
            return new ToggleableLampBlockBehavior(
                    block,
                    BlockBehaviorFactory.getProperty(section.path(), block, "lit", Boolean.class),
                    canOpenWithHand ? BlockBehaviorFactory.getOptionalProperty(block, "powered", Boolean.class) : BlockBehaviorFactory.getProperty(section.path(), block, "powered", Boolean.class),
                    canOpenWithHand
            );
        }
    }
}
