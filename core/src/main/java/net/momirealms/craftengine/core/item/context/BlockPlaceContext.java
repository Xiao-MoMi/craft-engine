package net.momirealms.craftengine.core.item.context;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.BlockHitResult;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;

public class BlockPlaceContext extends UseOnContext {
    private final BlockPos relativePos;
    protected boolean replaceClicked;

    public BlockPlaceContext(UseOnContext context) {
        this(context.getLevel(), context.getPlayer(), context.getHand(), context.getItem(), context.getHitResult());
    }

    public BlockPlaceContext(World world, Player player, InteractionHand hand, Item<?> stack, BlockHitResult hit) {
        super(world, player, hand, stack, hit);
        this.relativePos = hit.getBlockPos().relative(hit.getDirection());
        this.replaceClicked = world.getBlockAt(hit.getBlockPos()).canBeReplaced(this);
    }

    @Override
    public BlockPos getClickedPos() {
        return this.replaceClicked ? super.getClickedPos() : this.relativePos;
    }

    public BlockPos getAgainstPos() {
        return super.getClickedPos();
    }

    public boolean canPlace() {
        return this.replaceClicked || this.getLevel().getBlockAt(this.getClickedPos()).canBeReplaced(this);
    }

    public boolean isWaterSource() {
        return this.getLevel().getBlockAt(this.getClickedPos()).isWaterSource(this);
    }

    public boolean replacingClickedOnBlock() {
        return this.replaceClicked;
    }

    public Direction getNearestLookingDirection() {
        return Direction.orderedByNearest(this.getPlayer())[0];
    }
}
