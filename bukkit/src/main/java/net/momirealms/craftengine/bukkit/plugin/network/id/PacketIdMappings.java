package net.momirealms.craftengine.bukkit.plugin.network.id;

import io.netty.buffer.ByteBuf;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.plugin.network.ConnectionState;
import net.momirealms.craftengine.core.plugin.network.PacketFlow;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.network.ConnectionProtocolProxy;
import net.momirealms.craftengine.proxy.minecraft.network.ProtocolDetailsProviderProxy;
import net.momirealms.craftengine.proxy.minecraft.network.ProtocolDetailsProxy;
import net.momirealms.craftengine.proxy.minecraft.network.ProtocolInfoProxy;
import net.momirealms.craftengine.proxy.minecraft.network.ProtocolPacketVisitorProxy;
import net.momirealms.craftengine.proxy.minecraft.network.RegistryFriendlyByteBufProxy;
import net.momirealms.craftengine.proxy.minecraft.network.UnboundPacketVisitorProxy;
import net.momirealms.craftengine.proxy.minecraft.network.UnboundProtocolInfoProxy;
import net.momirealms.craftengine.proxy.minecraft.network.codec.IdDispatchCodecProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketFlowProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.configuration.ConfigurationProtocolsProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.GameProtocolsProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.handshake.HandshakeProtocolsProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.login.LoginProtocolsProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.status.StatusProtocolsProxy;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static net.momirealms.craftengine.core.plugin.network.ConnectionState.*;
import static net.momirealms.craftengine.core.plugin.network.PacketFlow.CLIENTBOUND;
import static net.momirealms.craftengine.core.plugin.network.PacketFlow.SERVERBOUND;

final class PacketIdMappings {
    private PacketIdMappings() {
    }

    static Map<ConnectionState, Map<PacketFlow, Map<Class<?>, Integer>>> byClass() {
        Map<ConnectionState, Map<PacketFlow, Map<Class<?>, Integer>>> result = new EnumMap<>(ConnectionState.class);
        Object[] flows = PacketFlowProxy.CLASS.getEnumConstants();
        for (Object protocol : ConnectionProtocolProxy.CLASS.getEnumConstants()) {
            ConnectionState state = ConnectionState.valueOf(((Enum<?>) protocol).name().toUpperCase(Locale.ROOT));
            Map<PacketFlow, Map<Class<?>, Integer>> directions = new EnumMap<>(PacketFlow.class);
            for (Object flow : flows) {
                PacketFlow direction = PacketFlow.valueOf(((Enum<?>) flow).name().toUpperCase(Locale.ROOT));
                Map<Class<?>, Integer> ids = new HashMap<>();
                ConnectionProtocolProxy.INSTANCE.getPacketsByIds(protocol, flow).forEach((id, packetClass) -> ids.put(packetClass, id));
                directions.put(direction, ids);
            }
            result.put(state, directions);
        }
        return result;
    }

    static Map<ConnectionState, Map<PacketFlow, Map<String, Integer>>> byName() {
        Map<ConnectionState, Map<PacketFlow, Map<String, Integer>>> result = new EnumMap<>(ConnectionState.class);
        for (ConnectionState state : ConnectionState.values()) {
            Map<PacketFlow, Map<String, Integer>> directions = new EnumMap<>(PacketFlow.class);
            directions.put(CLIENTBOUND, new HashMap<>());
            directions.put(SERVERBOUND, new HashMap<>());
            result.put(state, directions);
        }

        boolean templates = VersionHelper.isOrAbove1_21;
        HandshakeProtocolsProxy handshake = HandshakeProtocolsProxy.INSTANCE;
        StatusProtocolsProxy status = StatusProtocolsProxy.INSTANCE;
        LoginProtocolsProxy login = LoginProtocolsProxy.INSTANCE;
        ConfigurationProtocolsProxy configuration = ConfigurationProtocolsProxy.INSTANCE;
        GameProtocolsProxy game = GameProtocolsProxy.INSTANCE;

        read(result.get(HANDSHAKING).get(SERVERBOUND), templates ? handshake.serverbound() : handshake.serverbound$legacy());
        read(result.get(STATUS).get(CLIENTBOUND), templates ? status.clientbound() : status.clientbound$legacy());
        read(result.get(STATUS).get(SERVERBOUND), templates ? status.serverbound() : status.serverbound$legacy());
        read(result.get(LOGIN).get(CLIENTBOUND), templates ? login.clientbound() : login.clientbound$legacy());
        read(result.get(LOGIN).get(SERVERBOUND), templates ? login.serverbound() : login.serverbound$legacy());
        read(result.get(CONFIGURATION).get(CLIENTBOUND), templates ? configuration.clientbound() : configuration.clientbound$legacy());
        read(result.get(CONFIGURATION).get(SERVERBOUND), templates ? configuration.serverbound() : configuration.serverbound$legacy());
        read(result.get(PLAY).get(CLIENTBOUND), templates ? game.clientbound() : bind(game.clientbound$legacy()));
        read(result.get(PLAY).get(SERVERBOUND), templates ? game.serverbound() : bind(game.serverbound$legacy()));
        return result;
    }

    private static Object bind(Object template) {
        return UnboundProtocolInfoProxy.INSTANCE.bind(template, PacketIdMappings::registryBuffer);
    }

    private static ByteBuf registryBuffer(ByteBuf buffer) {
        return RegistryFriendlyByteBufProxy.INSTANCE.newInstance(buffer, RegistryUtils.getRegistryAccess());
    }

    private static void read(Map<String, Integer> ids, Object protocol) {
        if (VersionHelper.isOrAbove1_21_5) {
            Object details = ProtocolDetailsProviderProxy.INSTANCE.details(protocol);
            ProtocolDetailsProxy.INSTANCE.listPackets(details, visitor(ProtocolPacketVisitorProxy.CLASS, ids));
        } else if (VersionHelper.isOrAbove1_21) {
            UnboundProtocolInfoProxy.INSTANCE.listPackets(protocol, visitor(UnboundPacketVisitorProxy.CLASS, ids));
        } else {
            Object codec = ProtocolInfoProxy.INSTANCE.codec(protocol);
            List<Object> packets = IdDispatchCodecProxy.INSTANCE.getById(codec);
            for (int id = 0; id < packets.size(); id++) {
                putPacket(ids, IdDispatchCodecProxy.EntryProxy.INSTANCE.getType(packets.get(id)), id);
            }
        }
    }

    private static Object visitor(Class<?> type, Map<String, Integer> ids) {
        return MethodHandleProxies.asInterfaceInstance(type, PacketVisitor.PUT_PACKET.bindTo(ids));
    }

    private static void putPacket(Map<String, Integer> ids, Object packetType, int id) {
        ids.put(PacketTypeProxy.INSTANCE.id(packetType).toString(), id);
    }

    private static final class PacketVisitor {
        private static final MethodHandle PUT_PACKET = putPacketHandle();

        private static MethodHandle putPacketHandle() {
            try {
                return MethodHandles.lookup().findStatic(PacketIdMappings.class, "putPacket",
                        MethodType.methodType(void.class, Map.class, Object.class, int.class));
            } catch (ReflectiveOperationException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }
    }
}
