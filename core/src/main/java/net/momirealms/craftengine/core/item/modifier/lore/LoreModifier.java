package net.momirealms.craftengine.core.item.modifier.lore;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDataModifierFactory;
import net.momirealms.craftengine.core.item.modifier.ItemDataModifier;
import net.momirealms.craftengine.core.item.modifier.ItemDataModifiers;
import net.momirealms.craftengine.core.item.modifier.SimpleNetworkItemDataModifier;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.text.minimessage.FormattedLine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

public sealed interface LoreModifier<I> extends SimpleNetworkItemDataModifier<I>
        permits LoreModifier.EmptyLoreModifier, LoreModifier.CompositeLoreModifier, LoreModifier.DoubleLoreModifier, LoreModifier.SingleLoreModifier {
    Factory<?> FACTORY = new Factory<>();

    @Override
    default Key type() {
        return ItemDataModifiers.LORE;
    }

    @Override
    @Nullable
    default Key componentType(Item<I> item, ItemBuildContext context) {
        return ComponentKeys.LORE;
    }

    @Override
    @Nullable
    default Object[] nbtPath(Item<I> item, ItemBuildContext context) {
        return new Object[]{"display", "Lore"};
    }

    @Override
    default String nbtPathString(Item<I> item, ItemBuildContext context) {
        return "display.Lore";
    }

    List<LoreModification> lore();

    class Factory<I> implements ItemDataModifierFactory<I> {
        @Override
        public ItemDataModifier<I> create(Object arg) {
            return createLoreModifier(arg);
        }
    }

    static <I> LoreModifier<I> createLoreModifier(Object arg) {
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
            return new SingleLoreModifier<>(new LoreModification(LoreModification.Operation.APPEND, false,
                    Arrays.stream(rawLore).map(line -> Config.addNonItalicTag() ? FormattedLine.create("<!i>" + line) : FormattedLine.create(line))
                            .toArray(FormattedLine[]::new)));
        }

        List<LoreModificationHolder> modifications = new ArrayList<>(rawLoreData.size() + 1);
        int lastPriority = 0;
        for (Object o : rawLoreData) {
            if (o instanceof Map<?,?> complexLore) {
                String[] content = MiscUtils.getAsStringArray(complexLore.get("content"));
                LoreModification.Operation operation = ResourceConfigUtils.getAsEnum(Optional.ofNullable(complexLore.get("operation")).map(String::valueOf).orElse(null), LoreModification.Operation.class, LoreModification.Operation.APPEND);
                lastPriority = Optional.ofNullable(complexLore.get("priority")).map(it -> ResourceConfigUtils.getAsInt(it, "priority")).orElse(lastPriority);
                boolean split = ResourceConfigUtils.getAsBoolean(complexLore.get("split-lines"), "split-lines");
                modifications.add(new LoreModificationHolder(new LoreModification(operation, split,
                        Arrays.stream(content).map(line -> Config.addNonItalicTag() ? FormattedLine.create("<!i>" + line) : FormattedLine.create(line))
                        .toArray(FormattedLine[]::new)), lastPriority));
            }
        }
        modifications.sort(LoreModificationHolder::compareTo);
        return switch (modifications.size()) {
            case 0 -> new EmptyLoreModifier<>();
            case 1 -> new SingleLoreModifier<>(modifications.get(0).modification());
            case 2 -> new DoubleLoreModifier<>(modifications.get(0).modification(), modifications.get(1).modification());
            default -> new CompositeLoreModifier<>(modifications.stream().map(LoreModificationHolder::modification).toArray(LoreModification[]::new));
        };
    }

    non-sealed class EmptyLoreModifier<I> implements LoreModifier<I> {

        @Override
        public Item<I> apply(Item<I> item, ItemBuildContext context) {
            return item;
        }

        @Override
        public List<LoreModification> lore() {
            return List.of();
        }
    }

    non-sealed class SingleLoreModifier<I> implements LoreModifier<I> {
        private final LoreModification modification;

        public SingleLoreModifier(LoreModification modification) {
            this.modification = modification;
        }

        @Override
        public Item<I> apply(Item<I> item, ItemBuildContext context) {
            item.loreComponent(this.modification.parseAsList(context));
            return item;
        }

        @Override
        public List<LoreModification> lore() {
            return List.of(modification);
        }
    }

    non-sealed class DoubleLoreModifier<I> implements LoreModifier<I> {
        private final LoreModification modification1;
        private final LoreModification modification2;

        public DoubleLoreModifier(LoreModification m1, LoreModification m2) {
            this.modification1 = m1;
            this.modification2 = m2;
        }

        @Override
        public Item<I> apply(Item<I> item, ItemBuildContext context) {
            item.loreComponent(this.modification2.apply(this.modification1.apply(Stream.empty(), context), context).toList());
            return item;
        }

        @Override
        public List<LoreModification> lore() {
            return List.of(modification1, modification2);
        }
    }

    non-sealed class CompositeLoreModifier<I> implements LoreModifier<I> {
        private final LoreModification[] modifications;

        public CompositeLoreModifier(LoreModification... modifications) {
            this.modifications = modifications;
        }

        @Override
        public Item<I> apply(Item<I> item, ItemBuildContext context) {
            item.loreComponent(Arrays.stream(this.modifications).reduce(Stream.<Component>empty(), (stream, modification) -> modification.apply(stream, context), Stream::concat).toList());
            return item;
        }

        @Override
        public List<LoreModification> lore() {
            return Arrays.asList(modifications);
        }
    }
}
