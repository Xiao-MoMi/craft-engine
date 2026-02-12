package net.momirealms.craftengine.core.item.processor.lore;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.DataComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemProcessorFactory;
import net.momirealms.craftengine.core.item.processor.SimpleNetworkItemProcessor;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.text.minimessage.FormattedLine;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

public sealed interface LoreProcessor extends SimpleNetworkItemProcessor
        permits LoreProcessor.EmptyLoreProcessor, LoreProcessor.CompositeLoreProcessor, LoreProcessor.DoubleLoreProcessor, LoreProcessor.SingleLoreProcessor {
    ItemProcessorFactory<LoreProcessor> FACTORY = new LoreFactory();

    @Override
    @Nullable
    default <I> Key componentType(Item<I> item, ItemBuildContext context) {
        return DataComponentKeys.LORE;
    }

    @Override
    @Nullable
    default <I> Object[] nbtPath(Item<I> item, ItemBuildContext context) {
        return new Object[]{"display", "Lore"};
    }

    @Override
    default <I> String nbtPathString(Item<I> item, ItemBuildContext context) {
        return "display.Lore";
    }

    List<LoreModification> lore();

    static LoreProcessor createLoreModifier(Object arg) {
        List<Object> rawLoreData = MiscUtils.getAsList(arg, Object.class);
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
                    Arrays.stream(rawLore).map(AdventureHelper::legacyToMiniMessage).map(line -> Config.addNonItalicTag() && !line.startsWith("<!i>") ? FormattedLine.create("<!i>" + line) : FormattedLine.create(line))
                            .toArray(FormattedLine[]::new), c -> true));
        }

        List<LoreModificationHolder> modifications = new ArrayList<>(rawLoreData.size() + 1);
        int lastPriority = 0;
        for (Object o : rawLoreData) {
            if (o instanceof Map<?,?> complexLore) {
                String[] content = MiscUtils.getAsStringArray(complexLore.get("content"));
                LoreModification.Operation operation = ResourceConfigUtils.getAsEnum(Optional.ofNullable(complexLore.get("operation")).map(String::valueOf).orElse(null), LoreModification.Operation.class, LoreModification.Operation.APPEND);
                lastPriority = Optional.ofNullable(complexLore.get("priority")).map(it -> ResourceConfigUtils.getAsInt(it, "priority")).orElse(lastPriority);
                boolean split = ResourceConfigUtils.getAsBoolean(complexLore.get("split-lines"), "split-lines");
                List<Condition<ItemBuildContext>> conditions = ResourceConfigUtils.parseConfigAsList(complexLore.get("conditions"), CommonConditions::fromMap);
                modifications.add(new LoreModificationHolder(new LoreModification(operation, split,
                        Arrays.stream(content).map(AdventureHelper::legacyToMiniMessage).map(line -> Config.addNonItalicTag() && !line.startsWith("<!i>") ? FormattedLine.create("<!i>" + line) : FormattedLine.create(line))
                        .toArray(FormattedLine[]::new), MiscUtils.allOf(conditions)), lastPriority));
            }
        }
        modifications.sort(LoreModificationHolder::compareTo);
        return switch (modifications.size()) {
            case 0 -> new EmptyLoreProcessor();
            case 1 -> new SingleLoreProcessor(modifications.get(0).modification());
            case 2 -> new DoubleLoreProcessor(modifications.get(0).modification(), modifications.get(1).modification());
            default -> new CompositeLoreProcessor(modifications.stream().map(LoreModificationHolder::modification).toArray(LoreModification[]::new));
        };
    }

    non-sealed class EmptyLoreProcessor implements LoreProcessor {

        @Override
        public <I> Item<I> apply(Item<I> item, ItemBuildContext context) {
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
        public <I> Item<I> apply(Item<I> item, ItemBuildContext context) {
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
        public <I> Item<I> apply(Item<I> item, ItemBuildContext context) {
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
        public <I> Item<I> apply(Item<I> item, ItemBuildContext context) {
            item.loreComponent(Arrays.stream(this.modifications).reduce(Stream.<Component>empty(), (stream, modification) -> modification.apply(stream, context), Stream::concat).toList());
            return item;
        }

        @Override
        public List<LoreModification> lore() {
            return Arrays.asList(modifications);
        }
    }
}
