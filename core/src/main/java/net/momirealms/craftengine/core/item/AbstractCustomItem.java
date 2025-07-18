package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.modifier.ItemDataModifier;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.event.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.UniqueKey;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractCustomItem<I> implements CustomItem<I> {
    protected final UniqueKey id;
    protected final Key material;
    protected final Key clientBoundMaterial;
    protected final ItemDataModifier<I>[] modifiers;
    protected final ItemDataModifier<I>[] clientBoundModifiers;
    protected final List<ItemBehavior> behaviors;
    protected final ItemSettings settings;
    protected final Map<EventTrigger, List<Function<PlayerOptionalContext>>> events;

    @SuppressWarnings("unchecked")
    public AbstractCustomItem(UniqueKey id, Key material, Key clientBoundMaterial,
                              List<ItemBehavior> behaviors,
                              List<ItemDataModifier<I>> modifiers,
                              List<ItemDataModifier<I>> clientBoundModifiers,
                              ItemSettings settings,
                              Map<EventTrigger, List<Function<PlayerOptionalContext>>> events) {
        this.id = id;
        this.material = material;
        this.clientBoundMaterial = clientBoundMaterial;
        this.events = events;
        // unchecked cast
        this.modifiers = modifiers.toArray(new ItemDataModifier[0]);
        // unchecked cast
        this.clientBoundModifiers = clientBoundModifiers.toArray(new ItemDataModifier[0]);
        this.behaviors = List.copyOf(behaviors);
        this.settings = settings;
    }

    @Override
    public void execute(PlayerOptionalContext context, EventTrigger trigger) {
        for (Function<PlayerOptionalContext> function : Optional.ofNullable(this.events.get(trigger)).orElse(Collections.emptyList())) {
            function.run(context);
        }
    }

    @Override
    public Key id() {
        return this.id.key();
    }

    @Override
    public UniqueKey uniqueId() {
        return this.id;
    }

    @Override
    public Key material() {
        return this.material;
    }

    @Override
    public Key clientBoundMaterial() {
        return this.clientBoundMaterial;
    }

    @Override
    public ItemDataModifier<I>[] dataModifiers() {
        return this.modifiers;
    }

    @Override
    public boolean hasClientBoundDataModifier() {
        return this.clientBoundModifiers.length != 0;
    }

    @Override
    public ItemDataModifier<I>[] clientBoundDataModifiers() {
        return this.clientBoundModifiers;
    }

    @Override
    public ItemSettings settings() {
        return this.settings;
    }

    @Override
    public @NotNull List<ItemBehavior> behaviors() {
        return this.behaviors;
    }
}
