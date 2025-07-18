package net.momirealms.craftengine.core.item.recipe.network.legacy;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Obsolete
public class LegacySmithingTransformRecipe implements LegacyRecipe {
    private final LegacyIngredient template;
    private final LegacyIngredient base;
    private final LegacyIngredient addition;
    private Item<Object> result;

    public LegacySmithingTransformRecipe(LegacyIngredient addition, LegacyIngredient template, LegacyIngredient base, Item<Object> result) {
        this.addition = addition;
        this.template = template;
        this.base = base;
        this.result = result;
    }

    @Override
    public void applyClientboundData(Player player) {
        this.result = CraftEngine.instance().itemManager().s2c(this.result, player);
        this.template.applyClientboundData(player);
        this.base.applyClientboundData(player);
        this.addition.applyClientboundData(player);
    }

    public static LegacySmithingTransformRecipe read(FriendlyByteBuf buf) {
        LegacyIngredient template = LegacyIngredient.read(buf);
        LegacyIngredient base = LegacyIngredient.read(buf);
        LegacyIngredient addition = LegacyIngredient.read(buf);
        Item<Object> result = CraftEngine.instance().itemManager().decode(buf);
        return new LegacySmithingTransformRecipe(template, base, addition, result);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        this.template.write(buf);
        this.base.write(buf);
        this.addition.write(buf);
        CraftEngine.instance().itemManager().encode(buf, this.result);
    }
}
