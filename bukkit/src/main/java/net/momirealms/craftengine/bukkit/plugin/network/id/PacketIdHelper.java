package net.momirealms.craftengine.bukkit.plugin.network.id;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.plugin.network.ConnectionState;
import net.momirealms.craftengine.core.plugin.network.PacketFlow;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.Map;

public final class PacketIdHelper {
    // 1.20.5-latest
    private static final Map<ConnectionState, Map<PacketFlow, Map<String, Integer>>> byName = VersionHelper.isOrAbove1_20_5 ? FastNMS.INSTANCE.gamePacketIdsByName() : Map.of();
    // 1.20-1.20.4
    private static final Map<ConnectionState, Map<PacketFlow, Map<Class<?>, Integer>>> byClazz = VersionHelper.isOrAbove1_20_5 ? Map.of() : FastNMS.INSTANCE.gamePacketIdsByClazz();

    private PacketIdHelper() {}

    public static int count(PacketFlow direction, ConnectionState state) {
        if (VersionHelper.isOrAbove1_20_5) {
            return byName.getOrDefault(state, Map.of()).getOrDefault(direction, Map.of()).size();
        } else {
            return byClazz.getOrDefault(state, Map.of()).getOrDefault(direction, Map.of()).size();
        }
    }

    public static int byName(String packetName, ConnectionState state, PacketFlow direction) {
        return byName.getOrDefault(state, Map.of()).getOrDefault(direction, Map.of()).getOrDefault(packetName, -1);
    }

    public static int byClazz(Class<?> clazz, ConnectionState state, PacketFlow direction) {
        if (clazz == null) {
            return -1;
        }
        return byClazz.getOrDefault(state, Map.of()).getOrDefault(direction, Map.of()).getOrDefault(clazz, -1);
    }
}
