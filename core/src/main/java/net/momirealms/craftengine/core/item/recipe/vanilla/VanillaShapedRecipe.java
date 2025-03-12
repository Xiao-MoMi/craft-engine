package net.momirealms.craftengine.core.item.recipe.vanilla;

import net.momirealms.craftengine.core.item.recipe.CraftingRecipeCategory;

import java.util.List;
import java.util.Map;

public class VanillaShapedRecipe extends VanillaCraftingRecipe {
    private final String[] pattern;
    private final Map<Character, List<String>> key;

    public VanillaShapedRecipe(CraftingRecipeCategory category,
                               String group,
                               Map<Character, List<String>> key,
                               String[] pattern,
                               RecipeResult result) {
        super(category, group, result);
        this.key = key;
        this.pattern = pattern;
    }

    public Map<Character, List<String>> ingredients() {
        return key;
    }

    public String[] pattern() {
        return pattern;
    }
}
