package net.momirealms.craftengine.bukkit.entity.furniture.element;

import net.momirealms.craftengine.core.entity.furniture.element.ConditionalFurnitureElement;
import net.momirealms.craftengine.core.plugin.context.PlayerContext;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public abstract class AbstractConditionalFurnitureElement implements ConditionalFurnitureElement {
    protected final Predicate<PlayerContext> predicate;

    protected AbstractConditionalFurnitureElement(Predicate<PlayerContext> predicate) {
        this.predicate = predicate;
    }

    @Override
    @NotNull
    public Predicate<PlayerContext> condition() {
        return this.predicate;
    }
}
