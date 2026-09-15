package net.momirealms.craftengine.core.item.processor.lore;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.plugin.text.minimessage.FormattedLine;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.TriFunction;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public record LoreModification(Operation operation, boolean split, FormattedLine[] content, Predicate<ItemBuildContext> predicate) {
    public static final Predicate<ItemBuildContext> ALWAYS_ADD = MiscUtils.allOf();

    public boolean isConstant() {
        if (this.predicate != ALWAYS_ADD)
            return false;
        for (FormattedLine line : this.content) {
            if (!line.isConstant())
                return false;
        }
        return true;
    }

    public Stream<Component> apply(Stream<Component> lore, ItemBuildContext context) {
        return this.operation.function.apply(lore, context, this);
    }

    public Stream<Component> parseAsStream(ItemBuildContext context) {
        Stream<Component> parsed = Arrays.stream(this.content).map(line -> line.parse(context));
        return this.split ? parsed.map(AdventureHelper::splitLines).flatMap(List::stream) : parsed;
    }

    public List<Component> parseAsList(ItemBuildContext context) {
        return this.parseAsStream(context).toList();
    }

    public enum Operation {
        APPEND((s, c, modification) -> {
            if (modification.predicate.test(c)) {
                return Stream.concat(s, modification.parseAsStream(c));
            }
            return s;
        }),
        PREPEND((s, c, modification) -> {
            if (modification.predicate.test(c)) {
                return Stream.concat(modification.parseAsStream(c), s);
            }
            return s;
        });

        private final TriFunction<Stream<Component>, ItemBuildContext, LoreModification, Stream<Component>> function;

        Operation(TriFunction<Stream<Component>, ItemBuildContext, LoreModification, Stream<Component>> function) {
            this.function = function;
        }
    }
}
