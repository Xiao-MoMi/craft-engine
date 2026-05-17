package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.font.FontManager;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.IllegalCharacterProcessResult;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.world.BlockPos;

public final class SignUpdateListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new SignUpdateListener();

    private SignUpdateListener() {}

    @Override
    public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
        if (!Config.filterSign()) return;
        // check bypass
        if (((BukkitServerPlayer) user).hasPermission(FontManager.BYPASS_SIGN)) {
            return;
        }
        FontManager manager = CraftEngine.instance().fontManager();
        if (!manager.isDefaultFontInUse()) return;
        FriendlyByteBuf buf = event.getBuffer();
        BlockPos pos = buf.readBlockPos();
        boolean isFrontText = buf.readBoolean();
        String[] lines = new String[4];
        boolean changed = false;
        for(int i = 0; i < 4; ++i) {
            String line = buf.readUtf();
            if (line != null && !line.isEmpty()) {
                IllegalCharacterProcessResult result = BukkitNetworkManager.instance().processIllegalCharacters(line);
                if (result.has()) {
                    lines[i] = result.text();
                    changed = true;
                } else {
                    lines[i] = line;
                }
            } else {
                lines[i] = "";
            }
        }
        if (changed) {
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeBlockPos(pos);
            buf.writeBoolean(isFrontText);
            for (String line : lines) {
                buf.writeUtf(line);
            }
        }
    }
}
