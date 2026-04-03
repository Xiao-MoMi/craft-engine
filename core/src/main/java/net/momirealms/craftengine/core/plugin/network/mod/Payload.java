package net.momirealms.craftengine.core.plugin.network.mod;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;

public interface Payload {

    FriendlyByteBuf toBuffer();

    Key channel();
}
