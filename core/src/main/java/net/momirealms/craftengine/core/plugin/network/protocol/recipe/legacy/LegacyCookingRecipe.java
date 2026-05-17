package net.momirealms.craftengine.core.plugin.network.protocol.recipe.legacy;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.recipe.CookingRecipeCategory;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

@ApiStatus.Obsolete
public final class LegacyCookingRecipe implements LegacyRecipe {
    private Item result;
    private final CookingRecipeCategory category;
    private final String group;
    private final LegacyIngredient ingredient;
    private final float experience;
    private final int cookingTime;

    public LegacyCookingRecipe(LegacyIngredient ingredient,
                               CookingRecipeCategory category,
                               float experience,
                               int cookingTime,
                               Item result,
                               String group) {
        this.ingredient = ingredient;
        this.category = category;
        this.experience = experience;
        this.cookingTime = cookingTime;
        this.result = result;
        this.group = group;
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        this.result = function.apply(this.result);
        this.ingredient.applyClientboundData(function);
    }

    public static LegacyCookingRecipe read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        String group = buf.readUtf();
        CookingRecipeCategory category = CookingRecipeCategory.byId(buf.readVarInt());
        LegacyIngredient ingredient = LegacyIngredient.read(buf, reader);
        Item result = reader.apply(buf);
        float experience = buf.readFloat();
        int cookingTime = buf.readVarInt();
        return new LegacyCookingRecipe(ingredient, category, experience, cookingTime, result, group);
    }

    @Override
    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeUtf(this.group);
        buf.writeVarInt(this.category.ordinal());
        this.ingredient.write(buf, writer);
        writer.accept(buf, this.result);
        buf.writeFloat(this.experience);
        buf.writeVarInt(this.cookingTime);
    }
}
