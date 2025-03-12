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

package net.momirealms.craftengine.core.loot.entry;

import net.momirealms.craftengine.core.loot.LootContext;

import java.util.function.Consumer;

public interface LootEntryContainer<T> {

    static <T> LootEntryContainer<T> alwaysFalse() {
        return (context, choiceConsumer) -> false;
    }

    static <T> LootEntryContainer<T> alwaysTrue() {
        return (context, choiceConsumer) -> true;
    }

    boolean expand(LootContext context, Consumer<LootEntry<T>> choiceConsumer);

    default LootEntryContainer<T> and(LootEntryContainer<T> other) {
        return (context, lootChoiceExpander) -> this.expand(context, lootChoiceExpander) && other.expand(context, lootChoiceExpander);
    }

    default LootEntryContainer<T> or(LootEntryContainer<T> other) {
        return (context, lootChoiceExpander) -> this.expand(context, lootChoiceExpander) || other.expand(context, lootChoiceExpander);
    }
}
