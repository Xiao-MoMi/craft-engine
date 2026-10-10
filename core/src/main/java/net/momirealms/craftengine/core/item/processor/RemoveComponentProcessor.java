package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemData;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public final class RemoveComponentProcessor implements NetworkItemProcessor {
    public static final ItemProcessorFactory<RemoveComponentProcessor> FACTORY = new Factory();
    private final List<Key> arguments;

    public RemoveComponentProcessor(List<Key> arguments) {
        this.arguments = arguments;
    }

    public List<Key> components() {
        return Collections.unmodifiableList(this.arguments);
    }

    @Override
    public void apply(ItemBuildContext context) {
        Item item = context.item();
        for (Key argument : this.arguments) {
            item.removeComponent(argument);
        }
    }

    @Override
    public boolean applyNetworkItem(NetworkItemBuildContext context, @Nullable NetworkItemData networkData) {
        Item item = context.item();
        boolean changed = false;
        for (Key component : this.arguments) {
            if (item.hasComponent(component)) {
                if (networkData != null) {
                    networkData.saveComponent(component);
                }
                item.removeComponent(component);
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public void prepareNetworkItem(NetworkItemBuildContext context, NetworkItemData networkData) {
        for (Key component : this.arguments) {
            if (!networkData.contains(component.asString()) && context.item().hasComponent(component)) {
                networkData.saveComponent(component);
            }
        }
    }

    private static class Factory implements ItemProcessorFactory<RemoveComponentProcessor> {

        @Override
        public RemoveComponentProcessor create(ConfigValue value) {
            return new RemoveComponentProcessor(value.getAsList(ConfigValue::getAsIdentifier));
        }
    }
}
