package net.momirealms.craftengine.bukkit.plugin.network.listener;

import net.momirealms.craftengine.core.plugin.network.NMSPacketEvent;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;

public interface NMSPacketListener {

    default void onPacketReceive(NetWorkUser user, NMSPacketEvent event, Object packet) {
    }

    default void onPacketSend(NetWorkUser user, NMSPacketEvent event, Object packet) {
    }
}
