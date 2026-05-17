package net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface RecipeDisplay {

    void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer);

    void applyClientboundData(Function<Item, Item> function);

    static RecipeDisplay read(final FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        return buf.readById(BuiltInRegistries.RECIPE_DISPLAY_TYPE).read(buf, reader);
    }

    record Type<T extends RecipeDisplay>(Key id, BiFunction<FriendlyByteBuf, FriendlyByteBuf.Reader<Item>, T> reader) {

        public T read(final FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
            return this.reader.apply(buf, reader);
        }
    }
}
