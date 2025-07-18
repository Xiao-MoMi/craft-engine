package net.momirealms.craftengine.core.item.recipe.network.modern.display.slot;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public class ItemStackSlotDisplay implements SlotDisplay {
    private Item<Object> item;

    public ItemStackSlotDisplay(Item<Object> item) {
        this.item = item;
    }

    public static ItemStackSlotDisplay read(FriendlyByteBuf buf) {
        Item<Object> itemStack = CraftEngine.instance().itemManager().decode(buf);
        return new ItemStackSlotDisplay(itemStack);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(3);
        CraftEngine.instance().itemManager().encode(buf, this.item);
    }

    @Override
    public void applyClientboundData(Player player) {
        this.item = CraftEngine.instance().itemManager().s2c(this.item, player);
    }

    public Item<?> item() {
        return this.item;
    }

    public void setItem(Item<Object> item) {
        this.item = item;
    }

    @Override
    public String toString() {
        return "ItemStackSlotDisplay{" +
                "item=" + this.item.getLiteralObject() +
                '}';
    }
}
