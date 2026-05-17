package net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display.slot.SlotDisplay;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record SmithingRecipeDisplay(SlotDisplay template, SlotDisplay base, SlotDisplay addition, SlotDisplay result, SlotDisplay craftingStation) implements RecipeDisplay {

    public static SmithingRecipeDisplay read(FriendlyByteBuf buffer, FriendlyByteBuf.Reader<Item> reader) {
        SlotDisplay template = SlotDisplay.read(buffer, reader);
        SlotDisplay base = SlotDisplay.read(buffer, reader);
        SlotDisplay addition = SlotDisplay.read(buffer, reader);
        SlotDisplay result = SlotDisplay.read(buffer, reader);
        SlotDisplay craftingStation = SlotDisplay.read(buffer, reader);
        return new SmithingRecipeDisplay(template, base, addition, result, craftingStation);
    }

    @Override
    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeVarInt(4);
        this.template.write(buf, writer);
        this.base.write(buf, writer);
        this.addition.write(buf, writer);
        this.result.write(buf, writer);
        this.craftingStation.write(buf, writer);
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        this.template.applyClientboundData(function);
        this.base.applyClientboundData(function);
        this.addition.applyClientboundData(function);
        this.result.applyClientboundData(function);
        this.craftingStation.applyClientboundData(function);
    }

    @Override
    public @NotNull String toString() {
        return "SmithingRecipeDisplay{" +
                "addition=" + this.addition +
                ", template=" + this.template +
                ", base=" + this.base +
                ", result=" + this.result +
                ", craftingStation=" + this.craftingStation +
                '}';
    }
}
