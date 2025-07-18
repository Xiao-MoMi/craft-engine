package net.momirealms.craftengine.core.item.recipe.network.legacy;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

@ApiStatus.Obsolete
public interface LegacyRecipe {

    default void applyClientboundData(Player player) {}

    void write(FriendlyByteBuf buf);

    record Type(Function<FriendlyByteBuf, LegacyRecipe> reader) {

        public LegacyRecipe read(FriendlyByteBuf buf) {
            return this.reader.apply(buf);
        }
    }
}
