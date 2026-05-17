package net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.function.Function;

public record RecipeBookEntry(RecipeBookDisplayEntry entry, byte flags) {

    public void applyClientboundData(Function<Item, Item> function) {
        this.entry.applyClientboundData(function);
    }

    public static RecipeBookEntry read(FriendlyByteBuf buffer, FriendlyByteBuf.Reader<Item> reader) {
        RecipeBookDisplayEntry displayEntry = RecipeBookDisplayEntry.read(buffer, reader);
        byte flags = buffer.readByte();
        return new RecipeBookEntry(displayEntry, flags);
    }

    public void write(FriendlyByteBuf buffer, FriendlyByteBuf.Writer<Item> writer) {
        this.entry.write(buffer, writer);
        buffer.writeByte(this.flags);
    }
}

