package net.momirealms.craftengine.core.item.processor;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.network.NetworkItemHandler;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.TagParser;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class ComponentsProcessor implements ItemProcessor {
    public static final ItemProcessorFactory<ComponentsProcessor> FACTORY = new Factory();
    private final List<DynamicComponentProvider> arguments;
    private DynamicComponentProvider customData = null;

    public ComponentsProcessor(ConfigSection section) {
        Set<String> keys = section.keySet();
        List<DynamicComponentProvider> arguments = new ArrayList<>(keys.size());
        for (String key : keys) {
            Key id = Key.of(key);
            ConfigValue value = section.getValue(key);
            if (value == null) continue;
            if (DataComponentKeys.CUSTOM_DATA.equals(id)) {
                this.customData = getProvider(id, value);
            } else {
                arguments.add(getProvider(id, value));
            }
        }
        this.arguments = arguments;
    }

    public ComponentsProcessor(DynamicComponentProvider customData, List<DynamicComponentProvider> arguments) {
        this.customData = customData;
        this.arguments = arguments;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        for (DynamicComponentProvider argument : arguments) {
            item.setSparrowTagComponent(argument.type, argument.function.apply(context));
        }
        if (this.customData != null) {
            CompoundTag tag = (CompoundTag) item.getSparrowTag(DataComponentKeys.CUSTOM_DATA);
            if (tag != null) {
                for (Map.Entry<String, Tag> entry : ((CompoundTag) this.customData.function.apply(context)).entrySet()) {
                    tag.put(entry.getKey(), entry.getValue());
                }
                item.setComponent(DataComponentKeys.CUSTOM_DATA, tag);
            } else {
                item.setComponent(DataComponentKeys.CUSTOM_DATA, this.customData.function.apply(context));
            }
        }
        return item;
    }

    @Override
    public Item prepareNetworkItem(Item item, ItemBuildContext context, CompoundTag networkData) {
        for (DynamicComponentProvider argument : this.arguments) {
            String componentType = argument.type.asString();
            Tag previous = item.getComponentAsSparrowTag(componentType);
            if (previous != null) {
                networkData.put(componentType, NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put(componentType, NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        }
        return item;
    }

    public record DynamicComponentProvider(Key type, Function<ItemBuildContext, Tag> function) {
    }

    // todo 未来需要支持普通yaml格式使用 <>，最好先重构textprovider与Item
    private static DynamicComponentProvider getProvider(Key key, ConfigValue value) {
        if (value.is(String.class)) {
            String stringValue = value.getAsString();
            if (stringValue.startsWith("(json) ")) {
                // todo 需要未来先 tokenized 后再判断，而不是使用 < > 作为依据
                if (stringValue.contains("<") && stringValue.contains(">")) {
                    TextProvider provider = TextProviders.fromString(stringValue.substring("(json) ".length()));
                    return new DynamicComponentProvider(key, c -> {
                        JsonElement element = GsonHelper.get().fromJson(provider.get(c), JsonElement.class);
                        return CraftEngine.instance().platform().jsonToSparrowNBT(element);
                    });
                } else {
                    JsonElement element = GsonHelper.get().fromJson(stringValue.substring("(json) ".length()), JsonElement.class);
                    Tag tag = CraftEngine.instance().platform().jsonToSparrowNBT(element);
                    return new DynamicComponentProvider(key, c -> tag);
                }
            } else if (stringValue.startsWith("(snbt) ")) {
                String snbt = stringValue.substring("(snbt) ".length());
                if (stringValue.contains("<") && stringValue.contains(">")) {
                    TextProvider provider = TextProviders.fromString(snbt);
                    return new DynamicComponentProvider(key, c -> {
                        try {
                            return TagParser.parseTagFully(provider.get(c));
                        } catch (Exception e) {
                            throw new KnownResourceException(ConfigConstants.PARSE_SNBT_FAILED, value.path(), snbt, e.getMessage());
                        }
                    });
                } else {
                    try {
                        Tag tag = TagParser.parseTagFully(snbt);
                        return new DynamicComponentProvider(key, c -> tag);
                    } catch (Exception e) {
                        throw new KnownResourceException(ConfigConstants.PARSE_SNBT_FAILED, value.path(), snbt, e.getMessage());
                    }
                }
            }
        }
        Tag tag = CraftEngine.instance().platform().javaToSparrowNBT(value.value());
        return new DynamicComponentProvider(key, c -> tag);
    }

    private static class Factory implements ItemProcessorFactory<ComponentsProcessor> {

        @Override
        public ComponentsProcessor create(ConfigValue value) {
            ConfigSection componentsSection = value.getAsSection();
            DynamicComponentProvider customData = null;
            List<DynamicComponentProvider> arguments = new ArrayList<>();
            for (String key : componentsSection.keySet()) {
                Key id = Key.of(key);
                ConfigValue componentValue = componentsSection.getValue(key);
                if (componentValue == null) continue;
                if (DataComponentKeys.CUSTOM_DATA.equals(id)) {
                    customData = getProvider(id, componentValue);
                } else {
                    arguments.add(getProvider(id, componentValue));
                }
            }
            return new ComponentsProcessor(customData, arguments);
        }
    }
}
