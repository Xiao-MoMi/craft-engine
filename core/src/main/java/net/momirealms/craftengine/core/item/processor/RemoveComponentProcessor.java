package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemHandler;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Collections;
import java.util.List;

public final class RemoveComponentProcessor implements ItemProcessor {
    public static final ItemProcessorFactory<RemoveComponentProcessor> FACTORY = new Factory();
    private final List<Key> arguments;

    public RemoveComponentProcessor(List<Key> arguments) {
        this.arguments = arguments;
    }

    public List<Key> components() {
        return Collections.unmodifiableList(this.arguments);
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        for (Key argument : this.arguments) {
            item.removeComponent(argument);
        }
        return item;
    }

    @Override
    public Item prepareNetworkItem(Item item, ItemBuildContext context, CompoundTag networkData) {
        for (Key component : this.arguments) {
            Tag previous = item.getComponentAsSparrowTag(component);
            if (previous != null) {
                networkData.put(component.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            }
        }
        return item;
    }

    private static class Factory implements ItemProcessorFactory<RemoveComponentProcessor> {

        @Override
        public RemoveComponentProcessor create(ConfigValue value) {
            return new RemoveComponentProcessor(value.getAsList(ConfigValue::getAsIdentifier));
        }
    }
}
