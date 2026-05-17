package net.momirealms.craftengine.core.world.context;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.BlockHitResult;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import org.jetbrains.annotations.Nullable;

public class BlockPlaceContext extends UseOnContext {
    private final BlockPos relativePos;
    private boolean replaceClicked;

    public BlockPlaceContext(UseOnContext context) {
        this(context.getLevel(), context.getPlayer(), context.getHand(), context.getItem(), context.getHitResult());
    }

    public BlockPlaceContext(World world, @Nullable Player player, InteractionHand hand, Item stack, BlockHitResult hit) {
        super(world, player, hand, stack, hit);
        this.relativePos = hit.blockPos().relative(hit.direction());
        this.replaceClicked = true;
        this.replaceClicked = world.getBlock(hit.blockPos()).canBeReplaced(this);
    }

    @Override
    public BlockPos getClickedPos() {
        return this.replaceClicked ? super.getClickedPos() : this.relativePos;
    }

    public BlockPos getAgainstPos() {
        return super.getClickedPos();
    }

    public boolean canPlace() {
        return this.replaceClicked || this.getLevel().getBlock(this.getClickedPos()).canBeReplaced(this);
    }

    public boolean isWaterSource() {
        return this.getLevel().getBlock(this.getClickedPos()).isWaterSource(this);
    }

    public boolean replacingClickedBlock() {
        return this.replaceClicked;
    }

    public Direction getVerticalLookingDirection() {
        Player player = this.getPlayer();
        if (player != null) {
            return player.xRot() > 0 ? Direction.UP : Direction.DOWN;
        }
        return Direction.UP;
    }

    public Direction getNearestLookingDirection() {
        Player player = this.getPlayer();
        if (player == null) {
            return Direction.NORTH;
        }
        return Direction.orderedByNearest(player)[0];
    }

    public Direction[] getNearestLookingDirections() {
        Player player = this.getPlayer();
        if (player == null) {
            return new Direction[] { Direction.NORTH };
        }

        Direction[] directions = Direction.orderedByNearest(player);
        if (!this.replaceClicked) {
            Direction clickedFace = this.getClickedFace();
            int i = 0;

            while (i < directions.length && directions[i] != clickedFace.opposite()) {
                i++;
            }

            if (i > 0) {
                System.arraycopy(directions, 0, directions, 1, i);
                directions[0] = clickedFace.opposite();
            }

        }
        return directions;
    }
}
