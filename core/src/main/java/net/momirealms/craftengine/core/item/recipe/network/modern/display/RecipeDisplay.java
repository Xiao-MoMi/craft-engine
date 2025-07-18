package net.momirealms.craftengine.core.item.recipe.network.modern.display;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.function.Function;

public interface RecipeDisplay {

    void write(FriendlyByteBuf buf);

    void applyClientboundData(Player player);

    static RecipeDisplay read(final FriendlyByteBuf buf) {
        return buf.readById(BuiltInRegistries.RECIPE_DISPLAY_TYPE).read(buf);
    }

    record Type(Function<FriendlyByteBuf, RecipeDisplay> reader) {

        public RecipeDisplay read(final FriendlyByteBuf buf) {
            return this.reader.apply(buf);
        }
    }
}
