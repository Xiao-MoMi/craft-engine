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

package net.momirealms.craftengine.core.block;

import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class EmptyBlock extends CustomBlock {
    public static EmptyBlock INSTANCE;

    public EmptyBlock(Key id, Holder.Reference<CustomBlock> holder) {
        super(id, holder, Map.of(), Map.of(), Map.of(), BlockSettings.of(), null, null);
        INSTANCE = this;
    }

    @Override
    protected void applyPlatformSettings() {
    }
}
