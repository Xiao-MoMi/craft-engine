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

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.condition.LootCondition;
import net.momirealms.craftengine.core.loot.function.LootFunction;
import net.momirealms.craftengine.core.loot.function.LootFunctions;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public abstract class AbstractSingleLootEntryContainer<T> extends AbstractLootEntryContainer<T> {
    protected final int weight;
    protected final int quality;
    protected final List<LootFunction<T>> functions;
    protected final BiFunction<Item<T>, LootContext, Item<T>> compositeFunction;
    private final EntryBase<T> entry = new EntryBase<T>() {
        @Override
        public void createItem(Consumer<Item<T>> lootConsumer, LootContext context) {
            AbstractSingleLootEntryContainer.this.createItem(
                    LootFunction.decorate(AbstractSingleLootEntryContainer.this.compositeFunction, lootConsumer, context), context
            );
        }
    };

    protected AbstractSingleLootEntryContainer(List<LootCondition> conditions, List<LootFunction<T>> functions, int weight, int quality) {
        super(conditions);
        this.weight = weight;
        this.quality = quality;
        this.functions = functions;
        this.compositeFunction = LootFunctions.compose(functions);
    }

    @Override
    public boolean expand(LootContext context, Consumer<LootEntry<T>> choiceConsumer) {
        if (super.test(context)) {
            choiceConsumer.accept(this.entry);
            return true;
        } else {
            return false;
        }
    }

    public int weight() {
        return weight;
    }

    public int quality() {
        return quality;
    }

    protected abstract void createItem(Consumer<Item<T>> lootConsumer, LootContext context);

    protected abstract class EntryBase<A> implements LootEntry<A> {
        // https://luckformula.emc.gs
        @Override
        public int getWeight(float luck) {
            float qualityModifier = (float) quality() * luck;
            final int weightBoost = 100;
            double baseWeight = weightBoost * (weight() + qualityModifier);
            double impacted = baseWeight * ((baseWeight - weightBoost) / weightBoost / 100);
            float luckModifier = Math.min(100, luck * 10) / 100;
            double reduced = Math.ceil(baseWeight - (impacted * luckModifier));
            return (int) Math.max(0, reduced);
        }
    }
}
