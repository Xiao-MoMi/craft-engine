package net.momirealms.craftengine.core.item.recipe.network.legacy;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.recipe.CraftingRecipeCategory;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

@ApiStatus.Obsolete
public class LegacyShapedRecipe implements LegacyRecipe {
    private final int width;
    private final int height;
    private final List<LegacyIngredient> ingredients;
    private Item<Object> result;
    private final String group;
    private final CraftingRecipeCategory category;
    private final boolean showNotification;

    public LegacyShapedRecipe(int width, int height,
                              List<LegacyIngredient> ingredients,
                              Item<Object> result,
                              String group,
                              CraftingRecipeCategory category,
                              boolean showNotification) {
        this.category = category;
        this.width = width;
        this.height = height;
        this.ingredients = ingredients;
        this.result = result;
        this.group = group;
        this.showNotification = showNotification;
    }

    private static final Function<FriendlyByteBuf, LegacyShapedRecipe> READER = VersionHelper.isOrAbove1_20_3() ?
            (buf) -> {
                String group = buf.readUtf();
                int category = buf.readVarInt();
                int width = buf.readVarInt();
                int height = buf.readVarInt();
                int size = width * height;
                List<LegacyIngredient> ingredients = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    ingredients.add(LegacyIngredient.read(buf));
                }
                Item<Object> result = CraftEngine.instance().itemManager().decode(buf);
                boolean flag = buf.readBoolean();
                return new LegacyShapedRecipe(width, height, ingredients, result, group, CraftingRecipeCategory.byId(category), flag);
            } :
            (buf) -> {
                int width = buf.readVarInt();
                int height = buf.readVarInt();
                String group = buf.readUtf();
                int category = buf.readVarInt();
                int size = width * height;
                List<LegacyIngredient> ingredients = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    ingredients.add(LegacyIngredient.read(buf));
                }
                Item<Object> result = CraftEngine.instance().itemManager().decode(buf);
                boolean flag = buf.readBoolean();
                return new LegacyShapedRecipe(width, height, ingredients, result, group, CraftingRecipeCategory.byId(category), flag);
            };

    private static final BiConsumer<LegacyShapedRecipe, FriendlyByteBuf> WRITER = VersionHelper.isOrAbove1_20_3() ?
            (recipe, buf) -> {
                buf.writeUtf(recipe.group);
                buf.writeVarInt(recipe.category.ordinal());
                buf.writeVarInt(recipe.width);
                buf.writeVarInt(recipe.height);
                for (LegacyIngredient ingredient : recipe.ingredients) {
                    ingredient.write(buf);
                }
                CraftEngine.instance().itemManager().encode(buf, recipe.result);
                buf.writeBoolean(recipe.showNotification);
            } :
            (recipe, buf) -> {
                buf.writeVarInt(recipe.width);
                buf.writeVarInt(recipe.height);
                buf.writeUtf(recipe.group);
                buf.writeVarInt(recipe.category.ordinal());
                for (LegacyIngredient ingredient : recipe.ingredients) {
                    ingredient.write(buf);
                }
                CraftEngine.instance().itemManager().encode(buf, recipe.result);
                buf.writeBoolean(recipe.showNotification);
            };

    @Override
    public void applyClientboundData(Player player) {
        this.result = CraftEngine.instance().itemManager().s2c(this.result, player);
        for (LegacyIngredient ingredient : this.ingredients) {
            ingredient.applyClientboundData(player);
        }
    }

    public static LegacyShapedRecipe read(FriendlyByteBuf buf) {
        return READER.apply(buf);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        WRITER.accept(this, buf);
    }
}
