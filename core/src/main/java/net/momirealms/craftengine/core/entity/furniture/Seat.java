package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.entity.seat.SeatEntity;
import org.joml.Vector3f;

public interface Seat {

    SeatEntity spawn(Player player, Furniture furniture);

    Vector3f offset();

    float yaw();
}
