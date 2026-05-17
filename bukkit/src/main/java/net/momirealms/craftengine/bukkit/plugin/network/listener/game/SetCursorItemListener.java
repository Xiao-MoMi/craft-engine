package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.AbstractContainerMenuProxy;

public final class SetCursorItemListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new SetCursorItemListener();

    private SetCursorItemListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        if (Config.disableItemOperations()) return;
        if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
        FriendlyByteBuf buf = event.getBuffer();
        Item item = PacketUtils.readItem(buf);

        // 为了避免其他插件造成的手感冲突
        if (VersionHelper.isOrAbove1_21_5) {
            // 发出来的是非空物品
            if (!item.isEmpty()) {
                Object containerMenu = PlayerProxy.INSTANCE.getContainerMenu(serverPlayer.serverPlayer());
                if (containerMenu != null) {
                    Item carried = ItemStackUtils.wrap(AbstractContainerMenuProxy.INSTANCE.getCarried(containerMenu));
                    // 但服务端上实际确是空气，就把它写成空气，避免因为其他插件导致手感问题
                    if (carried.isEmpty()) {
                        event.setChanged(true);
                        buf.clear();
                        buf.writeVarInt(event.packetID());
                        PacketUtils.writeItem(buf, carried);
                        return;
                    }
                }
            }
        }

        BukkitItemManager.instance().s2c(item, serverPlayer).ifPresent((newItemStack) -> {
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            PacketUtils.writeItem(buf, newItemStack);
        });
    }
}
