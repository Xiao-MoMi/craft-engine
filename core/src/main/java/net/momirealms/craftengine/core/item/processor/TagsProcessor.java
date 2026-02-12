package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.*;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class TagsProcessor implements ItemProcessor {
    public static final ItemProcessorFactory<TagsProcessor> FACTORY = new Factory();
    private final Map<String, Object> arguments;

    public TagsProcessor(Map<String, Object> arguments) {
        this.arguments = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            if (entry.getKey().charAt(0) == '@') {
                this.arguments.put(entry.getKey().substring(1), entry.getValue());
            } else {
                if (entry.getValue() instanceof Map<?,?> innerMap) {
                    processTags(entry.getKey(), MiscUtils.castToMap(innerMap, false), this.arguments::put);
                } else {
                    this.arguments.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public Map<String, Object> tags() {
        return this.arguments;
    }

    private void processTags(String path, Map<String, Object> arguments, BiConsumer<String, Object> callback) {
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            if (entry.getKey().charAt(0) == '@') {
                callback.accept(path + "." + entry.getKey().substring(1), entry.getValue());
            } else {
                if (entry.getValue() instanceof Map<?,?> innerMap) {
                    processTags(path + "." + entry.getKey(), MiscUtils.castToMap(innerMap, false), callback);
                } else {
                    callback.accept(path + "." + entry.getKey(), entry.getValue());
                }
            }
        }
    }

    @Override
    public <I> Item<I> apply(Item<I> item, ItemBuildContext context) {
        for (Map.Entry<String, Object> entry : this.arguments.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String[] split = key.split("\\.");
            item.setTag(value, (Object[]) split);
        }
        return item;
    }

    @Override
    public <I> Item<I> prepareNetworkItem(Item<I> item, ItemBuildContext context, CompoundTag networkData) {
        if (VersionHelper.isOrAbove1_20_5()) {
            Tag previous = item.getSparrowNBTComponent(DataComponentKeys.CUSTOM_DATA);
            if (previous != null) {
                networkData.put(DataComponentKeys.CUSTOM_DATA.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put(DataComponentKeys.CUSTOM_DATA.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        } else {
            for (Map.Entry<String, Object> entry : this.arguments.entrySet()) {
                String key = entry.getKey();
                String[] split = key.split("\\.");
                Tag previous = item.getTag((Object[]) split);
                if (previous != null) {
                    networkData.put(entry.getKey(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
                } else {
                    networkData.put(entry.getKey(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
                }
            }
        }
        return item;
    }

    private static class Factory implements ItemProcessorFactory<TagsProcessor> {

        @Override
        public TagsProcessor create(Object arg) {
            Map<String, Object> data = ResourceConfigUtils.getAsMap(arg, "nbt");
            return new TagsProcessor(data);
        }
    }
}
