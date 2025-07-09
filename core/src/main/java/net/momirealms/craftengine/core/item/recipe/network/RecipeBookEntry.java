package net.momirealms.craftengine.core.item.recipe.network;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public record RecipeBookEntry(RecipeBookDisplayEntry entry, byte flags) {

    public static RecipeBookEntry read(FriendlyByteBuf buffer) {
        RecipeBookDisplayEntry displayEntry = RecipeBookDisplayEntry.read(buffer);
        byte flags = buffer.readByte();
        return new RecipeBookEntry(displayEntry, flags);
    }

    public void write(FriendlyByteBuf buffer) {
        this.entry.write(buffer);
        buffer.writeByte(this.flags);
    }
}

