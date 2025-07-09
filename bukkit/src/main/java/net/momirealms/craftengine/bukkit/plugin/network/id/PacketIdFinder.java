package net.momirealms.craftengine.bukkit.plugin.network.id;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PacketIdFinder {
    private static final Map<String, Map<String, Integer>> gamePacketIdsByName = new HashMap<>();
    private static final Map<String, Map<Class<?>, Integer>> gamePacketIdsByClazz = new HashMap<>();
    private static final int maxC2SPacketId;
    private static final int maxS2CPacketId;

    static {
        try {
            if (VersionHelper.isOrAbove1_21()) {
                Object packetReport = CoreReflections.constructor$PacketReport.newInstance((Object) null);
                JsonElement jsonElement = (JsonElement) CoreReflections.method$PacketReport$serializePackets.invoke(packetReport);
                var play = jsonElement.getAsJsonObject().get("play");
                for (var entry : play.getAsJsonObject().entrySet()) {
                    Map<String, Integer> ids = new HashMap<>();
                    gamePacketIdsByName.put(entry.getKey(), ids);
                    for (var entry2 : entry.getValue().getAsJsonObject().entrySet()) {
                        ids.put(entry2.getKey(), entry2.getValue().getAsJsonObject().get("protocol_id").getAsInt());
                    }
                }
            } else if (VersionHelper.isOrAbove1_20_5()) {
                gamePacketIdsByName.putAll(FastNMS.INSTANCE.gamePacketIdsByName());
            } else {
                gamePacketIdsByClazz.putAll(FastNMS.INSTANCE.gamePacketIdsByClazz());
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to get packets", e);
        }
        maxS2CPacketId = calculateMaxId("clientbound");
        maxC2SPacketId = calculateMaxId("serverbound");
    }
    private static int calculateMaxId(String direction) {
        if (VersionHelper.isOrAbove1_20_5()) {
            return gamePacketIdsByName.getOrDefault(direction, Collections.emptyMap()).size();
        } else {
            return gamePacketIdsByClazz.getOrDefault(direction, Collections.emptyMap()).size();
        }
    }

    public static int maxC2SPacketId() {
        return maxC2SPacketId;
    }

    public static int maxS2CPacketId() {
        return maxS2CPacketId;
    }

    public static int clientboundByName(String packetName) {
        return gamePacketIdsByName.get("clientbound").getOrDefault(packetName, -1);
    }

    public static int clientboundByClazz(Class<?> clazz) {
        return gamePacketIdsByClazz.get("clientbound").getOrDefault(clazz, -1);
    }

    public static int serverboundByName(String packetName) {
        return gamePacketIdsByName.get("serverbound").getOrDefault(packetName, -1);
    }

    public static int serverboundByClazz(Class<?> clazz) {
        return gamePacketIdsByClazz.get("serverbound").getOrDefault(clazz, -1);
    }
}
