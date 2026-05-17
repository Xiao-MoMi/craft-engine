package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.font.BukkitFontManager;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.List;

public final class CustomChatCompletionsListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new CustomChatCompletionsListener();

    private CustomChatCompletionsListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        int action = buf.readVarInt();
        if (action != 2/*SET*/) return;
        List<String> entries = buf.readStringList();
        entries.addAll(BukkitFontManager.instance().getEmojiSuggestions((net.momirealms.craftengine.core.entity.player.Player) user));
        event.setChanged(true);
        buf.clear();
        buf.writeVarInt(event.packetID());
        buf.writeVarInt(action);
        buf.writeStringList(entries);
    }
}
