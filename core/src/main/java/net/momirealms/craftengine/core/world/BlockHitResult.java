package net.momirealms.craftengine.core.world;

import net.momirealms.craftengine.core.util.Direction;

public class BlockHitResult extends HitResult {
    private final Direction direction;
    private final BlockPos blockPos;
    private final boolean miss;
    private final boolean inside;
    private final boolean worldBorderHit;

    public static BlockHitResult miss(Vec3d pos, Direction side, BlockPos blockPos) {
        return new BlockHitResult(true, pos, side, blockPos, false, false);
    }

    public BlockHitResult(Vec3d pos, Direction side, BlockPos blockPos, boolean insideBlock) {
        this(false, pos, side, blockPos, insideBlock, false);
    }

    public BlockHitResult(Vec3d pos, Direction side, BlockPos blockPos, boolean insideBlock, boolean againstWorldBorder) {
        this(false, pos, side, blockPos, insideBlock, againstWorldBorder);
    }

    private BlockHitResult(boolean missed, Vec3d pos, Direction side, BlockPos blockPos, boolean insideBlock, boolean againstWorldBorder) {
        super(pos);
        this.miss = missed;
        this.direction = side;
        this.blockPos = blockPos;
        this.inside = insideBlock;
        this.worldBorderHit = againstWorldBorder;
    }

    public BlockHitResult withDirection(Direction side) {
        return new BlockHitResult(this.miss, this.location, side, this.blockPos, this.inside, this.worldBorderHit);
    }

    public BlockHitResult withPosition(BlockPos blockPos) {
        return new BlockHitResult(this.miss, this.location, this.direction, blockPos, this.inside, this.worldBorderHit);
    }

    public BlockHitResult hitBorder() {
        return new BlockHitResult(this.miss, this.location, this.direction, this.blockPos, this.inside, true);
    }

    public BlockPos blockPos() {
        return this.blockPos;
    }

    public Direction direction() {
        return this.direction;
    }

    @Override
    public HitResult.Type type() {
        return this.miss ? HitResult.Type.MISS : HitResult.Type.BLOCK;
    }

    public boolean isInside() {
        return this.inside;
    }

    public boolean isWorldBorderHit() {
        return this.worldBorderHit;
    }
}
