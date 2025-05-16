package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.modifier.ItemDataModifier;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.plugin.event.EventTrigger;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public interface CustomItem<I> extends BuildableItem<I> {

    Key id();

    Key material();

    NetworkItemDataProcessor<I>[] networkItemDataProcessors();

    ItemDataModifier<I>[] dataModifiers();

    Map<String, ItemDataModifier<I>> dataModifierMap();

    boolean hasClientBoundDataModifier();

    ItemDataModifier<I>[] clientBoundDataModifiers();

    Map<String, ItemDataModifier<I>> clientBoundDataModifierMap();

    ItemSettings settings();

    default boolean is(Key tag) {
        return settings().tags().contains(tag);
    }

    default Item<I> buildItem(Player player) {
        return buildItem(ItemBuildContext.of(player));
    }

    Item<I> buildItem(ItemBuildContext context);

    void execute(PlayerOptionalContext context, EventTrigger trigger);

    @NotNull
    List<ItemBehavior> behaviors();

    interface Builder<I> {
        Builder<I> id(Key id);

        Builder<I> material(Key material);

        Builder<I> dataModifier(ItemDataModifier<I> modifier);

        Builder<I> dataModifiers(List<ItemDataModifier<I>> modifiers);

        Builder<I> clientBoundDataModifier(ItemDataModifier<I> modifier);

        Builder<I> clientBoundDataModifiers(List<ItemDataModifier<I>> modifiers);

        Builder<I> behavior(ItemBehavior behavior);

        Builder<I> behaviors(List<ItemBehavior> behaviors);

        Builder<I> settings(ItemSettings settings);

        Builder<I> events(EnumMap<EventTrigger, List<Function<PlayerOptionalContext>>> events);

        CustomItem<I> build();
    }
}
