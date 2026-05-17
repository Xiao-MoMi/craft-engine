package net.momirealms.craftengine.core.item.processor.lore;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.processor.ItemProcessorFactory;
import net.momirealms.craftengine.core.item.processor.SimpleNetworkItemProcessor;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.text.minimessage.FormattedLine;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public sealed interface LoreProcessor extends SimpleNetworkItemProcessor
        permits LoreProcessor.EmptyLoreProcessor, LoreProcessor.CompositeLoreProcessor, LoreProcessor.DoubleLoreProcessor, LoreProcessor.SingleLoreProcessor {
    ItemProcessorFactory<LoreProcessor> FACTORY = new LoreFactory();
    Object[] NBT_PATH = new Object[]{"display", "Lore"};

    @Override
    @Nullable
    default Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.LORE;
    }

    @Override
    @Nullable
    default Object[] nbtPath(Item item, ItemBuildContext context) {
        return new Object[]{"display", "Lore"};
    }

    @Override
    default String nbtPathString(Item item, ItemBuildContext context) {
        return "display.Lore";
    }

    List<LoreModification> lore();

    static LoreProcessor createLoreModifier(ConfigValue configValue) {
        List<Object> rawLoreData = configValue.getAsList();
        String[] rawLore = new String[rawLoreData.size()];
        label_all_string_check: {
            for (int i = 0; i < rawLore.length; i++) {
                Object o = rawLoreData.get(i);
                if (o instanceof Map<?,?>) {
                    break label_all_string_check;
                } else {
                    rawLore[i] = o.toString();
                }
            }
            return new SingleLoreProcessor(new LoreModification(LoreModification.Operation.APPEND, false,
                    Arrays.stream(rawLore)
                            .map(AdventureHelper::legacyToMiniMessage)
                            .map(line -> Config.addNonItalicTag() && !line.startsWith("<!i>") ? FormattedLine.create("<!i>" + line) : FormattedLine.create(line))
                            .toArray(FormattedLine[]::new), c -> true));
        }

        List<LoreModificationHolder> modifications = getLoreModificationHolders(configValue);
        modifications.sort(LoreModificationHolder::compareTo);
        return switch (modifications.size()) {
            case 0 -> new EmptyLoreProcessor();
            case 1 -> new SingleLoreProcessor(modifications.get(0).modification());
            case 2 -> new DoubleLoreProcessor(modifications.get(0).modification(), modifications.get(1).modification());
            default -> new CompositeLoreProcessor(modifications.stream().map(LoreModificationHolder::modification).toArray(LoreModification[]::new));
        };
    }

    String[] SPLIT_LINES = new String[] {"split_lines", "split-lines"};

    static @NotNull List<LoreModificationHolder> getLoreModificationHolders(ConfigValue configValue) {
        MutableInt lastPriority = new MutableInt(0);
        List<LoreModificationHolder> modifications = new ArrayList<>();
        configValue.forEach(v -> {
            if (v.is(Map.class)) {
                ConfigSection section = v.getAsSection();
                String[] contents = section.getStringList("content").toArray(String[]::new);
                LoreModification.Operation operation = section.getEnum("operation", LoreModification.Operation.class, LoreModification.Operation.APPEND);
                int priority = section.getInt("priority", lastPriority.intValue());
                boolean split = section.getBoolean(SPLIT_LINES);
                List<Condition<ItemBuildContext>> conditions = section.getList("conditions", a -> CommonConditions.fromConfig(a.getAsSection()));
                modifications.add(new LoreModificationHolder(new LoreModification(operation, split,
                        Arrays.stream(contents)
                                .map(AdventureHelper::legacyToMiniMessage)
                                .map(line -> Config.addNonItalicTag() && !line.startsWith("<!i>") ? FormattedLine.create("<!i>" + line) : FormattedLine.create(line))
                                .toArray(FormattedLine[]::new), MiscUtils.allOf(conditions)
                        ),
                        priority
                ));
                lastPriority.set(priority);
            } else {
                modifications.add(new LoreModificationHolder(
                        new LoreModification(
                                LoreModification.Operation.APPEND,
                                false,
                                new FormattedLine[]{FormattedLine.create(v.getAsString())},
                                (c) -> true
                        ),
                        lastPriority.intValue()
                ));
            }
        });
        return modifications;
    }

    non-sealed class EmptyLoreProcessor implements LoreProcessor {

        @Override
        public Item apply(Item item, ItemBuildContext context) {
            return item;
        }

        @Override
        public List<LoreModification> lore() {
            return List.of();
        }
    }

    non-sealed class SingleLoreProcessor implements LoreProcessor {
        private final LoreModification modification;

        public SingleLoreProcessor(LoreModification modification) {
            this.modification = modification;
        }

        @Override
        public Item apply(Item item, ItemBuildContext context) {
            item.loreComponent(this.modification.parseAsList(context));
            return item;
        }

        @Override
        public List<LoreModification> lore() {
            return List.of(modification);
        }
    }

    non-sealed class DoubleLoreProcessor implements LoreProcessor {
        private final LoreModification modification1;
        private final LoreModification modification2;

        public DoubleLoreProcessor(LoreModification m1, LoreModification m2) {
            this.modification1 = m1;
            this.modification2 = m2;
        }

        @Override
        public Item apply(Item item, ItemBuildContext context) {
            item.loreComponent(this.modification2.apply(this.modification1.apply(Stream.empty(), context), context).toList());
            return item;
        }

        @Override
        public List<LoreModification> lore() {
            return List.of(modification1, modification2);
        }
    }

    non-sealed class CompositeLoreProcessor implements LoreProcessor {
        private final LoreModification[] modifications;

        public CompositeLoreProcessor(LoreModification... modifications) {
            this.modifications = modifications;
        }

        @Override
        public Item apply(Item item, ItemBuildContext context) {
            item.loreComponent(Arrays.stream(this.modifications).reduce(Stream.<Component>empty(), (stream, modification) -> modification.apply(stream, context), Stream::concat).toList());
            return item;
        }

        @Override
        public List<LoreModification> lore() {
            return Arrays.asList(modifications);
        }
    }
}
