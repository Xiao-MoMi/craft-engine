package net.momirealms.craftengine.core.plugin.network.protocol.recipe.legacy;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.recipe.CraftingRecipeCategory;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Obsolete
public final class LegacyCustomRecipe implements LegacyRecipe {
    private final CraftingRecipeCategory category;

    public LegacyCustomRecipe(CraftingRecipeCategory category) {
        this.category = category;
    }

    public static LegacyCustomRecipe read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        CraftingRecipeCategory category = CraftingRecipeCategory.byId(buf.readVarInt());
        return new LegacyCustomRecipe(category);
    }

    @Override
    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeVarInt(this.category.ordinal());
    }
}
