package net.momirealms.craftengine.bukkit.compatibility.bedrock;

import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public final class FloodgateUtils {
    private FloodgateUtils() {}

    public static boolean isFloodgatePlayer(UUID uuid) {
        return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
    }
}
