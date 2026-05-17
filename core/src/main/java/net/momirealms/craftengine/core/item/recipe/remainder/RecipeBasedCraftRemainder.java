package net.momirealms.craftengine.core.item.recipe.remainder;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RecipeBasedCraftRemainder implements CraftRemainder {
    public static final CraftRemainderFactory<RecipeBasedCraftRemainder> FACTORY = new Factory();
    private final Map<Key, CraftRemainder> remainders;
    @Nullable
    private final CraftRemainder fallback;

    public RecipeBasedCraftRemainder(Map<Key, CraftRemainder> remainders, @Nullable CraftRemainder fallback) {
        this.remainders = remainders;
        this.fallback = fallback;
    }

    @Override
    public Item remainder(Key recipeId, Item item) {
        CraftRemainder remainder = this.remainders.get(recipeId);
        if (remainder != null) {
            return remainder.remainder(recipeId, item);
        }
        return this.fallback != null ? this.fallback.remainder(recipeId, item) : null;
    }

    private static class Factory implements CraftRemainderFactory<RecipeBasedCraftRemainder> {
        private static final String[] CRAFT_REMAINDER = new String[] {"craft_remainder", "craft_remaining_item", "craft-remainder", "craft-remaining-item"};

        @Override
        public RecipeBasedCraftRemainder create(ConfigSection section) {
            Map<Key, CraftRemainder> remainders = new HashMap<>();
            List<GroupedRemainder> remainderList = section.getNonEmptyList("terms", v -> {
                ConfigSection termSection = v.getAsSection();
                List<Key> recipes = termSection.getNonEmptyList("recipes", ConfigValue::getAsIdentifier);
                CraftRemainder remainder = termSection.getValue(CRAFT_REMAINDER, CraftRemainders::fromConfig);
                return new GroupedRemainder(recipes, remainder);
            });
            for (GroupedRemainder remainder : remainderList) {
                for (Key recipeId : remainder.recipes) {
                    remainders.put(recipeId, remainder.remainder());
                }
            }
            return new RecipeBasedCraftRemainder(remainders, section.getValue("fallback", CraftRemainders::fromConfig));
        }

        public record GroupedRemainder(List<Key> recipes, CraftRemainder remainder) {
        }
    }
}
