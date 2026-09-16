package net.momirealms.craftengine.core.plugin.network.mod;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslationArgument;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.plugin.network.mod.protocol.*;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.function.BiPredicate;

public final class CustomPackets {
    public static final int PROTOCOL_VERSION = 1;
    public static final BiPredicate<NetWorkUser, Key> ALWAYS_ALLOWED = (user, key) -> true;
    public static final ClientCustomPacketType<ClientboundLightPacket> LIGHT = registerClientbound(ClientboundLightPacket.ID, ClientboundLightPacket.CODEC, ALWAYS_ALLOWED, true);
    public static final ClientCustomPacketType<ClientboundVisualBlockStateBatchStartPacket> VISUAL_BLOCK_STATE_BATCH_START = registerClientbound(ClientboundVisualBlockStateBatchStartPacket.ID, ClientboundVisualBlockStateBatchStartPacket.CODEC, false);
    public static final ClientCustomPacketType<ClientboundVisualBlockStateBatchFinishedPacket> VISUAL_BLOCK_STATE_BATCH_FINISHED = registerClientbound(ClientboundVisualBlockStateBatchFinishedPacket.ID, ClientboundVisualBlockStateBatchFinishedPacket.CODEC, false);
    public static final ClientCustomPacketType<ClientboundVisualBlockStatesPacket> VISUAL_BLOCK_STATES = registerClientbound(ClientboundVisualBlockStatesPacket.ID, ClientboundVisualBlockStatesPacket.CODEC, false);
    public static final ClientCustomPacketType<ClientboundCancelBlockUpdateResponsePacket> CANCEL_BLOCK_UPDATE_RESPONSE = registerClientbound(ClientboundCancelBlockUpdateResponsePacket.ID, ClientboundCancelBlockUpdateResponsePacket.CODEC, false);
    public static final ClientCustomPacketType<ClientboundCreativeModeTabItemsPacket> CREATIVE_MODE_TAB_ITEMS = registerClientbound(ClientboundCreativeModeTabItemsPacket.ID, ClientboundCreativeModeTabItemsPacket.CODEC, false);
    public static final ServerCustomPacketType<ServerboundHandshakePacket> HANDSHAKE = registerServerbound(ServerboundHandshakePacket.ID, ServerboundHandshakePacket.CODEC, (p, k) -> true);
    public static final ServerCustomPacketType<ServerboundEnableClientCustomBlockPacket> ENABLE_CLIENT_CUSTOM_BLOCK = registerServerbound(ServerboundEnableClientCustomBlockPacket.ID, ServerboundEnableClientCustomBlockPacket.CODEC);
    public static final ServerCustomPacketType<ServerboundCancelBlockUpdateRequestPacket> CANCEL_BLOCK_UPDATE_REQUEST = registerServerbound(ServerboundCancelBlockUpdateRequestPacket.ID, ServerboundCancelBlockUpdateRequestPacket.CODEC);
    public static final ServerCustomPacketType<ServerboundLegacyPacket> LEGACY_PACKET = registerServerbound(ServerboundLegacyPacket.ID, ServerboundLegacyPacket.CODEC);

    private CustomPackets() {
    }

    public static void init() {
    }

    public static <T extends ClientCustomPacket> ClientCustomPacketType<T> registerClientbound(Key id, NetworkCodec<FriendlyByteBuf, T> codec, boolean inServerHandle) {
        return registerClientbound(id, codec, CustomPackets::checkClientboundPacketPermission, inServerHandle);
    }

    public static <T extends ClientCustomPacket> ClientCustomPacketType<T> registerClientbound(Key id, NetworkCodec<FriendlyByteBuf, T> codec, BiPredicate<NetWorkUser, Key> permissionChecker, boolean inServerHandle) {
        ClientCustomPacketType<T> type = new ClientCustomPacketType<>(id, codec, permissionChecker, inServerHandle);
        ((WritableRegistry<ClientCustomPacketType<? extends ClientCustomPacket>>) BuiltInRegistries.CLIENT_MOD_PACKET)
                .register(ResourceKey.create(Registries.CLIENT_MOD_PACKET.location(), id), type);
        return type;
    }

    public static <T extends ServerCustomPacket> ServerCustomPacketType<T> registerServerbound(Key id, NetworkCodec<FriendlyByteBuf, T> codec) {
        return registerServerbound(id, codec, CustomPackets::checkServerboundPacketPermission);
    }

    public static <T extends ServerCustomPacket> ServerCustomPacketType<T> registerServerbound(Key id, NetworkCodec<FriendlyByteBuf, T> codec, BiPredicate<NetWorkUser, Key> permissionChecker) {
        ServerCustomPacketType<T> type = new ServerCustomPacketType<>(id, codec, permissionChecker);
        ((WritableRegistry<ServerCustomPacketType<? extends ServerCustomPacket>>) BuiltInRegistries.SERVER_MOD_PACKET)
                .register(ResourceKey.create(Registries.SERVER_MOD_PACKET.location(), id), type);
        return type;
    }

    public static boolean checkServerboundPacketPermission(NetWorkUser user, Key id) {
        return checkPermission(user, id, false);
    }

    public static boolean checkClientboundPacketPermission(NetWorkUser user, Key id) {
        return checkPermission(user, id, true);
    }

    public static boolean checkPermission(NetWorkUser user, Key id, boolean toClient) {
        if (!Config.modChannelRequiresPermission()) return true;
        String permission = "ce.mod." + (toClient ? "clientbound" : "serverbound") + "." + (id.namespace.equals("craftengine") ? id.value : id);
        boolean hasPermission = CraftEngine.instance().compatibilityManager().hasPermission(user, permission);
        if (!hasPermission && Config.modChannelLoggingPermissionDenied()) {
            CraftEngine.instance().logger().warn(TranslationManager.instance().plainTranslation(
                    toClient ? "mod.clientbound.no_permission" : "mod.serverbound.no_permission",
                    user.name(), permission, id.asString()
            ));
        }
        return hasPermission;
    }

    public static void checkProtocolVersion(NetWorkUser user) {
        if (user.clientModProtocol() == PROTOCOL_VERSION) return;
        user.kick(Component.translatable(
                "disconnect.craftengine.client_outdated",
                "Please update your CraftEngine Client Mod \n client protocol version " + user.clientModProtocol() + ", server protocol version " + PROTOCOL_VERSION,
                TranslationArgument.numeric(user.clientModProtocol()),
                TranslationArgument.numeric(PROTOCOL_VERSION)
        ));
    }
}
