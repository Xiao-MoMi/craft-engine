package net.momirealms.craftengine.core.plugin.network.mod;

import io.netty.buffer.Unpooled;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public class ModPackets {
    protected ModPackets() {
    }

    public static <T extends ModPacket> ModPacketType<T> register(ResourceKey<NetworkCodec<FriendlyByteBuf, ? extends ModPacket>> key, NetworkCodec<FriendlyByteBuf, T> codec) {
        ModPacketType<T> type = new ModPacketType<>(key.location(), codec);
        ((WritableRegistry<NetworkCodec<FriendlyByteBuf, ? extends ModPacket>>) BuiltInRegistries.MOD_PACKET).register(key, codec);
        return type;
    }

    public static void sendPacket(NetWorkUser user, ModPacket packet) {
        if (!Config.clientCommunication()) return;
        @SuppressWarnings("unchecked")
        NetworkCodec<FriendlyByteBuf, ModPacket> codec = (NetworkCodec<FriendlyByteBuf, ModPacket>) BuiltInRegistries.MOD_PACKET.getValue(packet.type());
        if (codec == null) {
            CraftEngine.instance().logger().warn("Unknown data type class: " + packet.getClass().getName());
            return;
        }
        if (Config.clientCommunicationPermission()) {
            Key key = BuiltInRegistries.MOD_PACKET.getKey(codec);
            String permission = Config.clientCommunicationPermissionClientbound(key);
            if (permission != null && !CraftEngine.instance().compatibilityManager().hasPermission(user, permission)) {
                Debugger.COMMON.debug(() -> "Player " + user.name() + " does not have permission to receive " + key);
                return;
            }
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeByte(BuiltInRegistries.MOD_PACKET.getId(codec));
        codec.encode(buf, packet);
        user.sendCustomPayload(ModChannelKeys.CRAFTENGINE_CHANNEL, buf.array());
    }

    public static void handlePayload(NetWorkUser user, Payload payload) {
        if (!Config.clientCommunication()) return;
        try {
            if (payload.channel().equals(ModChannelKeys.CRAFTENGINE_CHANNEL)) {
                handleCraftEnginePayload(user, payload);
            }
        } catch (Throwable e) {
            // 乱发包我给你踹了
            user.kick(Component.translatable(
                    "disconnect.craftengine.invalid_payload",
                    "Connection terminated due to transmission of invalid payload. \n Please ensure that the client mod and server plugin are the latest version."
            ));
            Debugger.COMMON.warn(() -> "Failed to handle payload", e);
        }
    }

    private static void handleCraftEnginePayload(NetWorkUser user, Payload payload) {
        FriendlyByteBuf buf = payload.toBuffer();
        byte type = buf.readByte();
        @SuppressWarnings("unchecked")
        NetworkCodec<FriendlyByteBuf, ModPacket> codec = (NetworkCodec<FriendlyByteBuf, ModPacket>) BuiltInRegistries.MOD_PACKET.getValue(type);
        if (codec == null) {
            Debugger.COMMON.debug(() -> "Unknown data type received: " + type);
            return;
        }
        if (Config.clientCommunicationPermission()) {
            Key key = BuiltInRegistries.MOD_PACKET.getKey(codec);
            String permission = Config.clientCommunicationPermissionServerbound(key);
            if (permission != null && !CraftEngine.instance().compatibilityManager().hasPermission(user, permission)) {
                Debugger.COMMON.debug(() -> "Player " + user.name() + " does not have permission to send " + key);
                return;
            }
        }
        ModPacket networkData = codec.decode(buf);
        networkData.handle(user);
    }
}
