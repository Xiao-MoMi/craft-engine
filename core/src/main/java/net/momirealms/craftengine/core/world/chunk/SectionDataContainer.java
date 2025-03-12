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
import org.jetbrains.annotations.Nullable;

public class SectionDataContainer {

    private final ImmutableBlockState[] states;

    public SectionDataContainer() {
        this.states = new ImmutableBlockState[4096];
    }

    public SectionDataContainer(ImmutableBlockState[] states) {
        this.states = states;
    }

    public @Nullable ImmutableBlockState get(int index) {
        return states[index];
    }

    public void set(int index, ImmutableBlockState value) {
        states[index] = value;
    }

    public ImmutableBlockState[] states() {
        return states;
    }
}
