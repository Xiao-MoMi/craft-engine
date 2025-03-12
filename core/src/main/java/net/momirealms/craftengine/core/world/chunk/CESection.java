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

package net.momirealms.craftengine.core.world.chunk;

import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.world.BlockPos;

public class CESection {
    public static final int SECTION_WIDTH = 16;
    public static final int SECTION_HEIGHT = 16;
    public static final int SECTION_SIZE = SECTION_WIDTH * SECTION_WIDTH * SECTION_HEIGHT;

    private final int sectionY;
    private final PalettedContainer<ImmutableBlockState> statesContainer;

    public CESection(int sectionY, PalettedContainer<ImmutableBlockState> statesContainer) {
        this.sectionY = sectionY;
        this.statesContainer = statesContainer;
    }

    public void setBlockState(BlockPos pos, ImmutableBlockState state) {
        setBlockState(pos.x() & 15, pos.y() & 15, pos.z() & 15, state);
    }

    public void setBlockState(int x, int y, int z, ImmutableBlockState state) {
        statesContainer.set((y << 4 | z) << 4 | x, state);
    }

    public void setBlockState(int index, ImmutableBlockState state) {
        statesContainer.set(index, state);
    }

    public ImmutableBlockState getBlockState(BlockPos pos) {
        return getBlockState(pos.x() & 15, pos.y() & 15, pos.z() & 15);
    }

    public ImmutableBlockState getBlockState(int x, int y, int z) {
        return statesContainer.get((y << 4 | z) << 4 | x);
    }

    public ImmutableBlockState getBlockState(int index) {
        return statesContainer.get(index);
    }

    public PalettedContainer<ImmutableBlockState> statesContainer() {
        return statesContainer;
    }

    public int sectionY() {
        return sectionY;
    }
}
