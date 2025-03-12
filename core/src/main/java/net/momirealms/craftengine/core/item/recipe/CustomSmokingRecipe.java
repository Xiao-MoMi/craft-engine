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

package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CustomSmokingRecipe<T> extends CustomCookingRecipe<T> {
    public static final Factory<?> FACTORY = new Factory<>();

    public CustomSmokingRecipe(Key id, CookingRecipeCategory category, String group, Ingredient<T> ingredient, int cookingTime, float experience, CustomRecipeResult<T> result) {
        super(id, category, group, ingredient, cookingTime, experience, result);
    }

    @Override
    public @NotNull Key type() {
        return RecipeTypes.SMOKING;
    }

    public static class Factory<A> implements RecipeFactory<CustomSmokingRecipe<A>> {

        @SuppressWarnings({"unchecked", "rawtypes", "DuplicatedCode"})
        @Override
        public Recipe<CustomSmokingRecipe<A>> create(Key id, Map<String, Object> arguments) {
            CookingRecipeCategory recipeCategory = arguments.containsKey("category") ? CookingRecipeCategory.valueOf(arguments.get("category").toString().toUpperCase(Locale.ENGLISH)) : null;
            String group = arguments.containsKey("group") ? arguments.get("group").toString() : null;
            int cookingTime = MiscUtils.getAsInt(arguments.getOrDefault("time", 80));
            float experience = MiscUtils.getAsFloat(arguments.getOrDefault("experience", 0.0f));
            List<String> items = MiscUtils.getAsStringList(arguments.get("ingredient"));
            Set<Holder<Key>> holders = new HashSet<>();
            for (String item : items) {
                if (item.charAt(0) == '#') {
                    holders.addAll(CraftEngine.instance().itemManager().tagToItems(Key.of(item.substring(1))));
                } else {
                    holders.add(BuiltInRegistries.OPTIMIZED_ITEM_ID.get(Key.of(item)).orElseThrow(() -> new IllegalArgumentException("Invalid vanilla/custom item: " + item)));
                }
            }
            return new CustomSmokingRecipe(
                    id,
                    recipeCategory,
                    group,
                    Ingredient.of(holders),
                    cookingTime,
                    experience,
                    parseResult(arguments)
            );
        }
    }
}
