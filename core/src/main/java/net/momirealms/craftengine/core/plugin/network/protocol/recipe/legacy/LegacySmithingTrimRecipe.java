package net.momirealms.craftengine.core.plugin.network.protocol.recipe.legacy;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

@ApiStatus.Obsolete
public final class LegacySmithingTrimRecipe implements LegacyRecipe {
    private final LegacyIngredient template;
    private final LegacyIngredient base;
    private final LegacyIngredient addition;

    public LegacySmithingTrimRecipe(LegacyIngredient addition, LegacyIngredient template, LegacyIngredient base) {
        this.addition = addition;
        this.template = template;
        this.base = base;
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        this.template.applyClientboundData(function);
        this.base.applyClientboundData(function);
        this.addition.applyClientboundData(function);
    }

    public static LegacySmithingTrimRecipe read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        LegacyIngredient template = LegacyIngredient.read(buf, reader);
        LegacyIngredient base = LegacyIngredient.read(buf, reader);
        LegacyIngredient addition = LegacyIngredient.read(buf, reader);
        return new LegacySmithingTrimRecipe(template, base, addition);
    }

    @Override
    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        this.template.write(buf, writer);
        this.base.write(buf, writer);
        this.addition.write(buf, writer);
    }
}
