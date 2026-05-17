package net.momirealms.craftengine.core.plugin.network.protocol.recipe.legacy;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.BiFunction;
import java.util.function.Function;

@ApiStatus.Obsolete
public interface LegacyRecipe {

    default void applyClientboundData(Function<Item, Item> function) {}

    void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer);

    record Type<T extends LegacyRecipe>(Key id, BiFunction<FriendlyByteBuf, FriendlyByteBuf.Reader<Item>, T> reader) {

        public T read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
            return this.reader.apply(buf, reader);
        }
    }
}
