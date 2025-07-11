package net.momirealms.craftengine.core.item.recipe.network.legacy;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.recipe.CraftingRecipeCategory;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Obsolete
public class LegacyShapelessRecipe implements LegacyRecipe {
    private final List<LegacyIngredient> ingredients;
    private Item<Object> result;
    private final String group;
    private final CraftingRecipeCategory category;

    public LegacyShapelessRecipe(List<LegacyIngredient> ingredients,
                                 Item<Object> result,
                                 String group,
                                 CraftingRecipeCategory category) {
        this.category = category;
        this.ingredients = ingredients;
        this.result = result;
        this.group = group;
    }

    @Override
    public void applyClientboundData(Player player) {
        this.result = CraftEngine.instance().itemManager().s2c(this.result, player);
        for (LegacyIngredient ingredient : this.ingredients) {
            ingredient.applyClientboundData(player);
        }
    }

    public static LegacyShapelessRecipe read(FriendlyByteBuf buf) {
        String group = buf.readUtf();
        CraftingRecipeCategory category = CraftingRecipeCategory.byId(buf.readVarInt());
        List<LegacyIngredient> ingredient = buf.readCollection(ArrayList::new, LegacyIngredient::read);
        Item<Object> result = CraftEngine.instance().itemManager().decode(buf);
        return new LegacyShapelessRecipe(ingredient, result, group, category);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.group);
        buf.writeVarInt(this.category.ordinal());
        buf.writeCollection(this.ingredients, (byteBuf, legacyIngredient) -> legacyIngredient.write(buf));
        CraftEngine.instance().itemManager().encode(buf, this.result);
    }
}
