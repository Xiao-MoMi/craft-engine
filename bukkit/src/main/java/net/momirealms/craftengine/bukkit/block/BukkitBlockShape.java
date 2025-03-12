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

package net.momirealms.craftengine.bukkit.block;

import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.shared.block.BlockShape;

public class BukkitBlockShape implements BlockShape {
    private final Object rawBlockState;

    public BukkitBlockShape(Object rawBlockState) {
        this.rawBlockState = rawBlockState;
    }

    @Override
    public Object getShape(Object thisObj, Object[] args) throws Exception {
        return Reflections.method$BlockBehaviour$getShape.invoke(Reflections.field$StateHolder$owner.get(this.rawBlockState), this.rawBlockState, args[1], args[2], args[3]);
    }
}
