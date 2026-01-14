package net.momirealms.craftengine.bukkit.compatibility.coreprotect;

import net.coreprotect.listener.player.PlayerInteractEntityListener;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.Location;
import org.bukkit.Material;

public final class CoreProtectHelper {
    private CoreProtectHelper() {}

    public static void logContainerOperation(Player player, WorldPosition position, Object container, boolean isInventory) {
        String name = player.name();
        Location location = LocationUtils.toLocation(position);
        PlayerInteractEntityListener.queueContainerSpecifiedItems(name, isInventory ? Material.STONE : Material.JUKEBOX, container, location, false);
    }
}
