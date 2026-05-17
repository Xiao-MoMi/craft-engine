package net.momirealms.craftengine.core.block.entity.render.element;

import net.momirealms.craftengine.core.plugin.context.PlayerContext;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public abstract class AbstractConstantBlockEntityElement implements ConstantBlockEntityElement {
    protected final Predicate<PlayerContext> predicate;
    protected final boolean hasCondition;

    protected AbstractConstantBlockEntityElement(Predicate<PlayerContext> predicate, boolean hasCondition) {
        this.predicate = predicate;
        this.hasCondition = hasCondition;
    }

    @Override
    public @NotNull Predicate<PlayerContext> condition() {
        return this.predicate;
    }

    @Override
    public boolean hasCondition() {
        return this.hasCondition;
    }
}
