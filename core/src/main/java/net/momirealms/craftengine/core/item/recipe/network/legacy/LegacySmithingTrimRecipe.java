package net.momirealms.craftengine.core.item.recipe.network.legacy;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Obsolete
public class LegacySmithingTrimRecipe implements LegacyRecipe {
    private final LegacyIngredient template;
    private final LegacyIngredient base;
    private final LegacyIngredient addition;

    public LegacySmithingTrimRecipe(LegacyIngredient addition, LegacyIngredient template, LegacyIngredient base) {
        this.addition = addition;
        this.template = template;
        this.base = base;
    }

    @Override
    public void applyClientboundData(Player player) {
        this.template.applyClientboundData(player);
        this.base.applyClientboundData(player);
        this.addition.applyClientboundData(player);
    }

    public static LegacySmithingTrimRecipe read(FriendlyByteBuf buf) {
        LegacyIngredient template = LegacyIngredient.read(buf);
        LegacyIngredient base = LegacyIngredient.read(buf);
        LegacyIngredient addition = LegacyIngredient.read(buf);
        return new LegacySmithingTrimRecipe(template, base, addition);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        this.template.write(buf);
        this.base.write(buf);
        this.addition.write(buf);
    }
}
