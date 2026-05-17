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
import net.momirealms.craftengine.core.util.VersionHelper;

public final class SetPlayerInventoryListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = VersionHelper.isOrAbove1_21_2 ? new SetPlayerInventoryListener() : null;

    private SetPlayerInventoryListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        if (Config.disableItemOperations()) return;
        if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
        FriendlyByteBuf buf = event.getBuffer();
        int slot = buf.readVarInt();
        Item itemStack = PacketUtils.readItem(buf);
        BukkitItemManager.instance().s2c(itemStack, serverPlayer).ifPresent((newItemStack) -> {
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeVarInt(slot);
            PacketUtils.writeItem(buf, newItemStack);
        });
    }
}
