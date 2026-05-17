package net.momirealms.craftengine.core.item.recipe.transform;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;

public final class KeepComponents implements ItemTransformDataProcessor {
    public static final ItemTransformDataProcessor.Factory<KeepComponents> FACTORY = new Factory();
    private final List<Key> components;

    public KeepComponents(List<Key> components) {
        this.components = components;
    }

    @Override
    public void accept(Item item1, Item item2, Item item3) {
        for (Key component : this.components) {
            Object componentObj = item1.getExactComponent(component);
            if (componentObj != null) {
                item3.setExactComponent(component, componentObj);
            }
        }
    }

    private static class Factory implements ItemTransformDataProcessor.Factory<KeepComponents> {
        private static final Key CUSTOM_DATA = Key.of("minecraft", "custom_data");

        @Override
        public KeepComponents create(ConfigSection section) {
            return new KeepComponents(section.getNonEmptyList("components", ConfigValue::getAsIdentifier).stream().filter(it -> !CUSTOM_DATA.equals(it)).toList());
        }
    }
}