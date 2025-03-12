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
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.modifier.ItemModifier;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.context.ContextHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CustomItem<I> extends BuildableItem<I> {

    Key id();

    Key material();

    List<ItemModifier<I>> modifiers();

    ItemSettings settings();

    default Item<I> buildItem(Player player) {
        return buildItem(new ItemBuildContext(player, ContextHolder.EMPTY));
    }

    Item<I> buildItem(ItemBuildContext context);

    @NotNull
    List<ItemBehavior> behaviors();

    interface Builder<I> {
        Builder<I> id(Key id);

        Builder<I> material(Key material);

        Builder<I> modifiers(List<ItemModifier<I>> modifiers);

        Builder<I> modifier(ItemModifier<I> modifier);

        Builder<I> behavior(ItemBehavior behavior);

        Builder<I> behavior(List<ItemBehavior> behaviors);

        Builder<I> settings(ItemSettings settings);

        CustomItem<I> build();
    }
}
