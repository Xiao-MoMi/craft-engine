package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.plugin.network.EntityDataValueReplacer;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.NMSPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.NMSPacketListener;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;

import java.util.ArrayList;
import java.util.List;

public final class NMSSetEntityDataListener implements NMSPacketListener {
    public static final NMSSetEntityDataListener INSTANCE = new NMSSetEntityDataListener();

    private NMSSetEntityDataListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, NMSPacketEvent event, Object packet) {
        if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
        List<Object> packedItems = ClientboundSetEntityDataPacketProxy.INSTANCE.getPackedItems(packet);
        List<Object> newItems = null;
        for (int i = 0, size = packedItems.size(); i < size; i++) {
            Object newDataValue = EntityDataValueReplacer.replace(serverPlayer, packedItems.get(i));
            if (newDataValue == null) continue;
            if (newItems == null) newItems = new ArrayList<>(packedItems);
            newItems.set(i, newDataValue);
        }
        if (newItems != null) {
            PacketUtils.replacePacket(event, packet, ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(ClientboundSetEntityDataPacketProxy.INSTANCE.getId(packet), newItems));
        }
    }
}
