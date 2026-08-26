package net.momirealms.craftengine.core.item.processor;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.network.NetworkItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemHandler;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.plugin.context.text.StringTemplate;
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
    public void apply(ItemBuildContext context) {
        Item item = context.item();
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
    }

    @Override
    public void prepareNetworkItem(NetworkItemBuildContext context, CompoundTag networkData) {
        Item item = context.item();
        for (DynamicComponentProvider argument : this.arguments) {
            String componentType = argument.type.asString();
            Tag previous = item.getComponentAsSparrowTag(componentType);
            if (previous != null) {
                networkData.put(componentType, NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put(componentType, NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        }
    }

    public record DynamicComponentProvider(Key type, Function<ItemBuildContext, Tag> function) {
    }

    public static ComponentsProcessor createSingle(Key type, ConfigValue value) {
        DynamicComponentProvider provider = getProvider(type, value);
        if (DataComponentKeys.CUSTOM_DATA.equals(type)) {
            return new ComponentsProcessor(provider, List.of());
        }
        return new ComponentsProcessor(null, List.of(provider));
    }

    private static DynamicComponentProvider getProvider(Key key, ConfigValue value) {
        if (value.is(String.class)) {
            String stringValue = value.getAsString();
            if (stringValue.startsWith("(json) ")) {
                String json = stringValue.substring("(json) ".length());
                StringTemplate template = StringTemplate.of(json);
                if (template.hasTags()) {
                    return new DynamicComponentProvider(key, c -> {
                        JsonElement element = GsonHelper.get().fromJson(template.render(c), JsonElement.class);
                        return CraftEngine.instance().platform().jsonToSparrowNBT(element);
                    });
                } else {
                    JsonElement element = GsonHelper.get().fromJson(json, JsonElement.class);
                    Tag tag = CraftEngine.instance().platform().jsonToSparrowNBT(element);
                    return new DynamicComponentProvider(key, c -> tag);
                }
            } else if (stringValue.startsWith("(snbt) ")) {
                String snbt = stringValue.substring("(snbt) ".length());
                StringTemplate template = StringTemplate.of(snbt);
                if (template.hasTags()) {
                    return new DynamicComponentProvider(key, c -> {
                        try {
                            return TagParser.parseTagFully(template.render(c));
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
