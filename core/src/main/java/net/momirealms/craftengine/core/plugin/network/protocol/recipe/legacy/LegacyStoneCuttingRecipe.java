package net.momirealms.craftengine.core.plugin.network.protocol.recipe.legacy;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

@ApiStatus.Obsolete
public final class LegacyStoneCuttingRecipe implements LegacyRecipe {
    private Item result;
    private final String group;
    private final LegacyIngredient ingredient;

    public LegacyStoneCuttingRecipe(LegacyIngredient ingredient,
                                    Item result,
                                    String group) {
        this.ingredient = ingredient;
        this.result = result;
        this.group = group;
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        this.result = function.apply(this.result);
        this.ingredient.applyClientboundData(function);
    }

    public static LegacyStoneCuttingRecipe read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        String group = buf.readUtf();
        LegacyIngredient ingredient = LegacyIngredient.read(buf, reader);
        Item result = reader.apply(buf);
        return new LegacyStoneCuttingRecipe(ingredient, result, group);
    }

    @Override
    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeUtf(this.group);
        this.ingredient.write(buf, writer);
        writer.accept(buf, this.result);
    }
}
