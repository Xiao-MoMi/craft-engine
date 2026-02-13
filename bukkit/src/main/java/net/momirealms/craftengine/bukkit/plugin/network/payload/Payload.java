package net.momirealms.craftengine.bukkit.plugin.network.payload;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;

public interface Payload {

    FriendlyByteBuf toBuffer();

    Key channel();
}
