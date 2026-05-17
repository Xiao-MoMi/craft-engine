package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.item.behavior.EmptyItemBehavior;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractItemDefinition implements ItemDefinition {
    protected final boolean isVanillaItem;
    protected final UniqueKey id;
    protected final Key material;
    protected final Key clientBoundMaterial;
    protected final ItemProcessor[] processors;
    protected final ItemProcessor[] clientBoundProcessors;
    protected final ItemBehavior behavior;
    protected final ItemSettings settings;
    protected final Map<EventTrigger, List<Function<Context>>> events;
    protected final ItemUpdateConfig updater;

    public AbstractItemDefinition(boolean isVanillaItem, UniqueKey id, Key material, Key clientBoundMaterial,
                                  ItemBehavior behavior,
                                  List<ItemProcessor> processors,
                                  List<ItemProcessor> clientBoundProcessors,
                                  ItemSettings settings,
                                  Map<EventTrigger, List<Function<Context>>> events,
                                  ItemUpdateConfig updater) {
        this.isVanillaItem = isVanillaItem;
        this.id = id;
        this.material = material;
        this.clientBoundMaterial = clientBoundMaterial;
        this.events = events;
        // unchecked cast
        this.processors = processors.toArray(new ItemProcessor[0]);
        // unchecked cast
        this.clientBoundProcessors = clientBoundProcessors.toArray(new ItemProcessor[0]);
        this.behavior = Optional.ofNullable(behavior).orElse(EmptyItemBehavior.INSTANCE);
        this.settings = settings;
        this.updater = updater;
    }

    @Override
    public void execute(Context context, EventTrigger trigger) {
        for (Function<Context> function : Optional.ofNullable(this.events.get(trigger)).orElse(Collections.emptyList())) {
            function.run(context);
        }
    }

    @Override
    public Optional<ItemUpdateConfig> updater() {
        return Optional.ofNullable(this.updater);
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
    public ItemProcessor[] processors() {
        return this.processors;
    }

    @Override
    public boolean isVanillaItem() {
        return isVanillaItem;
    }

    @Override
    public boolean hasClientBoundProcessor() {
        return this.clientBoundProcessors.length != 0;
    }

    @Override
    public ItemProcessor[] clientBoundProcessors() {
        return this.clientBoundProcessors;
    }

    @Override
    public ItemSettings settings() {
        return this.settings;
    }

    @Override
    public @NotNull ItemBehavior behavior() {
        return this.behavior;
    }
}
