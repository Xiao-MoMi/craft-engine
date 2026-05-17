package net.momirealms.craftengine.core.item.recipe.input;

import net.momirealms.craftengine.core.item.recipe.UniqueIdItem;
import org.jetbrains.annotations.NotNull;

public final class SmithingInput implements RecipeInput {
    private final UniqueIdItem base;
    private final UniqueIdItem template;
    private final UniqueIdItem addition;

    public SmithingInput(@NotNull UniqueIdItem base,
                         @NotNull UniqueIdItem template,
                         @NotNull UniqueIdItem addition) {
        this.base = base;
        this.template = template;
        this.addition = addition;
    }

    @NotNull
    public UniqueIdItem base() {
        return this.base;
    }

    @NotNull
    public UniqueIdItem template() {
        return this.template;
    }

    @NotNull
    public UniqueIdItem addition() {
        return this.addition;
    }
}
