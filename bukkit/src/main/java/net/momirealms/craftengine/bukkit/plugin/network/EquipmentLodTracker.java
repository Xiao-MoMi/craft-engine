package net.momirealms.craftengine.bukkit.plugin.network;

import ca.spottedleaf.concurrentutil.map.concurrent.ints.ConcurrentChainedInt2ObjectHashTable;
import com.mojang.datafixers.util.Pair;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.item.setting.value.EquipmentData;
import net.momirealms.craftengine.core.item.setting.value.EquipmentFallback;
import net.momirealms.craftengine.core.plugin.network.PacketPosition;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEquipmentPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EquipmentSlotProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EquipmentLodTracker {
    private final Player player;
    private final ConcurrentChainedInt2ObjectHashTable<EntityState> entities = new ConcurrentChainedInt2ObjectHashTable<>();
    private PacketPosition viewerPosition;

    public EquipmentLodTracker(BukkitServerPlayer player) {
        this.player = player;
    }

    private static boolean isFar(double distanceSquared, double distance, boolean wasFar) {
        double threshold = wasFar ? distance * 0.9 : distance;
        return distanceSquared >= threshold * threshold;
    }

    public void add(int entityId, PacketPosition position) {
        if (entityId != this.player.entityId()) {
            this.entities.put(entityId, new EntityState(entityId, position));
        }
    }

    public void remove(int entityId) {
        this.entities.remove(entityId);
    }

    public void clear() {
        this.entities.clear();
        this.viewerPosition = null;
    }

    public void move(int entityId, short dx, short dy, short dz) {
        EntityState state = this.entities.get(entityId);
        if (state != null) {
            state.position = state.position.move(dx, dy, dz);
        }
    }

    public void position(int entityId, PacketPosition position, int relatives) {
        EntityState state = this.entities.get(entityId);
        if (state != null) {
            state.position = state.position.resolve(position, relatives);
        }
    }

    public Optional<Item> equipment(int entityId, int slot, Item original) {
        EntityState state = this.entities.get(entityId);
        EquipmentFallback fallback = original.getDefinition().map(definition -> definition.settings().equipmentFallback()).orElse(null);
        Item snapshot = state != null && fallback != null ? original.copy() : null;
        Optional<Item> conversion = BukkitItemManager.instance().s2c(original, this.player, ItemPacketSource.SET_EQUIPMENT);
        if (state == null) return conversion;
        Item converted = conversion.orElse(original);
        SlotState previous = slot < state.slots.length ? state.slots[slot] : null;
        PacketPosition viewer = this.viewerPosition;
        Optional<EquipmentData> equipment = fallback == null ? Optional.empty() : converted.equippable();
        boolean enabled = equipment.isPresent() && equipment.get().assetId() != null;
        boolean far = enabled && viewer != null && isFar(viewer.distanceSquared(state.position), fallback.distance(), previous != null && previous.far);
        if (far) {
            converted = converted.copy();
            Optional<EquipmentData> equippable = converted.equippable();
            if (equippable.isPresent()) {
                EquipmentData data = equippable.get();
                data.setAssetId(fallback.assetId());
                converted.equippable(data);
            }
        }
        if (enabled || previous != null) {
            SlotState[] slots = state.slots.length == 0 ? new SlotState[EquipmentSlotProxy.VALUES.length] : state.slots.clone();
            slots[slot] = enabled ? new SlotState(snapshot, fallback.distance(), far) : null;
            state.slots = slots;
        }
        return far ? Optional.of(converted) : conversion;
    }

    public void asyncTick(PacketPosition viewer) {
        this.viewerPosition = viewer;
        List<EntityState> changed = new ArrayList<>();
        for (EntityState state : this.entities.values()) {
            double distance = viewer.distanceSquared(state.position);
            for (SlotState slot : state.slots) {
                if (slot != null && isFar(distance, slot.distance, slot.far) != slot.far) {
                    changed.add(state);
                    break;
                }
            }
        }
        if (changed.isEmpty()) return;
        for (EntityState state : changed) {
            this.sendEquipmentAppearance(state);
        }
    }

    private void sendEquipmentAppearance(EntityState state) {
        PacketPosition viewer = this.viewerPosition;
        if (viewer == null) return;
        double distance = viewer.distanceSquared(state.position);
        SlotState[] slots = state.slots;
        List<Pair<Object, Object>> changes = new ArrayList<>();
        for (int slot = 0; slot < slots.length; slot++) {
            SlotState snapshotState = slots[slot];
            if (snapshotState != null && isFar(distance, snapshotState.distance, snapshotState.far) != snapshotState.far) {
                changes.add(Pair.of(EquipmentSlotProxy.VALUES[slot], snapshotState.original.copy().minecraftItem()));
            }
        }
        if (!changes.isEmpty()) {
            this.player.sendPacket(ClientboundSetEquipmentPacketProxy.INSTANCE.newInstance(state.entityId, changes), true);
        }
    }

    private static final class EntityState {
        private final int entityId;
        private PacketPosition position;
        private SlotState[] slots = new SlotState[0];

        private EntityState(int entityId, PacketPosition position) {
            this.entityId = entityId;
            this.position = position;
        }
    }

    private record SlotState(Item original, double distance, boolean far) {
    }
}
