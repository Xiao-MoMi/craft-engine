package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.processor.ItemProcessor;
import net.momirealms.craftengine.core.item.setting.ItemSettings;
import net.momirealms.craftengine.core.item.updater.ItemUpdateConfig;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.UniqueKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ItemDefinition extends BuildableItem {

    /**
     * Since CraftEngine allows users to add certain functionalities to vanilla items, this custom item might actually be a vanilla item.
     * This will be refactored before the 1.0 release, but no changes will be made for now to ensure compatibility.
     */
    boolean isVanillaItem();

    Key id();

    UniqueKey uniqueId();

    default String translationKey() {
        Key id = this.id();
        return "item." + id.namespace() + "." + id.value();
    }

    Key material();

    Key clientBoundMaterial();

    ItemProcessor[] processors();

    boolean hasClientBoundProcessor();

    ItemProcessor[] clientBoundProcessors();

    ItemSettings settings();

    Optional<ItemUpdateConfig> updater();

    default boolean is(Key tag) {
        return settings().tags().contains(tag);
    }

    void execute(Context context, EventTrigger trigger);

    @NotNull
    ItemBehavior behavior();

    interface Builder {
        Builder isVanillaItem(boolean isVanillaItem);

        Builder id(UniqueKey id);

        Builder clientBoundMaterial(Key clientBoundMaterialKey);

        Builder material(Key material);

        Builder dataProcessor(ItemProcessor modifier);

        Builder dataProcessors(List<ItemProcessor> modifiers);

        Builder clientBoundProcessor(ItemProcessor modifier);

        Builder clientBoundProcessors(List<ItemProcessor> modifiers);

        Builder behavior(ItemBehavior behavior);

        Builder settings(ItemSettings settings);

        Builder updater(ItemUpdateConfig updater);

        Builder events(Map<EventTrigger, List<Function<Context>>> events);

        ItemDefinition build();
    }
}
