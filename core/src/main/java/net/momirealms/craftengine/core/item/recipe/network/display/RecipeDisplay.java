package net.momirealms.craftengine.core.item.recipe.network.display;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.function.Function;

public interface RecipeDisplay {

    void write(FriendlyByteBuf buf);

    record Type(Function<FriendlyByteBuf, RecipeDisplay> reader) {

        public RecipeDisplay read(final FriendlyByteBuf buf) {
            return this.reader.apply(buf);
        }
    }
}
