package net.momirealms.craftengine.core.item.recipe.network.legacy;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.recipe.CookingRecipeCategory;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Obsolete
public class LegacyCookingRecipe implements LegacyRecipe {
    private Item<Object> result;
    private final CookingRecipeCategory category;
    private final String group;
    private final LegacyIngredient ingredient;
    private final float experience;
    private final int cookingTime;

    public LegacyCookingRecipe(LegacyIngredient ingredient,
                               CookingRecipeCategory category,
                               float experience,
                               int cookingTime,
                               Item<Object> result,
                               String group) {
        this.ingredient = ingredient;
        this.category = category;
        this.experience = experience;
        this.cookingTime = cookingTime;
        this.result = result;
        this.group = group;
    }

    @Override
    public void applyClientboundData(Player player) {
        this.result = CraftEngine.instance().itemManager().s2c(this.result, player);
        this.ingredient.applyClientboundData(player);
    }

    public static LegacyCookingRecipe read(FriendlyByteBuf buf) {
        String group = buf.readUtf();
        CookingRecipeCategory category = CookingRecipeCategory.byId(buf.readVarInt());
        LegacyIngredient ingredient = LegacyIngredient.read(buf);
        Item<Object> result = CraftEngine.instance().itemManager().decode(buf);
        float experience = buf.readFloat();
        int cookingTime = buf.readVarInt();
        return new LegacyCookingRecipe(ingredient, category, experience, cookingTime, result, group);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.group);
        buf.writeVarInt(this.category.ordinal());
        this.ingredient.write(buf);
        CraftEngine.instance().itemManager().encode(buf, this.result);
        buf.writeFloat(this.experience);
        buf.writeVarInt(this.cookingTime);
    }
}
