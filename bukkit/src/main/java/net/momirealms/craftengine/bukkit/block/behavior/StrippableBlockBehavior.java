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

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.shared.block.BlockBehavior;

import java.util.Map;

public class StrippableBlockBehavior extends BlockBehavior {
    public static final Factory FACTORY = new Factory();
    private final Key stripped;

    public StrippableBlockBehavior(Key stripped) {
        this.stripped = stripped;
    }

    public Key stripped() {
        return stripped;
    }

    public static class Factory implements BlockBehaviorFactory {

        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            String stripped = (String) arguments.get("stripped");
            if (stripped == null) {
                throw new IllegalArgumentException("stripped is null");
            }
            return new StrippableBlockBehavior(Key.of(stripped));
        }
    }
}
