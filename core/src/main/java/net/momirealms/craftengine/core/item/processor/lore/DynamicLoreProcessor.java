package net.momirealms.craftengine.core.item.processor.lore;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.processor.ItemProcessorFactory;
import net.momirealms.craftengine.core.item.processor.SimpleNetworkItemProcessor;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class DynamicLoreProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<DynamicLoreProcessor> FACTORY = new Factory();
    public static final String CONTEXT_TAG_KEY = "craftengine:display_context";
    private final Map<String, LoreProcessor> displayContexts;
    private final LoreProcessor defaultModifier;

    public DynamicLoreProcessor(Map<String, LoreProcessor> displayContexts) {
        this.displayContexts = displayContexts;
        this.defaultModifier = displayContexts.values().iterator().next();
    }

    public Map<String, LoreProcessor> displayContexts() {
        return displayContexts;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        String displayContext = Optional.ofNullable(item.getTagAsJava(CONTEXT_TAG_KEY)).orElse(this.defaultModifier).toString();
        LoreProcessor lore = this.displayContexts.get(displayContext);
        if (lore == null) {
            lore = this.defaultModifier;
        }
        return lore.apply(item, context);
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.LORE;
    }

    @Override
    public Object[] nbtPath(Item item, ItemBuildContext context) {
        return LoreProcessor.NBT_PATH;
    }

    @Override
    public String nbtPathString(Item item, ItemBuildContext context) {
        return "display.Lore";
    }

    private static class Factory implements ItemProcessorFactory<DynamicLoreProcessor> {
        @Override
        public DynamicLoreProcessor create(ConfigValue value) {
            Map<String, LoreProcessor> dynamicLore = new LinkedHashMap<>();
            ConfigSection section = value.getAsSection();
            for (String key : section.keySet()) {
                dynamicLore.put(key, LoreProcessor.createLoreModifier(section.getNonNullValue(key, ConfigConstants.ARGUMENT_LIST)));
            }
            return new DynamicLoreProcessor(dynamicLore);
        }
    }
}
