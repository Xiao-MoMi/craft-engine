package net.momirealms.craftengine.core.item.recipe.network.modern.display.slot;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.function.Function;

public interface SlotDisplay {

    void write(FriendlyByteBuf buf);

    default void applyClientboundData(Player player) {
    }

    static SlotDisplay read(FriendlyByteBuf buf) {
        return buf.readById(BuiltInRegistries.SLOT_DISPLAY_TYPE).read(buf);
    }

    record Type(Function<FriendlyByteBuf, SlotDisplay> reader) {

        public SlotDisplay read(final FriendlyByteBuf buf) {
            return this.reader.apply(buf);
        }
    }
}
