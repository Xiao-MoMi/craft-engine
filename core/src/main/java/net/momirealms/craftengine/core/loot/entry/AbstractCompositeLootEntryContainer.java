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
import net.momirealms.craftengine.core.loot.condition.LootCondition;

import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractCompositeLootEntryContainer<T> extends AbstractLootEntryContainer<T> {
    protected final List<LootEntryContainer<T>> children;
    private final LootEntryContainer<T> composedChildren;

    protected AbstractCompositeLootEntryContainer(List<LootCondition> conditions, List<LootEntryContainer<T>> children) {
        super(conditions);
        this.children = children;
        this.composedChildren = compose(children);
    }

    protected abstract LootEntryContainer<T> compose(List<? extends LootEntryContainer<T>> children);

    @Override
    public final boolean expand(LootContext context, Consumer<LootEntry<T>> choiceConsumer) {
        return this.test(context) && this.composedChildren.expand(context, choiceConsumer);
    }
}
