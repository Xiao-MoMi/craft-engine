/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public Direction getDirection() {
        return this.direction;
    }

    @Override
    public HitResult.Type getType() {
        return this.miss ? HitResult.Type.MISS : HitResult.Type.BLOCK;
    }

    public boolean isInside() {
        return this.inside;
    }

    public boolean isWorldBorderHit() {
        return this.worldBorderHit;
    }
}
