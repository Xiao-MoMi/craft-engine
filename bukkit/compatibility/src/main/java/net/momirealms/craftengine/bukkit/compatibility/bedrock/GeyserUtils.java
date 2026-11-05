package net.momirealms.craftengine.bukkit.compatibility.bedrock;

import org.geysermc.api.Geyser;

import java.util.UUID;

public final class GeyserUtils {
    private GeyserUtils() {}

    public static boolean isGeyserPlayer(UUID uuid) {
        return Geyser.api().isBedrockPlayer(uuid);
    }
}
