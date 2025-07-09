package net.momirealms.craftengine.core.item.recipe.network.display;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public interface SlotDisplay {

    void write(FriendlyByteBuf buf);
}
