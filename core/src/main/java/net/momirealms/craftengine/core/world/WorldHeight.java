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

public interface WorldHeight {

    int getHeight();

    int getMinBuildHeight();

    default int getMaxBuildHeight() {
        return this.getMinBuildHeight() + this.getHeight();
    }

    default int getSectionsCount() {
        return this.getMaxSection() - this.getMinSection();
    }

    default int getMinSection() {
        return SectionPos.blockToSectionCoord(this.getMinBuildHeight());
    }

    default int getMaxSection() {
        return SectionPos.blockToSectionCoord(this.getMaxBuildHeight() - 1) + 1;
    }

    default boolean isOutsideBuildHeight(BlockPos pos) {
        return this.isOutsideBuildHeight(pos.y());
    }

    default boolean isOutsideBuildHeight(int y) {
        return y < this.getMinBuildHeight() || y >= this.getMaxBuildHeight();
    }

    default int getSectionIndex(int y) {
        return this.getSectionIndexFromSectionY(SectionPos.blockToSectionCoord(y));
    }

    default int getSectionIndexFromSectionY(int coord) {
        return coord - this.getMinSection();
    }

    default int getSectionYFromSectionIndex(int index) {
        return index + this.getMinSection();
    }

    static WorldHeight create(final int bottomY, final int height) {
        return new WorldHeightImpl(bottomY, height);
    }

    class WorldHeightImpl implements WorldHeight {
        private final int bottomY;
        private final int height;
        private final int minSection;
        public WorldHeightImpl(final int bottomY, final int height) {
            this.bottomY = bottomY;
            this.height = height;
            this.minSection = SectionPos.blockToSectionCoord(this.getMinBuildHeight());
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public int getMinBuildHeight() {
            return bottomY;
        }

        @Override
        public int getMinSection() {
            return this.minSection;
        }
    }
}
