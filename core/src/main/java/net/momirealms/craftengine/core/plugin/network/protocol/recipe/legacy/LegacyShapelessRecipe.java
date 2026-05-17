package net.momirealms.craftengine.core.plugin.network.protocol.recipe.legacy;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.recipe.CraftingRecipeCategory;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@ApiStatus.Obsolete
public final class LegacyShapelessRecipe implements LegacyRecipe {
    private final List<LegacyIngredient> ingredients;
    private Item result;
    private final String group;
    private final CraftingRecipeCategory category;

    public LegacyShapelessRecipe(List<LegacyIngredient> ingredients,
                                 Item result,
                                 String group,
                                 CraftingRecipeCategory category) {
        this.category = category;
        this.ingredients = ingredients;
        this.result = result;
        this.group = group;
    }

    public static LegacyShapelessRecipe read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        String group = buf.readUtf();
        CraftingRecipeCategory category = CraftingRecipeCategory.byId(buf.readVarInt());
        List<LegacyIngredient> ingredient = buf.readCollection(ArrayList::new, (byteBuffer) -> LegacyIngredient.read(byteBuffer, reader));
        Item result = reader.apply(buf);
        return new LegacyShapelessRecipe(ingredient, result, group, category);
    }

    @Override
    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeUtf(this.group);
        buf.writeVarInt(this.category.ordinal());
        buf.writeCollection(this.ingredients, (byteBuf, legacyIngredient) -> legacyIngredient.write(buf, writer));
        writer.accept(buf, this.result);
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        this.result = function.apply(this.result);
        for (LegacyIngredient ingredient : this.ingredients) {
            ingredient.applyClientboundData(function);
        }
    }
}
