package net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern;

import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display.slot.SlotDisplay;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.function.Function;

public record SingleInputButtonDisplay(Either<List<Integer>, Key> ingredients, SlotDisplay display) {

    public static SingleInputButtonDisplay read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        Either<List<Integer>, Key> ingredients = buf.readHolderSet();
        SlotDisplay slotDisplay = SlotDisplay.read(buf, reader);
        return new SingleInputButtonDisplay(ingredients, slotDisplay);
    }

    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeHolderSet(this.ingredients);
        this.display.write(buf, writer);
    }

    public void applyClientboundData(Function<Item, Item> function) {
        this.display.applyClientboundData(function);
    }
}
