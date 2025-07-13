package net.momirealms.craftengine.core.item.recipe.network.legacy;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Obsolete
public class LegacyStoneCuttingRecipe implements LegacyRecipe {
    private Item<Object> result;
    private final String group;
    private final LegacyIngredient ingredient;

    public LegacyStoneCuttingRecipe(LegacyIngredient ingredient,
                                    Item<Object> result,
                                    String group) {
        this.ingredient = ingredient;
        this.result = result;
        this.group = group;
    }

    @Override
    public void applyClientboundData(Player player) {
        this.result = CraftEngine.instance().itemManager().s2c(this.result, player);
        this.ingredient.applyClientboundData(player);
    }

    public static LegacyStoneCuttingRecipe read(FriendlyByteBuf buf) {
        String group = buf.readUtf();
        LegacyIngredient ingredient = LegacyIngredient.read(buf);
        Item<Object> result = CraftEngine.instance().itemManager().decode(buf);
        return new LegacyStoneCuttingRecipe(ingredient, result, group);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.group);
        this.ingredient.write(buf);
        CraftEngine.instance().itemManager().encode(buf, this.result);
    }
}
