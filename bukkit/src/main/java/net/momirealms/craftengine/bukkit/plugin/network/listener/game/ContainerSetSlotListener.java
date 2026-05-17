package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public final class ContainerSetSlotListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new ContainerSetSlotListener();

    private ContainerSetSlotListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        if (Config.disableItemOperations()) return;
        if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
        FriendlyByteBuf buf = event.getBuffer();
        int containerId = buf.readContainerId();
        int stateId = buf.readVarInt();
        int slot = buf.readShort();
        Item itemStack;
        try {
            itemStack = PacketUtils.readItem(buf);
        } catch (Exception e) {
            // 其他插件干的，发送了非法的物品
            return;
        }
        BukkitItemManager.instance().s2c(itemStack, serverPlayer).ifPresent((newItemStack) -> {
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeContainerId(containerId);
            buf.writeVarInt(stateId);
            buf.writeShort(slot);
            PacketUtils.writeItem(buf, newItemStack);
        });
    }
}
