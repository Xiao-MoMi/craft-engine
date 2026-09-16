package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import com.mojang.datafixers.util.Pair;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.NMSPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.NMSPacketListener;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEquipmentPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class NMSSetEquipmentListener implements NMSPacketListener {
    public static final NMSSetEquipmentListener INSTANCE = new NMSSetEquipmentListener();

    private NMSSetEquipmentListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, NMSPacketEvent event, Object packet) {
        if (Config.disableItemOperations()) return;
        BukkitServerPlayer serverPlayer = (BukkitServerPlayer) user;
        List<Pair<Object, Object>> slots = ClientboundSetEquipmentPacketProxy.INSTANCE.getSlots(packet);
        List<Pair<Object, Object>> newSlots = new ArrayList<>(slots.size());
        boolean changed = false;
        for (Pair<Object, Object> slot : slots) {
            Optional<Item> converted = BukkitItemManager.instance().s2c(ItemStackUtils.wrap(ItemStackProxy.INSTANCE.copy(slot.getSecond())), serverPlayer, ItemPacketSource.SET_EQUIPMENT);
            if (converted.isPresent()) {
                changed = true;
                newSlots.add(Pair.of(slot.getFirst(), converted.get().minecraftItem()));
            } else {
                newSlots.add(slot);
            }
        }
        if (!changed) return;
        PacketUtils.replacePacket(event, packet, ClientboundSetEquipmentPacketProxy.INSTANCE.newInstance(
                ClientboundSetEquipmentPacketProxy.INSTANCE.getEntityId(packet),
                newSlots
        ));
    }
}
