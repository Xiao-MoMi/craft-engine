package net.momirealms.craftengine.core.item.modifier.lore;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.TriFunction;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// todo 可以考虑未来添加条件系统
public record LoreModification(Operation operation, boolean split, String[] content) {

    public Stream<Component> apply(Stream<Component> lore, ItemBuildContext context) {
        return this.operation.function.apply(lore, context, this);
    }

    public Stream<Component> parseAsStream(ItemBuildContext context) {
        Stream<Component> parsed = Arrays.stream(this.content).map(string -> AdventureHelper.miniMessage().deserialize(string, context.tagResolvers()));
        return this.split ? parsed.map(AdventureHelper::splitLines).flatMap(List::stream) : parsed;
    }

    public List<Component> parseAsList(ItemBuildContext context) {
        return this.parseAsStream(context).collect(Collectors.toList());
    }

    public enum Operation {
        APPEND((s, c, modification) -> Stream.concat(s, modification.parseAsStream(c))),
        PREPEND((s, c, modification) -> Stream.concat(modification.parseAsStream(c), s));

        private final TriFunction<Stream<Component>, ItemBuildContext, LoreModification, Stream<Component>> function;

        Operation(TriFunction<Stream<Component>, ItemBuildContext, LoreModification, Stream<Component>> function) {
            this.function = function;
        }
    }
}
