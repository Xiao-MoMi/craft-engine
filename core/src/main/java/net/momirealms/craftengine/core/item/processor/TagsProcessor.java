package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.network.NetworkItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemData;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class TagsProcessor implements NetworkItemProcessor {
    public static final ItemProcessorFactory<TagsProcessor> FACTORY = new Factory();
    private final Map<String, Object> arguments;

    public TagsProcessor(ConfigSection section) {
        this.arguments = new LinkedHashMap<>();
        for (String key : section.keySet()) {
            if (key.charAt(0) == '@') {
                this.arguments.put(key.substring(1), section.get(key));
            } else {
                ConfigValue value = section.getValue(key);
                if (value == null) continue;
                if (value.is(Map.class)) {
                    processTags(key, value.getAsSection(), this.arguments::put);
                } else {
                    this.arguments.put(key, value.value());
                }
            }
        }
    }

    public Map<String, Object> tags() {
        return this.arguments;
    }

    private void processTags(String path, ConfigSection section, BiConsumer<String, Object> callback) {
        for (String key : section.keySet()) {
            if (key.charAt(0) == '@') {
                callback.accept(path + "." + key.substring(1), section.get(key));
            } else {
                ConfigValue value = section.getValue(key);
                if (value == null) continue;
                if (value.is(Map.class)) {
                    processTags(path + "." + key, value.getAsSection(), callback);
                } else {
                    callback.accept(path + "." + key, value.value());
                }
            }
        }
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public void apply(ItemBuildContext context) {
        Item item = context.item();
        for (Map.Entry<String, Object> entry : this.arguments.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String[] split = key.split("\\.");
            item.setTag(value, (Object[]) split);
        }
    }

    @Override
    public void prepareNetworkItem(NetworkItemBuildContext context, NetworkItemData networkData) {
        if (VersionHelper.isOrAbove1_20_5) {
            networkData.saveComponent(DataComponentKeys.CUSTOM_DATA);
        } else {
            for (String key : this.arguments.keySet()) {
                networkData.saveTag(key);
            }
        }
    }

    private static class Factory implements ItemProcessorFactory<TagsProcessor> {

        @Override
        public TagsProcessor create(ConfigValue value) {
            return new TagsProcessor(value.getAsSection());
        }
    }
}
