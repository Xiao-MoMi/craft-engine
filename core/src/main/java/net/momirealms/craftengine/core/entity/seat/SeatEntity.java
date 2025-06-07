package net.momirealms.craftengine.core.entity.seat;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import org.joml.Vector3f;

public interface SeatEntity extends EntityPacketHandler {

	void add(NetWorkUser from, NetWorkUser to);

	void dismount(Player player);

	void event(Player player, Object event);

	void destroy();

	Furniture furniture();

	Vector3f vector3f();

	int playerID();
}
