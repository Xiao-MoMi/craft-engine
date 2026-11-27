package net.momirealms.craftengine.core.item.processor.lore;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.network.NetworkItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemData;
import net.momirealms.craftengine.core.item.processor.ItemProcessorFactory;
import net.momirealms.craftengine.core.item.processor.NetworkItemProcessor;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class RemoveLoreProcessor implements NetworkItemProcessor {
    public static final ItemProcessorFactory<RemoveLoreProcessor> FACTORY = new Factory();
    private final Pattern pattern;

    public RemoveLoreProcessor(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public void apply(ItemBuildContext context) {
        this.apply(context, null);
    }

    @Override
    public boolean applyNetworkItem(NetworkItemBuildContext context, @Nullable NetworkItemData networkData) {
        return this.apply(context, networkData);
    }

    private boolean apply(ItemBuildContext context, @Nullable NetworkItemData networkData) {
        Item item = context.item();
        List<Component> originalLore = item.loreComponent().orElse(null);
        if (originalLore == null || originalLore.isEmpty()) {
            return false;
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
            return false;
        }

        if (networkData != null) {
            this.prepareNetworkItem((NetworkItemBuildContext) context, networkData);
        }
        item.loreComponent(finalLore);
        return true;
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
