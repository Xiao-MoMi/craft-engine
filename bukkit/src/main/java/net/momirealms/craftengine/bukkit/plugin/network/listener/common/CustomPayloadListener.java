package net.momirealms.craftengine.bukkit.plugin.network.listener.common;

import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.ConnectionState;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.plugin.network.mod.ClientCustomPacket;
import net.momirealms.craftengine.core.plugin.network.mod.ClientCustomPacketType;
import net.momirealms.craftengine.core.plugin.network.mod.ServerCustomPacket;
import net.momirealms.craftengine.core.plugin.network.mod.ServerCustomPacketType;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.GameEdition;
import net.momirealms.craftengine.core.util.Key;

public final class CustomPayloadListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new CustomPayloadListener();
    private static final Key BRAND = Key.of("minecraft:brand");
    
    private CustomPayloadListener() {}

    @Override
    public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        Key packetId = buf.readKey();
        if (Config.identifyBedrockByGeyserBrand() && user.decoderState() == ConnectionState.CONFIGURATION && BRAND.equals(packetId)) {
            if ("Geyser".equals(buf.readUtf(256))) {
                ((BukkitServerPlayer) user).setGameEdition(GameEdition.BEDROCK);
            }
            return;
        }
        ServerCustomPacketType<? extends ServerCustomPacket> type = BuiltInRegistries.SERVER_MOD_PACKET.getValue(packetId);
        if (type == null) return;
        if (type.checkPermission(user)) {
            ServerCustomPacket packet = type.codec().decode(buf);
            packet.handle(user, event);
        } else {
            event.setCancelled(true);
        }
    }

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        Key packetId = buf.readKey();
        ClientCustomPacketType<? extends ClientCustomPacket> type = BuiltInRegistries.CLIENT_MOD_PACKET.getValue(packetId);
        if (type == null || !type.inServerHandle()) return;
        ClientCustomPacket packet = type.codec().decode(buf);
        packet.handle(user, event);
    }
}
