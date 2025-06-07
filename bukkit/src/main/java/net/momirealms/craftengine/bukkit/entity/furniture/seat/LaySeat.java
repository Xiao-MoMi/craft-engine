package net.momirealms.craftengine.bukkit.entity.furniture.seat;

import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.entity.data.LivingEntityData;
import net.momirealms.craftengine.bukkit.entity.data.PlayerData;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MItems;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.bukkit.plugin.scheduler.impl.FoliaTask;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.entity.furniture.AbstractSeat;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.Seat;
import net.momirealms.craftengine.core.entity.furniture.SeatFactory;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.entity.seat.SeatEntity;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.network.NMSPacketEvent;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.BlockPos;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.joml.Vector3f;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class LaySeat extends AbstractSeat {
	public static final SeatFactory FACTORY = new Factory();
	private final Direction facing;
	private final boolean sleep;
	private final boolean phantom;

	public LaySeat(Vector3f offset, Direction facing, boolean sleep, boolean phantom) {
		super(offset, 0);
		this.facing = facing;
		this.sleep = sleep;
		this.phantom = phantom;
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public SeatEntity spawn(Player cePlayer, Furniture furniture) {
		Location loc = ((BukkitFurniture)furniture).calculateSeatLocation(this);

		org.bukkit.entity.Player player = (org.bukkit.entity.Player) cePlayer.platformPlayer();
		Object serverPlayer = cePlayer.serverPlayer();

		// Pose offset nearly same as vanilla
		AttributeInstance attribute = VersionHelper.isOrAbove1_21_2() ? player.getAttribute(Attribute.SCALE) : null;
		double scale = attribute == null ? 1 : attribute.getValue();
		loc.add(0, 0.08525 * scale, 0);

		try {
			List<Object> packets = new ArrayList<>();
			// NPC
			Object server = CoreReflections.method$MinecraftServer$getServer.invoke(null);
			Object level = FastNMS.INSTANCE.field$CraftWorld$ServerLevel(player.getWorld());
			UUID uuid = UUID.randomUUID();
			Object npcProfile = CoreReflections.constructor$GameProfile.newInstance(uuid, player.getName());
			Object playerProfile = CoreReflections.method$ServerPlayer$getGameProfile.invoke(serverPlayer);

			Multimap<String, Object> properties = (Multimap<String, Object>) CoreReflections.method$GameProfile$getProperties.invoke(npcProfile);
			properties.putAll((Multimap<String, Object>) CoreReflections.method$GameProfile$getProperties.invoke(playerProfile));

			Object npc;
			if (VersionHelper.isOrAbove1_20_2()) {
				Object clientInfo = CoreReflections.method$ServerPlayer$clientInformation.invoke(serverPlayer);
				npc = CoreReflections.constructor$ServerPlayer.newInstance(server, level, npcProfile, clientInfo);
			} else {
				npc = CoreReflections.constructor$ServerPlayer.newInstance(server, level, npcProfile);
			}
			int npcId = FastNMS.INSTANCE.method$Entity$getId(npc);
			CoreReflections.method$Entity$absSnapTo.invoke(npc, loc.getX(), loc.getY(), loc.getZ(), 0, 0);
			Object npcSpawnPacket = FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(npcId, uuid,
					loc.getX(), loc.getY(), loc.getZ(), 0, 0,
					MEntityTypes.instance$EntityType$PLAYER, 0, CoreReflections.instance$Vec3$Zero, 0);

			// Info
			EnumSet enumSet = EnumSet.noneOf((Class<? extends Enum>) NetworkReflections.clazz$ClientboundPlayerInfoUpdatePacket$Action);
			enumSet.add(NetworkReflections.instance$ClientboundPlayerInfoUpdatePacket$Action$ADD_PLAYER);
			Object entry;
			if (VersionHelper.isOrAbove1_21_4()) {
				entry = NetworkReflections.constructor$ClientBoundPlayerInfoUpdatePacket$Entry.newInstance(
						uuid, npcProfile, false, 0, CoreReflections.instance$GameType$SURVIVAL, null, true, 0, null);
			} else if (VersionHelper.isOrAbove1_21_3()) {
				entry = NetworkReflections.constructor$ClientBoundPlayerInfoUpdatePacket$Entry.newInstance(
						uuid, npcProfile, false, 0, CoreReflections.instance$GameType$SURVIVAL, null, 0, null);
			} else {
				entry = NetworkReflections.constructor$ClientBoundPlayerInfoUpdatePacket$Entry.newInstance(
						uuid, npcProfile, false, 0, CoreReflections.instance$GameType$SURVIVAL, null, null);
			}
			Object npcInfoPacket = FastNMS.INSTANCE.constructor$ClientboundPlayerInfoUpdatePacket(enumSet, Collections.singletonList(entry));

			// Bed
			Direction bedDir = Direction.fromYaw(loc.getYaw() + Direction.getYaw(facing));
			if (bedDir == Direction.EAST || bedDir == Direction.WEST) bedDir = bedDir.opposite();
			BlockData bedData = Material.WHITE_BED.createBlockData("[facing=" + bedDir.name().toLowerCase() + ",part=head]");
			Location bedLoc = loc.clone();
			bedLoc.setY(bedLoc.getWorld().getMinHeight());
			Object bedPos = LocationUtils.toBlockPos(new BlockPos(bedLoc.getBlockX(), bedLoc.getBlockY(), bedLoc.getBlockZ()));
			Object blockState = BlockStateUtils.blockDataToBlockState(bedData);
			Object bedPacket = NetworkReflections.constructor$ClientboundBlockUpdatePacket.newInstance(bedPos, blockState);

			// Data
			Object npcData = CoreReflections.method$Entity$getEntityData.invoke(npc);
			Object playerData = CoreReflections.method$Entity$getEntityData.invoke(serverPlayer);
			CoreReflections.method$Entity$setInvisible.invoke(serverPlayer, true);
			CoreReflections.method$SynchedEntityData$set.invoke(npcData, PlayerData.Pose.entityDataAccessor(), CoreReflections.instance$Pose$SLEEPING);
			CoreReflections.method$SynchedEntityData$set.invoke(npcData, LivingEntityData.SleepingPos.entityDataAccessor(), Optional.of(bedPos));
			CoreReflections.method$SynchedEntityData$set.invoke(npcData, PlayerData.Skin.entityDataAccessor(), CoreReflections.method$SynchedEntityData$get.invoke(playerData, PlayerData.Skin.entityDataAccessor()));
			CoreReflections.method$SynchedEntityData$set.invoke(npcData, PlayerData.Hand.entityDataAccessor(), CoreReflections.method$SynchedEntityData$get.invoke(playerData, PlayerData.Hand.entityDataAccessor()));
			CoreReflections.method$SynchedEntityData$set.invoke(npcData, PlayerData.LShoulder.entityDataAccessor(), CoreReflections.method$SynchedEntityData$get.invoke(playerData, PlayerData.LShoulder.entityDataAccessor()));
			CoreReflections.method$SynchedEntityData$set.invoke(npcData, PlayerData.RShoulder.entityDataAccessor(), CoreReflections.method$SynchedEntityData$get.invoke(playerData, PlayerData.RShoulder.entityDataAccessor()));
			CoreReflections.method$SynchedEntityData$set.invoke(playerData, PlayerData.LShoulder.entityDataAccessor(), CoreReflections.instance$CompoundTag$Empty);
			CoreReflections.method$SynchedEntityData$set.invoke(playerData, PlayerData.RShoulder.entityDataAccessor(), CoreReflections.instance$CompoundTag$Empty);

			// SetData
			CoreReflections.method$Entity$setInvisible.invoke(serverPlayer, true);
			Object npcDataPacket = FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(
					npcId, (List) CoreReflections.method$SynchedEntityData$packDirty.invoke(npcData)
			);

			// Remove
			Object npcRemovePacket = NetworkReflections.constructor$ClientboundRemoveEntitiesPacket.newInstance((Object) new int[]{npcId});

			// TP
			Object npcTeleportPacket;
			if (VersionHelper.isOrAbove1_21_3()) {
				Object positionMoveRotation = CoreReflections.method$PositionMoveRotation$of.invoke(null, npc);
				npcTeleportPacket = NetworkReflections.constructor$ClientboundTeleportEntityPacket.newInstance(npcId, positionMoveRotation, Set.of(), false);
			} else {
				npcTeleportPacket = NetworkReflections.constructor$ClientboundTeleportEntityPacket.newInstance(npc);
			}


			// Equipment
			List<Pair<Object, Object>> emptySlots = new ArrayList<>();

			emptySlots.add(Pair.of(CoreReflections.instance$EquipmentSlot$MAINHAND, MItems.Air$ItemStack));
			emptySlots.add(Pair.of(CoreReflections.instance$EquipmentSlot$OFFHAND, MItems.Air$ItemStack));
			emptySlots.add(Pair.of(CoreReflections.instance$EquipmentSlot$HEAD, MItems.Air$ItemStack));
			emptySlots.add(Pair.of(CoreReflections.instance$EquipmentSlot$CHEST, MItems.Air$ItemStack));
			emptySlots.add(Pair.of(CoreReflections.instance$EquipmentSlot$LEGS, MItems.Air$ItemStack));
			emptySlots.add(Pair.of(CoreReflections.instance$EquipmentSlot$FEET, MItems.Air$ItemStack));
			Object emptyEquipPacket = NetworkReflections.constructor$ClientboundSetEquipmentPacket.newInstance(player.getEntityId(), emptySlots);

			Map<EquipmentSlot, ItemStack> equipments = new HashMap<>();
			EntityEquipment equipment = player.getEquipment();
			for (org.bukkit.inventory.EquipmentSlot slot : org.bukkit.inventory.EquipmentSlot.values()) {
				if ((!slot.isHand() && !slot.isArmor())
						|| (VersionHelper.isOrAbove1_20_5() && slot == org.bukkit.inventory.EquipmentSlot.BODY)) {
					continue;
				}
				EquipmentSlot slotId = EntityUtils.toCEEquipmentSlot(slot);
				ItemStack item = equipment.getItem(slot);
				equipments.put(slotId, item);
			}
			List<Pair<Object, Object>> npcSlots = new ArrayList<>();
			equipments.forEach((slot, item) -> npcSlots.add(Pair.of(EntityUtils.fromEquipmentSlot(slot), FastNMS.INSTANCE.method$CraftItemStack$asNMSCopy(item))));
			Object fullEquipPacket = NetworkReflections.constructor$ClientboundSetEquipmentPacket.newInstance(npcId, npcSlots);


			packets.add(npcInfoPacket);
			packets.add(npcSpawnPacket);
			packets.add(bedPacket);
			packets.add(npcDataPacket);
			packets.add(npcTeleportPacket);
			packets.add(emptyEquipPacket);
			Object npcInitPackets = FastNMS.INSTANCE.constructor$ClientboundBundlePacket(packets);

			// Spawn
			org.bukkit.entity.Entity seatEntity = EntityUtils.spawnSeatEntity(furniture, this, player.getWorld(), loc, false, null);
			seatEntity.addPassenger(player); // 0.5 higher
			cePlayer.sendPacket(npcInitPackets, true);
			cePlayer.sendPacket(fullEquipPacket, true);
			if (player.getY() > 0) {
				BukkitCraftEngine.instance().scheduler().asyncLater(() -> cePlayer.sendPacket(npcTeleportPacket, true),
						50, TimeUnit.MILLISECONDS); // over height 0 cost 2 npcTeleportPacket
			}

			for (org.bukkit.entity.Player p : PlayerUtils.getTrackedBy(player)) {
				BukkitNetworkManager.instance().getOnlineUser(p).sendPacket(npcInitPackets, false);
				BukkitNetworkManager.instance().getOnlineUser(p).sendPacket(fullEquipPacket, false);
				if (player.getY() > 0) {
					BukkitCraftEngine.instance().scheduler().asyncLater(() -> BukkitNetworkManager.instance().getOnlineUser(p).sendPacket(npcTeleportPacket, false),
							50, TimeUnit.MILLISECONDS);
				}
			}

			// HeadRot
			Direction npcDir = bedDir.opposite();
			float npcYawOffset = 0.0f;
			if (player.getY() > 0.0874218749) {
				if (VersionHelper.isOrAbove1_21_2()) {
					double offset = loc.x() - Math.floor(loc.x()) - 0.5;

					if (Math.abs(offset) > 0.05005) {
						npcYawOffset = offset > 0 ? +27f : -26f;
					}
				}
			}

			if (sleep) {
				player.setSleepingIgnored(true);
			}

			if (phantom) {
				player.setStatistic(Statistic.TIME_SINCE_REST, 0);
			}

			return new LayEntity(
					seatEntity,
					furniture,
					this.offset(),
					npcInitPackets,
					npcRemovePacket,
					npcTeleportPacket,
					(BukkitServerPlayer) cePlayer,
					bedLoc,
					npc,
					npcId,
					npcDir,
					npcYawOffset,
					equipments,
					emptyEquipPacket,
					fullEquipPacket,
					sleep
			);
		} catch (Exception e) {
			CraftEngine.instance().logger().warn("Failed to spawn LaySeat", e);
		}
		return null;
	}

	private static class LayEntity extends BukkitSeatEntity {
		private final Object npcInitPackets;
		private final Object npcRemovePacket;
		private final Object npcTPPacket;
		private final BukkitServerPlayer serverPlayer;
		private final Object npc;
		private final Location bedLoc;
		private final int npcID;
		private final Direction npcDir;
		private final float npcYawOffset;

		// Equipment
		private final PlayerMonitorTask task;
		private final Map<EquipmentSlot, ItemStack> equipments;
		private final Object emptyEquipPacket;
		private Object updateEquipPacket;
		private Object fullEquipPacket;

		private final boolean sleep;
		private Object npcRotHeadPacket;
		private Object npcDataPacket;

		public LayEntity(
				org.bukkit.entity.Entity entity,
				Furniture furniture,
				Vector3f vector,
				Object npcInitPackets,
				Object npcRemovePacket,
				Object npcTPPacket,
				BukkitServerPlayer serverPlayer,
				Location bedLoc,
				Object npc,
				int npcID,
				Direction npcDir,
				float npcYawOffset,
				Map<EquipmentSlot, ItemStack> equipments,
				Object emptyEquipPacket,
				Object fullEquipPacket,
				boolean sleep
		) {
			super(entity, furniture, vector, serverPlayer.entityID());
			this.npcInitPackets = npcInitPackets;
			this.npcRemovePacket = npcRemovePacket;
			this.npcTPPacket = npcTPPacket;
			this.serverPlayer = serverPlayer;
			this.bedLoc = bedLoc;
			this.npc = npc;
			this.npcID = npcID;
			this.npcDir = npcDir;
			this.npcYawOffset = npcYawOffset;

			this.task = new PlayerMonitorTask();
			this.equipments = equipments;
			this.emptyEquipPacket = emptyEquipPacket;
			this.fullEquipPacket = fullEquipPacket;

			this.sleep = sleep;
			updateNpcYaw(serverPlayer.xRot());
			updateNpcInvisible();
		}

		@Override
		public void add(NetWorkUser from, NetWorkUser to) {
			to.sendPacket(this.npcInitPackets, false);
			to.sendPacket(this.fullEquipPacket, false);
			if (serverPlayer.y() > 0) {
				BukkitCraftEngine.instance().scheduler().asyncLater(() -> {
					to.sendPacket(this.npcTPPacket, false);
					to.sendPacket(this.npcRotHeadPacket, false);
					if (npcDataPacket != null) to.sendPacket(this.npcDataPacket, false);
				}, 50, TimeUnit.MILLISECONDS);
			} else {
				to.sendPacket(this.npcRotHeadPacket, false);
				if (npcDataPacket != null) to.sendPacket(this.npcDataPacket, false);
			}
		}

		@Override
		public boolean handleEntitiesRemove(NetWorkUser user, IntList entityIds) {
			entityIds.add(npcID);
			return true;
		}

		@Override
		public void dismount(Player from) {
			super.dismount(from);
			this.task.task.cancel();
			org.bukkit.entity.Player player = (org.bukkit.entity.Player) from.platformPlayer();
			Object blockPos = LocationUtils.toBlockPos(bedLoc.getBlockX(), bedLoc.getBlockY(), bedLoc.getBlockZ());
			Object blockState = BlockStateUtils.blockDataToBlockState(bedLoc.getBlock().getBlockData());
			try {
				Object blockUpdatePacket = NetworkReflections.constructor$ClientboundBlockUpdatePacket.newInstance(blockPos, blockState);
				if (player.getPotionEffect(PotionEffectType.INVISIBILITY) == null) CoreReflections.method$Entity$setInvisible.invoke(serverPlayer.serverPlayer(), false);
				from.sendPacket(this.npcRemovePacket, true);
				from.sendPacket(blockUpdatePacket, true);

				Object npcData = CoreReflections.method$Entity$getEntityData.invoke(npc);
				Object playerData = CoreReflections.method$Entity$getEntityData.invoke(serverPlayer.serverPlayer());
				CoreReflections.method$SynchedEntityData$set.invoke(playerData, PlayerData.LShoulder.entityDataAccessor(), CoreReflections.method$SynchedEntityData$get.invoke(npcData, PlayerData.LShoulder.entityDataAccessor()));
				CoreReflections.method$SynchedEntityData$set.invoke(playerData, PlayerData.RShoulder.entityDataAccessor(), CoreReflections.method$SynchedEntityData$get.invoke(npcData, PlayerData.RShoulder.entityDataAccessor()));
				if (player.getPotionEffect(PotionEffectType.INVISIBILITY) == null) CoreReflections.method$Entity$setInvisible.invoke(serverPlayer.serverPlayer(), false);

				player.updateInventory();

				if (sleep) {
					player.setSleepingIgnored(false);
				}

				Object fullSlots = NetworkReflections.method$ClientboundSetEquipmentPacket$getSlots.invoke(this.fullEquipPacket);
				Object recoverEquip = NetworkReflections.constructor$ClientboundSetEquipmentPacket.newInstance(player.getEntityId(), fullSlots);

				for (org.bukkit.entity.Player p : PlayerUtils.getTrackedBy(player)) {
					BukkitServerPlayer sp = BukkitAdaptors.adapt(p);
					sp.entityPacketHandlers().remove(playerID());
					sp.sendPacket(this.npcRemovePacket, false);
					sp.sendPacket(blockUpdatePacket, false);
					sp.sendPacket(recoverEquip, false);
				}
			} catch (Exception e) {
				CraftEngine.instance().logger().warn("Failed to dismount LayEntity", e);
			}
		}

		public void equipmentChange(Map<EquipmentSlot, ItemStack> equipmentChanges, int previousSlot) {
			org.bukkit.entity.Player player = serverPlayer.platformPlayer();
			List<Pair<Object,Object>> changedSlots = new ArrayList<>();

			for (Map.Entry<EquipmentSlot, ItemStack> entry : equipmentChanges.entrySet()) {
				Object slotId = EntityUtils.fromEquipmentSlot(entry.getKey());
				Object itemStack = FastNMS.INSTANCE.method$CraftItemStack$asNMSCopy(entry.getValue());
				changedSlots.add(Pair.of(slotId, itemStack));
			}
			this.equipments.putAll(equipmentChanges);

			List<Pair<Object,Object>> allSlots = new ArrayList<>();
			equipments.forEach((slot, item) ->
					allSlots.add(Pair.of(EntityUtils.fromEquipmentSlot(slot), FastNMS.INSTANCE.method$CraftItemStack$asNMSCopy(item))));
			try {
				this.updateEquipPacket = NetworkReflections.constructor$ClientboundSetEquipmentPacket.newInstance(npcID, changedSlots);
				this.fullEquipPacket = NetworkReflections.constructor$ClientboundSetEquipmentPacket.newInstance(npcID, allSlots);
				if (previousSlot != -1) {
					player.updateInventory();
				}
			} catch (Exception e) {
				CraftEngine.instance().logger().warn("Failed to handle equipmentChange", e);
			}

			serverPlayer.sendPacket(this.emptyEquipPacket, false);
			serverPlayer.sendPacket(this.updateEquipPacket, false);

			for (org.bukkit.entity.Player p : PlayerUtils.getTrackedBy(player)) {
				BukkitNetworkManager.instance().getOnlineUser(p).sendPacket(this.updateEquipPacket, false);
			}
		}

		@Override
		public void handleSetEquipment(NetWorkUser user, NMSPacketEvent event, Object packet) {
			if (this.emptyEquipPacket == packet) return;
			event.setCancelled(true);
		}

		@Override
		public void handleContainerSetSlot(NetWorkUser user, NMSPacketEvent event, Object packet) {
			try {
				int slot = (int) NetworkReflections.method$ClientboundContainerSetSlotPacket$getSlot.invoke(packet);
				org.bukkit.entity.Player player = (org.bukkit.entity.Player) user.platformPlayer();

				int convertSlot;
				boolean isPlayerInv;

				if (!VersionHelper.isOrAbove1_21_1()) {
					Object openInventory = player.getClass().getMethod("getOpenInventory").invoke(player);

					Method convertSlotMethod = openInventory.getClass().getMethod("convertSlot", int.class);
					convertSlot = (int) convertSlotMethod.invoke(openInventory, slot);

					Method getTopInventoryMethod = openInventory.getClass().getMethod("getTopInventory");
					Object topInventory = getTopInventoryMethod.invoke(openInventory);
					Method getTypeMethod = topInventory.getClass().getMethod("getType");
					Object type = getTypeMethod.invoke(topInventory);
					isPlayerInv = type == InventoryType.CRAFTING;
				} else {
					convertSlot = player.getOpenInventory().convertSlot(slot);
					isPlayerInv = player.getOpenInventory().getTopInventory().getType() == InventoryType.CRAFTING;
				}

				if (!(convertSlot == player.getInventory().getHeldItemSlot() || (isPlayerInv && (slot == 45 || (slot >= 5 && slot <= 8))))) return;
				int containerId = (int) NetworkReflections.method$ClientboundContainerSetSlotPacket$getContainerId.invoke(packet);
				int stateId = (int) NetworkReflections.method$ClientboundContainerSetSlotPacket$getStateId.invoke(packet);
				Object replacePacket = NetworkReflections.constructor$ClientboundContainerSetSlotPacket.newInstance(containerId, stateId, slot, MItems.Air$ItemStack);
				event.replacePacket(replacePacket);
			} catch (Exception e) {
				CraftEngine.instance().logger().warn("Failed to handleContainerSetSlotPacket", e);
			}
		}

		@Override
		public void event(Player player, Object event) {
			if (event instanceof PlayerAnimationEvent e) {
				try {
					Object animatePacket;
					if (e.getAnimationType() == PlayerAnimationType.ARM_SWING) {
						animatePacket = NetworkReflections.constructor$ClientboundAnimatePacket.newInstance(npc, 0);
					} else {
						animatePacket = NetworkReflections.constructor$ClientboundAnimatePacket.newInstance(npc, 3);
					}
					serverPlayer.sendPacket(animatePacket, true);
					for (org.bukkit.entity.Player other : PlayerUtils.getTrackedBy(serverPlayer.platformPlayer())) {
						BukkitNetworkManager.instance().getOnlineUser(other).sendPacket(animatePacket, true);
					}
				} catch (Exception exception) {
					CraftEngine.instance().logger().warn("Failed to handle PlayerAnimationEvent", exception);
				}
			} else if (event instanceof PlayerItemHeldEvent e) {
				ItemStack item = e.getPlayer().getInventory().getItem(e.getNewSlot());
				if (item == null) item = ItemUtils.AIR;

				equipmentChange(Map.of(EquipmentSlot.MAIN_HAND, item), e.getPreviousSlot());
			}
		}

		@Override
		public Key type() {
			return Key.of("craftengine", "lay");
		}

		private class PlayerMonitorTask implements Runnable {

			private final SchedulerTask task;
			private float lastYaw;

			private PlayerMonitorTask() {
				org.bukkit.entity.Player p = serverPlayer.platformPlayer();
				BukkitCraftEngine plugin = BukkitCraftEngine.instance();
				if (VersionHelper.isFolia()) {
					this.task = new FoliaTask(p.getScheduler().runAtFixedRate(plugin.javaPlugin(), (t) -> this.run(), () -> {}, 1, 1));
				} else {
					this.task = plugin.scheduler().sync().runRepeating(this, 1, 1);
				}
			}

			@Override
			public void run() {
				org.bukkit.entity.Player player = serverPlayer.platformPlayer();
				if (player == null || !player.isValid()) {
					this.task.cancel();
					return;
				}

				// Invisible
				updateNpcInvisible();
				try {
					if (!player.isInvisible()) CoreReflections.method$Entity$setInvisible.invoke(serverPlayer.serverPlayer(), true);
				} catch (Exception exception) {
					CraftEngine.instance().logger().warn("Failed to set shared flag", exception);
				}

				// Sync Rotation
				float playerYaw = player.getYaw();
				if (lastYaw != playerYaw) {
					updateNpcYaw(playerYaw);
					serverPlayer.sendPacket(npcRotHeadPacket, false);
					for (org.bukkit.entity.Player other : PlayerUtils.getTrackedBy(player)) {
						BukkitNetworkManager.instance().getOnlineUser(other).sendPacket(npcRotHeadPacket, true);
					}
					this.lastYaw = playerYaw;
				}

				// Sync Equipment
				Map<EquipmentSlot, ItemStack> newEquipments = new HashMap<>();
				for (EquipmentSlot slot : EquipmentSlot.values()) {
					if (!slot.isHand() && !slot.isPlayerArmor()) continue;
					ItemStack newItem = player.getEquipment().getItem(EntityUtils.toBukkitEquipmentSlot(slot));
					try {
						ItemStack item = equipments.get(slot);
						boolean isChange = !newItem.equals(item);
						if (isChange) {
							newEquipments.put(slot, newItem);
						}
					} catch (Exception e) {
						CraftEngine.instance().logger().warn("Failed to monitor equipments change", e);
					}
				}

				if (!newEquipments.isEmpty()) {
					equipmentChange(newEquipments, -1);
					return;
				}
				serverPlayer.sendPacket(emptyEquipPacket, false);
			}
		}

		private void updateNpcYaw(float playerYaw) {
			byte packYaw = getRot(playerYaw);
			try {
				this.npcRotHeadPacket = NetworkReflections.constructor$ClientboundRotateHeadPacket.newInstance(npc, packYaw);
			} catch (Exception exception) {
				CraftEngine.instance().logger().warn("Failed to sync NPC yaw", exception);
			}
		}

		private byte getRot(float playerYaw) {
			float npcYaw = Direction.getYaw(npcDir);
			float centerYaw = normalizeYaw(npcYaw);
			float playerYawNorm = normalizeYaw(playerYaw);

			float deltaYaw = normalizeYaw(playerYawNorm - centerYaw);
			boolean isBehind = Math.abs(deltaYaw) > 90;

			float mappedYaw;
			if (isBehind) {
				float rel = Math.abs(deltaYaw) - 180;
				mappedYaw = rel * (deltaYaw > 0 ? -1 : 1);
			} else {
				mappedYaw = deltaYaw;
			}

			float limitedYaw = Math.max(-45, Math.min(45, mappedYaw));
			float finalYaw = limitedYaw + npcYawOffset;
			return MCUtils.packDegrees(finalYaw);
		}

		private float normalizeYaw(float yaw) {
			yaw %= 360.0f;
			if (yaw < -180.0f) yaw += 360.0f;
			if (yaw >= 180.0f) yaw -= 360.0f;
			return yaw;
		}

		private void updateNpcInvisible() {
			try {
				org.bukkit.entity.Player player = serverPlayer.platformPlayer();
				if (player.getPotionEffect(PotionEffectType.INVISIBILITY) == null && npcDataPacket != null) {
					npcDataPacket = null;
					CoreReflections.method$Entity$setInvisible.invoke(npc, false);
					Object npcData = CoreReflections.method$Entity$getEntityData.invoke(npc);
					Object dataItem = CoreReflections.method$SynchedEntityData$getItem.invoke(npcData, PlayerData.SharedFlags.entityDataAccessor());
					Object dataValue = CoreReflections.method$SynchedEntityData$DataItem$value.invoke(dataItem);
					Object packet = FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(npcID, List.of(dataValue));
					serverPlayer.sendPacket(packet, false);
					for (org.bukkit.entity.Player p : PlayerUtils.getTrackedBy(player)) {
						BukkitNetworkManager.instance().getOnlineUser(p).sendPacket(packet, false);
					}
				} else if (player.getPotionEffect(PotionEffectType.INVISIBILITY) != null && npcDataPacket == null) {
					CoreReflections.method$Entity$setInvisible.invoke(npc, true);
					Object npcData = CoreReflections.method$Entity$getEntityData.invoke(npc);
					Object dataItem = CoreReflections.method$SynchedEntityData$getItem.invoke(npcData, PlayerData.SharedFlags.entityDataAccessor());
					Object dataValue = CoreReflections.method$SynchedEntityData$DataItem$value.invoke(dataItem);
					npcDataPacket = FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(npcID, List.of(dataValue));
					serverPlayer.sendPacket(npcDataPacket, false);
					for (org.bukkit.entity.Player p : PlayerUtils.getTrackedBy(player)) {
						BukkitNetworkManager.instance().getOnlineUser(p).sendPacket(npcDataPacket, false);
					}
				}
			} catch (Exception e) {
				CraftEngine.instance().logger().warn("Failed to updateNpcInvisible", e);
			}
		}
	}

	public static class Factory implements SeatFactory {

		@Override
		public Seat create(List<String> args) {
			Vector3f offset = MiscUtils.getAsVector3f(args.get(0), "seats");
			Direction facing = args.size() > 1 ? parseFacing(args.get(1)) : Direction.SOUTH;
			boolean sleep = args.size() > 2 && Boolean.parseBoolean(args.get(2));
			boolean phantom = args.size() > 4 && Boolean.parseBoolean(args.get(3));

			if (facing == Direction.NORTH || facing == Direction.SOUTH) {
				float temp = offset.x;
				offset.x = offset.z;
				offset.z = temp;
			}
			return new LaySeat(offset, facing, sleep, phantom);
		}

		private Direction parseFacing(String facing) {
			return switch (facing.toLowerCase()) {
				case "back" -> Direction.NORTH;
				case "left" -> Direction.WEST;
				case "right" -> Direction.EAST;
				default -> Direction.SOUTH;
			};
		}
	}
}
