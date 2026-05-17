package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;

public final class EntityEventListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new EntityEventListener();

    private EntityEventListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        Object player = user.serverPlayer();
        if (player == null) return;
        FriendlyByteBuf buf = event.getBuffer();
        int entityId = buf.readInt();
        if (entityId != EntityProxy.INSTANCE.getId(player)) return;
        byte eventId = buf.readByte();
        if (eventId >= 24 && eventId <= 28) {
            CraftEngine.instance().fontManager().refreshEmojiSuggestions((BukkitServerPlayer) user);
        }
    }
}
