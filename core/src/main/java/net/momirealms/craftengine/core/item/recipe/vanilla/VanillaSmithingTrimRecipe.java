package net.momirealms.craftengine.core.item.recipe.vanilla;

import net.momirealms.craftengine.core.item.recipe.RecipeTypes;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VanillaSmithingTrimRecipe implements VanillaRecipe {
    @Nullable // 1.21.5
    private final String pattern;

    private final List<String> base;
    private final List<String> template;
    private final List<String> addition;

    public VanillaSmithingTrimRecipe(List<String> base, List<String> template, List<String> addition, @Nullable String pattern) {
        this.base = base;
        this.template = template;
        this.addition = addition;
        this.pattern = pattern;
    }

    @Override
    public Key type() {
        return RecipeTypes.SMITHING_TRIM;
    }

    public List<String> base() {
        return base;
    }

    public List<String> template() {
        return template;
    }

    public List<String> addition() {
        return addition;
    }

    @Nullable
    public String pattern() {
        return pattern;
    }
}
