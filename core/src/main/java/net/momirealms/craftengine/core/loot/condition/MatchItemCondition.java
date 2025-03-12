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

package net.momirealms.craftengine.core.loot.condition;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.parameter.LootParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.*;

public class MatchItemCondition implements LootCondition {
    public static final Factory FACTORY = new Factory();
    private final Set<String> ids;
    private final boolean regexMatch;

    public MatchItemCondition(Collection<String> ids, boolean regexMatch) {
        this.ids = new HashSet<>(ids);
        this.regexMatch = regexMatch;
    }

    @Override
    public Key type() {
        return LootConditions.MATCH_ITEM;
    }

    @Override
    public boolean test(LootContext lootContext) {
        Optional<Item<?>> item = lootContext.getOptionalParameter(LootParameters.TOOL);
        if (item.isEmpty()) return false;
        Key key = item.get().id();
        String itemId = key.toString();
        if (this.regexMatch) {
            for (String regex : ids) {
                if (itemId.matches(regex)) {
                    return true;
                }
            }
        } else {
            return this.ids.contains(itemId);
        }
        return false;
    }

    public static class Factory implements LootConditionFactory {
        @Override
        public LootCondition create(Map<String, Object> arguments) {
            List<String> ids = MiscUtils.getAsStringList(arguments.get("id"));
            boolean regex = (boolean) arguments.getOrDefault("regex", false);
            return new MatchItemCondition(ids, regex);
        }
    }
}
