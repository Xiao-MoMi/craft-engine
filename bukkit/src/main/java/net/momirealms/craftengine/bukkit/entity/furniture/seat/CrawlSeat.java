package net.momirealms.craftengine.bukkit.entity.furniture.seat;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.entity.data.PlayerData;
import net.momirealms.craftengine.bukkit.entity.data.ShulkerData;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MAttributeHolders;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.util.PlayerUtils;
import net.momirealms.craftengine.core.entity.furniture.AbstractSeat;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.Seat;
import net.momirealms.craftengine.core.entity.furniture.SeatFactory;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.entity.seat.SeatEntity;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pose;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class CrawlSeat extends AbstractSeat {
	public static final SeatFactory FACTORY = new Factory();
	private static final List<Object> visualData = new ArrayList<>();
	private final boolean limitPlayerRotation;

	static {
		ShulkerData.NoGravity.addEntityDataIfNotDefaultValue(true, visualData);
		ShulkerData.Silent.addEntityDataIfNotDefaultValue(true, visualData);
		ShulkerData.MobFlags.addEntityDataIfNotDefaultValue((byte) 0x01, visualData);
		ShulkerData.SharedFlags.addEntityDataIfNotDefaultValue((byte) 0x20, visualData);
	}

	public CrawlSeat(Vector3f offset, float yaw, boolean limitPlayerRotation) {
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

		// Fix Rider Pose
		int visualId = CoreReflections.instance$Entity$ENTITY_COUNTER.incrementAndGet();
		List<Object> packets = new ArrayList<>();
		packets.add(FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(visualId, UUID.randomUUID(), location.getX(), location.getY(), location.getZ(), location.getPitch(), location.getYaw(),
				MEntityTypes.instance$EntityType$SHULKER, 0, CoreReflections.instance$Vec3$Zero, 0));
		packets.add(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(visualId, List.copyOf(visualData)));

		try {
			if (VersionHelper.isOrAbove1_20_5()) {
				Object attributeInstance = CoreReflections.constructor$AttributeInstance.newInstance(MAttributeHolders.SCALE, (Consumer<?>) (o) -> {});
				CoreReflections.method$AttributeInstance$setBaseValue.invoke(attributeInstance, 0.6);
				packets.add(
						NetworkReflections.constructor$ClientboundUpdateAttributesPacket0
								.newInstance(visualId, Collections.singletonList(attributeInstance))
				);
				packets.add(FastNMS.INSTANCE.constructor$ClientboundSetPassengersPacket(seatEntity.getEntityId(), visualId));
			}
		} catch (Exception e) {
			CraftEngine.instance().logger().warn("Failed to add crawl seat attributes", e);
		}

		Object bundle = FastNMS.INSTANCE.constructor$ClientboundBundlePacket(packets);
		BukkitServerPlayer serverPlayer = BukkitAdaptors.adapt(player);
		serverPlayer.sendPacket(bundle, true);

		// Sync Pose
		player.setPose(Pose.SWIMMING, true);
		Object syncPosePacket = null;
		try {
			Object playerData = CoreReflections.method$Entity$getEntityData.invoke(serverPlayer.serverPlayer());
			Object dataItem = CoreReflections.method$SynchedEntityData$getItem.invoke(playerData, PlayerData.Pose.entityDataAccessor());
			Object dataValue = CoreReflections.method$SynchedEntityData$DataItem$value.invoke(dataItem);
			syncPosePacket = FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(serverPlayer.entityID(), List.of(dataValue));
		} catch (Exception e) {
			CraftEngine.instance().logger().warn("Failed to construct sync pose packet", e);
		}

		Object finalSyncPosePacket = syncPosePacket;
		BukkitCraftEngine.instance().scheduler().sync().runLater(() -> {
			serverPlayer.sendPacket(finalSyncPosePacket, true);
			for (org.bukkit.entity.Player p : PlayerUtils.getTrackedBy(player)) {
				BukkitNetworkManager.instance().sendPacket(BukkitAdaptors.adapt(p), finalSyncPosePacket, true);
			}
		}, 1, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4);

		return new CrawlEntity(seatEntity, furniture, offset(), player.getEntityId(), visualId, syncPosePacket);
	}

	private static class CrawlEntity extends BukkitSeatEntity {
		private final int visualId;
		private final Object syncPosePacket;


		public CrawlEntity(Entity entity, Furniture furniture, Vector3f vector3f, int playerID, int visualId, Object fixPosePacket) {
			super(entity, furniture, vector3f, playerID);
			this.visualId = visualId;
			this.syncPosePacket = fixPosePacket;
		}

		@Override
		public void add(NetWorkUser from, NetWorkUser to) {
			to.sendPacket(syncPosePacket, false);
		}

		@Override
		public void dismount(Player player) {
			super.dismount(player);
			((org.bukkit.entity.Player) player.platformPlayer()).setPose(Pose.STANDING, false);
			try {
				Object packet = NetworkReflections.constructor$ClientboundRemoveEntitiesPacket.newInstance((Object) new int[]{visualId});
				player.sendPacket(packet, false);
			} catch (Exception e) {
				BukkitCraftEngine.instance().logger().warn("Failed to remove crawl entity", e);
			}
		}

		@Override
		public Key type() {
			return Key.of("craftengine", "crawl");
		}
	}

	public static class Factory implements SeatFactory {

		@Override
		public Seat create(List<String> args) {
			if (args.size() == 1) return new CrawlSeat(MiscUtils.getAsVector3f(args.getFirst(), "seats"), 0, false);
			return new CrawlSeat(MiscUtils.getAsVector3f(args.getFirst(), "seats"), Float.parseFloat(args.get(1)), true);
		}
	}
}