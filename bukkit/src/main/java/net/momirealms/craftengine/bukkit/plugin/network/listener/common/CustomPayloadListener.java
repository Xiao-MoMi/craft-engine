package net.momirealms.craftengine.bukkit.plugin.network.listener.common;

import net.momirealms.craftengine.bukkit.entity.furniture.behavior.GlowingFurnitureBehaviorTemplate;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.plugin.network.mod.ModPackets;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;

public final class CustomPayloadListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new CustomPayloadListener();
    
    private CustomPayloadListener() {}

    @Override
    public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        Key channel = buf.readKey();
        ModPackets.handleReceive(user, channel, () -> new FriendlyByteBuf(buf.readBytes(buf.readableBytes())));
    }

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        Key channel = buf.readKey();
        if (channel.equals(GlowingFurnitureBehaviorTemplate.PAYLOAD_ID)) {
            GlowingFurnitureBehaviorTemplate.handleLightPacket(user, event, buf);
        }
    }
}
