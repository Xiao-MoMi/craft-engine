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

package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.core.block.behavior.BlockBehaviors;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.shared.block.EmptyBlockBehavior;

public class BukkitBlockBehaviors extends BlockBehaviors {
    public static final Key EMPTY = Key.from("craftengine:empty");
    public static final Key BUSH_BLOCK = Key.from("craftengine:bush_block");
    public static final Key FALLING_BLOCK = Key.from("craftengine:falling_block");
    public static final Key LEAVES_BLOCK = Key.from("craftengine:leaves_block");
    public static final Key STRIPPABLE_BLOCK = Key.from("craftengine:strippable_block");
    public static final Key SAPLING_BLOCK = Key.from("craftengine:sapling_block");

    public static void init() {
        register(EMPTY, (block, args) -> EmptyBlockBehavior.INSTANCE);
        register(FALLING_BLOCK, FallingBlockBehavior.FACTORY);
        register(BUSH_BLOCK, BushBlockBehavior.FACTORY);
        register(LEAVES_BLOCK, LeavesBlockBehavior.FACTORY);
        register(STRIPPABLE_BLOCK, StrippableBlockBehavior.FACTORY);
        register(SAPLING_BLOCK, SaplingBlockBehavior.FACTORY);
    }
}
