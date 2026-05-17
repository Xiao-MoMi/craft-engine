package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.entity.SeatBlockEntityController;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.EntityBlock;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.seat.SeatConfig;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.context.UseOnContext;

public final class SeatBlockBehavior extends BukkitBlockBehavior implements EntityBlock {
    public static final BlockBehaviorFactory<SeatBlockBehavior> FACTORY = new Factory();
    public final Property<Direction> directionProperty;
    public final SeatConfig[] seats;
    private int controllerId;

    private SeatBlockBehavior(BlockDefinition blockDefinition,
                              Property<Direction> directionProperty,
                              SeatConfig[] seats) {
        super(blockDefinition);
        this.seats = seats;
        this.directionProperty = directionProperty;
    }

    @Override
    public void initControllerId(int id) {
        this.controllerId = id;
    }

    @Override
    public BlockEntityController createBlockEntityController(BlockEntity blockEntity) {
        return new SeatBlockEntityController(blockEntity, this);
    }

    @Override
    public InteractionResult useWithoutItem(UseOnContext context, ImmutableBlockState state) {
        BukkitServerPlayer player = (BukkitServerPlayer) context.getPlayer();
        if (player == null || player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        CEWorld world = context.getLevel().storageWorld();
        BlockEntity blockEntity = world.getBlockEntityAtIfLoaded(pos);
        if (blockEntity == null) return InteractionResult.PASS;
        player.updateLastSuccessfulInteractionTick(player.gameTicks());
        BukkitCraftEngine.instance().scheduler().platform().runDelayed(() -> {
            blockEntity.controller.let(SeatBlockEntityController.class, this.controllerId, c -> {
                if (c.spawnSeat(player)) {
                    player.swingHand(context.getHand());
                }
            });
        }, null, player.platformPlayer());
        return InteractionResult.SUCCESS_AND_CANCEL;
    }

    private static class Factory implements BlockBehaviorFactory<SeatBlockBehavior> {

        @Override
        public SeatBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new SeatBlockBehavior(
                    block,
                    BlockBehaviorFactory.getOptionalProperty(block, "facing", Direction.class),
                    section.getList("seats", SeatConfig::fromConfig).toArray(new SeatConfig[0])
            );
        }
    }
}
