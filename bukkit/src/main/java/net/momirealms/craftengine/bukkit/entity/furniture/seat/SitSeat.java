package net.momirealms.craftengine.bukkit.entity.furniture.seat;

import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.core.entity.furniture.AbstractSeat;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.Seat;
import net.momirealms.craftengine.core.entity.furniture.SeatFactory;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.entity.seat.SeatEntity;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.joml.Vector3f;

import java.util.List;

public class SitSeat extends AbstractSeat {
	public static final SeatFactory FACTORY = new Factory();
	private final boolean limitPlayerRotation;

	public SitSeat(Vector3f offset, float yaw, boolean limitPlayerRotation) {
		super(offset, yaw);
		this.limitPlayerRotation = limitPlayerRotation;
	}

	@Override
	public SeatEntity spawn(Player player, Furniture furniture) {
		return spawn((org.bukkit.entity.Player) player.platformPlayer(), furniture);
	}

	public SeatEntity spawn(org.bukkit.entity.Player player, Furniture furniture) {
		Location location = ((BukkitFurniture)furniture).calculateSeatLocation(this);
		org.bukkit.entity.Entity seatEntity = EntityUtils.spawnSeatEntity(furniture, this, player.getWorld(), location, this.limitPlayerRotation, null);
		seatEntity.addPassenger(player);
		return new SitEntity(seatEntity, furniture, offset(), player.getEntityId());
	}

	private static class SitEntity extends BukkitSeatEntity {

		public SitEntity(Entity entity, Furniture furniture, Vector3f vector3f, int playerID) {
			super(entity, furniture, vector3f, playerID);
		}

		@Override
		public Key type() {
			return Key.of("craftengine", "sit") ;
		}
	}

	public static class Factory implements SeatFactory {

		@Override
		public Seat create(List<String> args) {
			if (args.size() == 1) return new SitSeat(MiscUtils.getAsVector3f(args.getFirst(), "seats"), 0, false);
			return new SitSeat(MiscUtils.getAsVector3f(args.getFirst(), "seats"), Float.parseFloat(args.get(1)), true);
		}
	}
}