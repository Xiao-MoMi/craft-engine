package net.momirealms.craftengine.core.item.recipe.network;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public record RecipeDisplayId(int id) {

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.id);
    }

    public static RecipeDisplayId read(FriendlyByteBuf buffer) {
        return new RecipeDisplayId(buffer.readVarInt());
    }
}
