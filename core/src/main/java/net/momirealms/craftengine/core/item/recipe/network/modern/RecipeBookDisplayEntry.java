package net.momirealms.craftengine.core.item.recipe.network.modern;

import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.recipe.network.modern.display.RecipeDisplay;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public record RecipeBookDisplayEntry(RecipeDisplayId displayId, RecipeDisplay display, OptionalInt group, int category, Optional<List<Ingredient>> ingredients) {

    public static RecipeBookDisplayEntry read(FriendlyByteBuf buffer) {
        RecipeDisplayId displayId = RecipeDisplayId.read(buffer);
        RecipeDisplay display = RecipeDisplay.read(buffer);
        OptionalInt group = buffer.readOptionalVarInt();
        int category = buffer.readVarInt(); // simplify the registry lookup since we don't care about the category
        Optional<List<Ingredient>> requirements = buffer.readOptional(buf -> buf.readCollection(ArrayList::new, byteBuf -> new Ingredient(byteBuf.readHolderSet()))); // simplify the registry lookup since we don't care about the ingredient ids
        return new RecipeBookDisplayEntry(displayId, display, group, category, requirements);
    }

    public void applyClientboundData(Player player) {
        this.display.applyClientboundData(player);
    }

    public void write(FriendlyByteBuf buffer) {
        this.displayId.write(buffer);
        this.display.write(buffer);
        buffer.writeOptionalVarInt(this.group);
        buffer.writeVarInt(this.category);
        buffer.writeOptional(this.ingredients, (buf, recipeIngredients) -> buf.writeCollection(recipeIngredients, (byteBuf, ingredient) -> byteBuf.writeHolderSet(ingredient.holderSet)));
    }

    @Override
    public @NotNull String toString() {
        return "RecipeBookDisplayEntry{" +
                "category=" + category +
                ", displayId=" + displayId +
                ", display=" + display +
                ", group=" + group +
                ", ingredients=" + ingredients +
                '}';
    }

    public record Ingredient(Either<List<Integer>, Key> holderSet) {
    }
}
