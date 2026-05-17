package net.momirealms.craftengine.core.item.processor.lore;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.processor.ItemProcessorFactory;
import net.momirealms.craftengine.core.item.processor.SimpleNetworkItemProcessor;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class InsertLoreProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<InsertLoreProcessor> FACTORY = new Factory();
    private final Pattern pattern;
    private final Position position;
    private final LoreModification[] lores;
    private final InsertLoreProcessor fallback;

    public InsertLoreProcessor(Position position, Pattern pattern, LoreModification[] lores, InsertLoreProcessor fallback) {
        this.pattern = pattern;
        this.position = position;
        this.lores = lores;
        this.fallback = fallback;
    }

    public Stream<Component> getLore(ItemBuildContext context) {
        Stream<Component> stream = Stream.empty();
        if (this.lores != null) {
            for (LoreModification mod : this.lores) {
                stream = mod.apply(stream, context);
            }
        }
        return stream;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        List<Component> loreToInsert = getLore(context).toList();
        if (loreToInsert.isEmpty()) {
            return item;
        }

        List<Component> originalLore = item.loreComponent().orElse(List.of());
        List<Component> finalLore = new ArrayList<>(originalLore.size() + loreToInsert.size());

        switch (this.position) {
            case HEAD -> {
                finalLore.addAll(loreToInsert);
                finalLore.addAll(originalLore);
            }
            case TAIL -> {
                finalLore.addAll(originalLore);
                finalLore.addAll(loreToInsert);
            }
            case AFTER, BEFORE -> {
                int targetIndex = -1;
                if (this.pattern != null && !originalLore.isEmpty()) {
                    for (int i = 0; i < originalLore.size(); i++) {
                        String plainText = AdventureHelper.plainTextContent(originalLore.get(i));
                        if (this.pattern.matcher(plainText).find()) {
                            targetIndex = i;
                            break;
                        }
                    }
                }

                if (targetIndex != -1) {
                    int insertAt = (this.position == Position.BEFORE) ? targetIndex : targetIndex + 1;
                    finalLore.addAll(originalLore.subList(0, insertAt));
                    finalLore.addAll(loreToInsert);
                    finalLore.addAll(originalLore.subList(insertAt, originalLore.size()));
                } else {
                    if (this.fallback != null) {
                        return this.fallback.apply(item, context);
                    }
                    return item;
                }
            }
        }

        return item.loreComponent(finalLore);
    }

    public enum Position {
        BEFORE, AFTER, TAIL, HEAD
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

    private static class Factory implements ItemProcessorFactory<InsertLoreProcessor> {

        @Override
        public InsertLoreProcessor create(ConfigValue value) {
            if (value.is(Map.class)) {
                ConfigSection section = value.getAsSection();
                List<LoreModificationHolder> modifications = LoreProcessor.getLoreModificationHolders(section.getNonNullValue("lore", ConfigConstants.ARGUMENT_LIST));
                modifications.sort(LoreModificationHolder::compareTo);
                Position position = section.getEnum("position", Position.class, Position.TAIL);
                Pattern pattern = null;
                if (position == Position.BEFORE || position == Position.AFTER) {
                    pattern = Pattern.compile(section.getNonNullString("pattern"));
                }
                return new InsertLoreProcessor(
                        position,
                        pattern,
                        modifications.stream().map(LoreModificationHolder::modification).toArray(LoreModification[]::new),
                        section.getValue("fallback", this::create)
                );
            } else {
                List<LoreModificationHolder> modifications = LoreProcessor.getLoreModificationHolders(value);
                return new InsertLoreProcessor(
                        Position.TAIL,
                        null,
                        modifications.stream().map(LoreModificationHolder::modification).toArray(LoreModification[]::new),
                        null
                );
            }
        }
    }
}