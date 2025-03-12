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

package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.context.ContextHolder;

public interface BuildableItem<I> {

    Key id();

    I buildItemStack(ItemBuildContext context, int count);

    default I buildItemStack(ItemBuildContext context) {
        return buildItemStack(context, 1);
    }

    default I buildItemStack() {
        return buildItemStack(ItemBuildContext.EMPTY, 1);
    }

    default I buildItemStack(int count) {
        return buildItemStack(ItemBuildContext.EMPTY, count);
    }

    default I buildItemStack(Player player) {
        return this.buildItemStack(new ItemBuildContext(player, ContextHolder.EMPTY), 1);
    }

    default I buildItemStack(Player player, int count) {
        return this.buildItemStack(new ItemBuildContext(player, ContextHolder.EMPTY), count);
    }
}
