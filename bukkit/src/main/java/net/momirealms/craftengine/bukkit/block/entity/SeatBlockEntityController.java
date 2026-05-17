package net.momirealms.craftengine.bukkit.block.entity;

import net.momirealms.craftengine.bukkit.block.behavior.SeatBlockBehavior;
import net.momirealms.craftengine.bukkit.entity.seat.BukkitSeat;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.entity.seat.Seat;
import net.momirealms.craftengine.core.entity.seat.SeatOwner;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.sparrow.nbt.CompoundTag;

public final class SeatBlockEntityController extends BlockEntityController implements SeatOwner {
    private final Seat<SeatBlockEntityController>[] seats;

    @SuppressWarnings("unchecked")
    public SeatBlockEntityController(BlockEntity blockEntity, SeatBlockBehavior behavior) {
        super(blockEntity);
        this.seats = new Seat[behavior.seats.length];
        for (int i = 0; i < seats.length; i++) {
            this.seats[i] = new BukkitSeat<>(this, behavior.seats[i]);
        }
    }

    @Override
    public void saveSeatEntityData(CompoundTag data) {
        data.putString("type", "seat_block_entity");
    }

    @Override
    public void onRemove() {
        for (Seat<SeatBlockEntityController> seat : this.seats) {
            seat.destroy();
        }
    }

    public boolean spawnSeat(Player player) {
        int yRot = 0;
        Property<Direction> facing = super.blockEntity.blockState.getProperty("facing");
        if (facing != null) {
            Direction direction = super.blockEntity.blockState.get(facing);
            yRot = switch (direction) {
                case DOWN, UP, NORTH -> 0;
                case SOUTH -> 180;
                case WEST -> 270;
                case EAST -> 90;
            };
        }
        for (Seat<SeatBlockEntityController> seat : this.seats) {
            if (!seat.isOccupied()) {
                if (seat.spawnSeat(player, new WorldPosition(super.blockEntity.world.world(), super.blockEntity.pos.x() + 0.5, super.blockEntity.pos.y(), super.blockEntity.pos.z() + 0.5, 0, yRot))) {
                    return true;
                }
            }
        }
        return false;
    }
}
