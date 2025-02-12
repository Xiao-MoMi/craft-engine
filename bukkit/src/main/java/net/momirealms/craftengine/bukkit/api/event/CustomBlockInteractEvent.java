package net.momirealms.craftengine.bukkit.api.event;

import net.momirealms.craftengine.core.block.ImmutableBlockState;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class CustomBlockInteractEvent extends CustomBlockEvent {
    public CustomBlockInteractEvent(ImmutableBlockState state, Location location, Player player) {
        super(state, location, player);
    }
}
