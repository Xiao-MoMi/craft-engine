package net.momirealms.craftengine.core.item.recipe.network.modern;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public record RecipeDisplayId(int id) {

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.id);
    }

    public static RecipeDisplayId read(FriendlyByteBuf buffer) {
        return new RecipeDisplayId(buffer.readVarInt());
    }

    @Override
    public @NotNull String toString() {
        return "RecipeDisplayId{" +
                "id=" + id +
                '}';
    }
}
