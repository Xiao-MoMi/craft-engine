package net.momirealms.craftengine.core.world;

import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.MiscUtils;

public class EntityHitResult {
    private final Direction direction;
    private final Vec3d position;
    private final BlockPos blockPos;

    public EntityHitResult(Direction direction, Vec3d position) {
        this.direction = direction;
        this.position = position;
        this.blockPos = getBlockPos();
    }

    public Direction direction() {
        return direction;
    }

    public Vec3d hitLocation() {
        return position;
    }

    private BlockPos getBlockPos() {
        int x = MiscUtils.floor(this.position.x);
        int y = MiscUtils.floor(this.position.y);
        int z = MiscUtils.floor(this.position.z);
        if (this.direction == Direction.UP) {
            if (this.position.y % 1 == 0) {
                y--;
            }
        } else if (this.direction == Direction.SOUTH) {
            if (this.position.z % 1 == 0) {
                z--;
            }
        } else if (this.direction == Direction.EAST) {
            if (this.position.x % 1 == 0) {
                x--;
            }
        }
        return new BlockPos(x, y, z);
    }

    public BlockPos blockPos() {
        return this.blockPos;
    }
}
