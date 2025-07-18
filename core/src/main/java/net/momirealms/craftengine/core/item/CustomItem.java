package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.modifier.ItemDataModifier;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.event.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.UniqueKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public interface CustomItem<I> extends BuildableItem<I> {

    Key id();

    UniqueKey uniqueId();

    Key material();

    Key clientBoundMaterial();

    ItemDataModifier<I>[] dataModifiers();

    boolean hasClientBoundDataModifier();

    ItemDataModifier<I>[] clientBoundDataModifiers();

    ItemSettings settings();

    default boolean is(Key tag) {
        return settings().tags().contains(tag);
    }

    void execute(PlayerOptionalContext context, EventTrigger trigger);

    @NotNull
    List<ItemBehavior> behaviors();

    interface Builder<I> {
        Builder<I> id(UniqueKey id);

        Builder<I> clientBoundMaterial(Key clientBoundMaterialKey);

        Builder<I> material(Key material);

        Builder<I> dataModifier(ItemDataModifier<I> modifier);

        Builder<I> dataModifiers(List<ItemDataModifier<I>> modifiers);

        Builder<I> clientBoundDataModifier(ItemDataModifier<I> modifier);

        Builder<I> clientBoundDataModifiers(List<ItemDataModifier<I>> modifiers);

        Builder<I> behavior(ItemBehavior behavior);

        Builder<I> behaviors(List<ItemBehavior> behaviors);

        Builder<I> settings(ItemSettings settings);

        Builder<I> events(Map<EventTrigger, List<Function<PlayerOptionalContext>>> events);

        CustomItem<I> build();
    }
}
