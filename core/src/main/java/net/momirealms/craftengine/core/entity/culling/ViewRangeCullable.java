package net.momirealms.craftengine.core.entity.culling;

import net.momirealms.craftengine.core.entity.player.Player;

public interface ViewRangeCullable {

    void setCulled(Player player, boolean culled);
}
