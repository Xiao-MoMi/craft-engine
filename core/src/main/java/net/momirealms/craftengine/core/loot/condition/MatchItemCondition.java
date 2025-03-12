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
