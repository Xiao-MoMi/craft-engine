package net.momirealms.craftengine.core.plugin.network.protocol.recipe.legacy;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.recipe.CraftingRecipeCategory;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@ApiStatus.Obsolete
public final class LegacyShapedRecipe implements LegacyRecipe {
    private final int width;
    private final int height;
    private final List<LegacyIngredient> ingredients;
    private Item result;
    private final String group;
    private final CraftingRecipeCategory category;
    private final boolean showNotification;

    public LegacyShapedRecipe(int width, int height,
                              List<LegacyIngredient> ingredients,
                              Item result,
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

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        this.result = function.apply(this.result);
        for (LegacyIngredient ingredient : this.ingredients) {
            ingredient.applyClientboundData(function);
        }
    }

    public static LegacyShapedRecipe read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        if (VersionHelper.isOrAbove1_20_3) {
            String group = buf.readUtf();
            int category = buf.readVarInt();
            int width = buf.readVarInt();
            int height = buf.readVarInt();
            int size = width * height;
            List<LegacyIngredient> ingredients = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                ingredients.add(LegacyIngredient.read(buf, reader));
            }
            Item result = reader.apply(buf);
            boolean flag = buf.readBoolean();
            return new LegacyShapedRecipe(width, height, ingredients, result, group, CraftingRecipeCategory.byId(category), flag);
        } else {
            int width = buf.readVarInt();
            int height = buf.readVarInt();
            String group = buf.readUtf();
            int category = buf.readVarInt();
            int size = width * height;
            List<LegacyIngredient> ingredients = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                ingredients.add(LegacyIngredient.read(buf, reader));
            }
            Item result = reader.apply(buf);
            boolean flag = buf.readBoolean();
            return new LegacyShapedRecipe(width, height, ingredients, result, group, CraftingRecipeCategory.byId(category), flag);
        }
    }

    @Override
    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        if (VersionHelper.isOrAbove1_20_3) {
            buf.writeUtf(this.group);
            buf.writeVarInt(this.category.ordinal());
            buf.writeVarInt(this.width);
            buf.writeVarInt(this.height);
        } else {
            buf.writeVarInt(this.width);
            buf.writeVarInt(this.height);
            buf.writeUtf(this.group);
            buf.writeVarInt(this.category.ordinal());
        }
        for (LegacyIngredient ingredient : this.ingredients) {
            ingredient.write(buf, writer);
        }
        writer.accept(buf, this.result);
        buf.writeBoolean(this.showNotification);
    }
}
