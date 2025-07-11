package net.momirealms.craftengine.core.item.recipe.network.legacy;

import net.momirealms.craftengine.core.item.recipe.CraftingRecipeCategory;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Obsolete
public class LegacyCustomRecipe implements LegacyRecipe {
    private final CraftingRecipeCategory category;

    public LegacyCustomRecipe(CraftingRecipeCategory category) {
        this.category = category;
    }

    public static LegacyCustomRecipe read(FriendlyByteBuf buf) {
        CraftingRecipeCategory category = CraftingRecipeCategory.byId(buf.readVarInt());
        return new LegacyCustomRecipe(category);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.category.ordinal());
    }
}
