package net.momirealms.craftengine.core.plugin.network.mod;

import io.netty.buffer.Unpooled;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.PacketFlow;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.plugin.network.mod.protocol.CancelBlockUpdatePacket;
import net.momirealms.craftengine.core.plugin.network.mod.protocol.ClientBlockStateSizePacket;
import net.momirealms.craftengine.core.plugin.network.mod.protocol.ClientCustomBlockPacket;
import net.momirealms.craftengine.core.plugin.network.mod.protocol.VisualBlockStatePacket;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.function.Supplier;

public final class ModPackets {
    public static final ModPacketType<ClientCustomBlockPacket> CLIENT_CUSTOM_BLOCK = register(ClientCustomBlockPacket.TYPE, ClientCustomBlockPacket.CODEC);
    public static final ModPacketType<CancelBlockUpdatePacket> CANCEL_BLOCK_UPDATE = register(CancelBlockUpdatePacket.TYPE, CancelBlockUpdatePacket.CODEC);
    public static final ModPacketType<ClientBlockStateSizePacket> CLIENT_BLOCK_STATE_SIZE = register(ClientBlockStateSizePacket.TYPE, ClientBlockStateSizePacket.CODEC);
    public static final ModPacketType<VisualBlockStatePacket> VISUAL_BLOCK_STATE = register(VisualBlockStatePacket.TYPE, VisualBlockStatePacket.CODEC);

    private ModPackets() {
    }

    public static void init() {
    }

    public static <T extends ModPacket> ModPacketType<T> register(ResourceKey<NetworkCodec<FriendlyByteBuf, ? extends ModPacket>> key, NetworkCodec<FriendlyByteBuf, T> codec) {
        ModPacketType<T> type = new ModPacketType<>(key.location(), codec);
        ((WritableRegistry<NetworkCodec<FriendlyByteBuf, ? extends ModPacket>>) BuiltInRegistries.MOD_PACKET).register(key, codec);
        return type;
    }

    public static void sendPacket(NetWorkUser user, ModPacket packet) {
        if (!Config.enableModChannel()) return;
        @SuppressWarnings("unchecked")
        NetworkCodec<FriendlyByteBuf, ModPacket> codec = (NetworkCodec<FriendlyByteBuf, ModPacket>) BuiltInRegistries.MOD_PACKET.getValue(packet.type());
        if (codec == null) {
            CraftEngine.instance().logger().warn("Unknown data type class: " + packet.getClass().getName());
            return;
        }
        if (Config.modChannelRequiresPermission()) {
            String permission = packet.permission(PacketFlow.CLIENTBOUND);
            if (permission != null && !CraftEngine.instance().compatibilityManager().hasPermission(user, permission)) {
                Debugger.COMMON.debug(() -> "Player " + user.name() + " does not have " + permission + " permission to receive " + packet.type().location());
                return;
            }
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeByte(BuiltInRegistries.MOD_PACKET.getId(codec));
        codec.encode(buf, packet);
        user.sendCustomPayload(ModChannelKeys.CRAFTENGINE_CHANNEL, buf.array());
    }

    public static void handleReceive(NetWorkUser user, Key channel, Supplier<FriendlyByteBuf> buf) {
        if (!Config.enableModChannel() || !ModChannelKeys.CRAFTENGINE_CHANNEL.equals(channel)) return;
        try {
            FriendlyByteBuf byteBuf = buf.get();
            byte type = byteBuf.readByte();
            @SuppressWarnings("unchecked")
            NetworkCodec<FriendlyByteBuf, ModPacket> codec = (NetworkCodec<FriendlyByteBuf, ModPacket>) BuiltInRegistries.MOD_PACKET.getValue(type);
            if (codec == null) {
                Debugger.COMMON.debug(() -> "Unknown data type received: " + type);
                return;
            }
            ModPacket packet = codec.decode(byteBuf);
            if (Config.modChannelRequiresPermission()) {
                String permission = packet.permission(PacketFlow.SERVERBOUND);
                if (permission != null && !CraftEngine.instance().compatibilityManager().hasPermission(user, permission)) {
                    Debugger.COMMON.debug(() -> "Player " + user.name() + " does not have " + permission + " permission to send " + packet.type().location());
                    return;
                }
            }
            packet.handle(user);
        } catch (Throwable e) {
            // 乱发包我给你踹了
            user.kick(Component.translatable(
                    "disconnect.craftengine.invalid_payload",
                    "Connection terminated due to transmission of invalid payload. \n Please ensure that the client mod and server plugin are the latest version."
            ));
            Debugger.PACKET.warn(() -> "Failed to handle receive", e);
        }
    }
}
