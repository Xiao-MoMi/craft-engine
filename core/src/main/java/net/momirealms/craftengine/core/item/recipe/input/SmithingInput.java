package net.momirealms.craftengine.core.item.recipe.input;

import net.momirealms.craftengine.core.item.recipe.UniqueIdItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SmithingInput<T> implements RecipeInput {
    private final UniqueIdItem<T> base;
    private final UniqueIdItem<T> template;
    private final UniqueIdItem<T> addition;

    public SmithingInput(@NotNull UniqueIdItem<T> base,
                         @Nullable UniqueIdItem<T> template,
                         @Nullable UniqueIdItem<T> addition) {
        this.base = base;
        this.template = template;
        this.addition = addition;
    }

    @NotNull
    public UniqueIdItem<T> base() {
        return base;
    }

    @Nullable
    public UniqueIdItem<T> template() {
        return template;
    }

    @Nullable
    public UniqueIdItem<T> addition() {
        return addition;
    }
}
