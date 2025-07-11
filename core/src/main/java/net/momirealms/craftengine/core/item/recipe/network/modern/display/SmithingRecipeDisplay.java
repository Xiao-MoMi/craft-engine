package net.momirealms.craftengine.core.item.recipe.network.modern.display;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.recipe.network.modern.display.slot.SlotDisplay;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public record SmithingRecipeDisplay(SlotDisplay template, SlotDisplay base, SlotDisplay addition, SlotDisplay result, SlotDisplay craftingStation) implements RecipeDisplay {

    public static SmithingRecipeDisplay read(FriendlyByteBuf buffer) {
        SlotDisplay template = SlotDisplay.read(buffer);
        SlotDisplay base = SlotDisplay.read(buffer);
        SlotDisplay addition = SlotDisplay.read(buffer);
        SlotDisplay result = SlotDisplay.read(buffer);
        SlotDisplay craftingStation = SlotDisplay.read(buffer);
        return new SmithingRecipeDisplay(template, base, addition, result, craftingStation);
    }

    @Override
    public void applyClientboundData(Player player) {
        this.template.applyClientboundData(player);
        this.base.applyClientboundData(player);
        this.addition.applyClientboundData(player);
        this.result.applyClientboundData(player);
        this.craftingStation.applyClientboundData(player);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(4);
        this.template.write(buf);
        this.base.write(buf);
        this.addition.write(buf);
        this.result.write(buf);
        this.craftingStation.write(buf);
    }

    @Override
    public @NotNull String toString() {
        return "SmithingRecipeDisplay{" +
                "addition=" + addition +
                ", template=" + template +
                ", base=" + base +
                ", result=" + result +
                ", craftingStation=" + craftingStation +
                '}';
    }
}
