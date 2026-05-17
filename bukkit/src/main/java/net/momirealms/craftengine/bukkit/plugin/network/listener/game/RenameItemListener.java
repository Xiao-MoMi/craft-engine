package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.font.FontManager;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.IllegalCharacterProcessResult;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public final class RenameItemListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new RenameItemListener();

    private RenameItemListener() {}

    @Override
    public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
        if (!Config.filterAnvil()) return;
        if (((BukkitServerPlayer) user).hasPermission(FontManager.BYPASS_ANVIL)) {
            return;
        }
        FriendlyByteBuf buf = event.getBuffer();
        String message = buf.readUtf();
        if (message != null && !message.isEmpty()) {
            // check bypass
            IllegalCharacterProcessResult result = BukkitNetworkManager.instance().processIllegalCharacters(message);
            if (result.has()) {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeUtf(result.text());
            }
        }
    }
}
