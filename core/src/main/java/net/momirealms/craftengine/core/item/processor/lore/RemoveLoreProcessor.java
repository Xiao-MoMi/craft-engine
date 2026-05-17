package net.momirealms.craftengine.core.item.processor.lore;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.processor.ItemProcessorFactory;
import net.momirealms.craftengine.core.item.processor.SimpleNetworkItemProcessor;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class RemoveLoreProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<RemoveLoreProcessor> FACTORY = new Factory();
    private final Pattern pattern;

    public RemoveLoreProcessor(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        List<Component> originalLore = item.loreComponent().orElse(null);
        if (originalLore == null || originalLore.isEmpty()) {
            return item;
        }

        List<Component> finalLore = new ArrayList<>(originalLore.size());
        boolean hasRemoved = false;

        for (Component line : originalLore) {
            String plainText = AdventureHelper.plainTextContent(line);
            if (this.pattern.matcher(plainText).find()) {
                hasRemoved = true;
                continue;
            }
            finalLore.add(line);
        }

        if (!hasRemoved) {
            return item;
        }

        return item.loreComponent(finalLore);
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

    private static class Factory implements ItemProcessorFactory<RemoveLoreProcessor> {

        @Override
        public RemoveLoreProcessor create(ConfigValue value) {
            if (value.is(Map.class)) {
                ConfigSection section = value.getAsSection();
                Pattern pattern = Pattern.compile(section.getNonNullString("pattern"));
                return new RemoveLoreProcessor(pattern);
            } else {
                return new RemoveLoreProcessor(Pattern.compile(value.getAsString()));
            }
        }
    }
}