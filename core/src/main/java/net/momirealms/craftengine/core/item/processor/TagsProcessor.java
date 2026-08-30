package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.network.NetworkItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemHandler;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

// todo 更好支持参数
public final class TagsProcessor implements ItemProcessor {
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
    public void prepareNetworkItem(NetworkItemBuildContext context, CompoundTag networkData) {
        Item item = context.item();
        if (VersionHelper.isOrAbove1_20_5) {
            Tag previous = item.getComponentAsSparrowTag(DataComponentKeys.CUSTOM_DATA);
            if (previous != null) {
                networkData.put(DataComponentKeys.CUSTOM_DATA.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put(DataComponentKeys.CUSTOM_DATA.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        } else {
            for (Map.Entry<String, Object> entry : this.arguments.entrySet()) {
                String key = entry.getKey();
                String[] split = key.split("\\.");
                Tag previous = item.getSparrowTag((Object[]) split);
                if (previous != null) {
                    networkData.put(entry.getKey(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
                } else {
                    networkData.put(entry.getKey(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
                }
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
