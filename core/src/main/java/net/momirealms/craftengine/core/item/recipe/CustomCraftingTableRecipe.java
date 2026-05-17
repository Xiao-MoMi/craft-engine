package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public abstract class CustomCraftingTableRecipe extends AbstractGroupedRecipe
        implements ConditionalRecipe, VisualResultRecipe, FunctionalRecipe {
    protected final CraftingRecipeCategory category;
    private final CustomRecipeResult visualResult;
    private final Function<Context>[] craftingFunctions;
    private final Predicate<Context> craftingCondition;
    private final boolean alwaysRebuildResult;

    protected CustomCraftingTableRecipe(Key id,
                                        boolean showNotification,
                                        CustomRecipeResult result,
                                        @Nullable CustomRecipeResult visualResult,
                                        String group,
                                        CraftingRecipeCategory category,
                                        Function<Context>[] craftingFunctions,
                                        Predicate<Context> craftingCondition,
                                        boolean alwaysRebuildResult) {
        super(id, showNotification, result, group);
        this.category = category == null ? CraftingRecipeCategory.MISC : category;
        this.visualResult = visualResult;
        this.craftingFunctions = craftingFunctions;
        this.craftingCondition = craftingCondition;
        this.alwaysRebuildResult = alwaysRebuildResult;
    }

    public boolean alwaysRebuildOutput() {
        return alwaysRebuildResult;
    }

    @Override
    public boolean canUse(Context context) {
        if (this.craftingCondition == null) return true;
        return this.craftingCondition.test(context);
    }

    @Override
    public boolean hasCondition() {
        return this.craftingCondition != null;
    }

    public CraftingRecipeCategory category() {
        return this.category;
    }

    @Override
    public RecipeType type() {
        return RecipeType.CRAFTING;
    }

    @Override
    public CustomRecipeResult visualResult() {
        return this.visualResult;
    }

    @Override
    public Function<Context>[] functions() {
        return this.craftingFunctions;
    }
}
