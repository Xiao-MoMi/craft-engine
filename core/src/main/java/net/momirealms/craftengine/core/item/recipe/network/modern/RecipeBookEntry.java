package net.momirealms.craftengine.core.item.recipe.network.modern;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public record RecipeBookEntry(RecipeBookDisplayEntry entry, byte flags) {

    public void applyClientboundData(Player player) {
        this.entry.applyClientboundData(player);
    }

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

