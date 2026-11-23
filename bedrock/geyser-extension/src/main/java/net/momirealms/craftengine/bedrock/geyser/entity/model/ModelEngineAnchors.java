package net.momirealms.craftengine.bedrock.geyser.entity.model;

import net.momirealms.craftengine.bedrock.geyser.network.GeyserPacketEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.*;
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.RemoveEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityLinkPacket;
import org.geysermc.geyser.entity.EntityDataBehaviorRegistry;
import org.geysermc.geyser.entity.VanillaEntities;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.AreaEffectCloudEntity;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.GeyserSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

final class ModelEngineAnchors {
    private final GeyserSession session;
    private final Set<Integer> hidden;
    private final Map<UUID, Integer> owners = new ConcurrentHashMap<>();
    private final Set<Long> carriers = ConcurrentHashMap.newKeySet();

    ModelEngineAnchors(GeyserSession session, Set<Integer> hidden) {
        this.session = session;
        this.hidden = hidden;
    }

    void replace(List<UUID> previous, List<UUID> current) {
        for (UUID id : previous) {
            if (!current.contains(id) && this.owners.computeIfPresent(id, (key, count) -> count == 1 ? null : count - 1) == null) {
                this.respawn(id, false);
            }
        }
        for (UUID id : current) {
            if (!previous.contains(id) && this.owners.merge(id, 1, Integer::sum) == 1) {
                this.respawn(id, true);
            }
        }
    }

    void onSpawn(GeyserPacketEvent<AddEntityPacket> event) {
        AddEntityPacket packet = event.packet();
        if (this.owners.isEmpty() || !"minecraft:area_effect_cloud".equals(packet.getIdentifier())) {
            return;
        }
        Entity entity = this.session.getEntityCache().getEntityByGeyserId(packet.getRuntimeEntityId());
        if (entity instanceof AreaEffectCloudEntity && this.owners.containsKey(entity.uuid())) {
            this.carriers.add(packet.getRuntimeEntityId());
            AddEntityPacket stand = packet.clone();
            stand.setIdentifier("minecraft:armor_stand");
            stand.setMetadata(carrierMetadata(packet.getMetadata(), true));
            event.setPacket(stand);
        }
    }

    void onEntityData(GeyserPacketEvent<SetEntityDataPacket> event) {
        SetEntityDataPacket packet = event.packet();
        if (this.carriers.contains(packet.getRuntimeEntityId())) {
            SetEntityDataPacket update = packet.clone();
            update.setMetadata(carrierMetadata(packet.getMetadata(), false));
            event.setPacket(update);
        }
    }

    private static EntityDataMap carrierMetadata(EntityDataMap original, boolean spawn) {
        EntityDataMap data = new EntityDataMap();
        data.putAll(original);
        data.remove(EntityDataTypes.AREA_EFFECT_CLOUD_RADIUS);
        data.remove(EntityDataTypes.AREA_EFFECT_CLOUD_PARTICLE);
        data.remove(EntityDataTypes.AREA_EFFECT_CLOUD_DURATION);
        data.remove(EntityDataTypes.AREA_EFFECT_CLOUD_SPAWN_TIME);
        data.remove(EntityDataTypes.AREA_EFFECT_CLOUD_CHANGE_RATE);
        data.remove(EntityDataTypes.AREA_EFFECT_CLOUD_CHANGE_ON_PICKUP);
        data.remove(EntityDataTypes.AREA_EFFECT_CLOUD_PICKUP_COUNT);
        EnumMap<EntityFlag, Boolean> flags = original.getFlags();
        if (spawn || flags != null) {
            flags = flags == null ? new EnumMap<>(EntityFlag.class) : new EnumMap<>(flags);
            flags.put(EntityFlag.INVISIBLE, true);
            flags.remove(EntityFlag.HAS_GRAVITY);
            flags.remove(EntityFlag.HAS_COLLISION);
            data.putFlags(flags);
        }
        if (spawn || original.containsKey(EntityDataTypes.WIDTH)) {
            data.put(EntityDataTypes.WIDTH, 0f);
        }
        if (spawn || original.containsKey(EntityDataTypes.HEIGHT)) {
            data.put(EntityDataTypes.HEIGHT, 0f);
        }
        return data;
    }

    private void respawn(UUID id, boolean entering) {
        Entity entity = this.session.getEntityCache().getEntityByUuid(id);
        if (!(entity instanceof AreaEffectCloudEntity cloud) || !cloud.isValid()
                || cloud.bedrockDefinition() != VanillaEntities.AREA_EFFECT_CLOUD.defaultBedrockDefinition()) {
            return;
        }
        RemoveEntityPacket remove = new RemoveEntityPacket();
        remove.setUniqueEntityId(cloud.geyserId());
        this.session.sendUpstreamPacket(remove);
        if (entering) {
            cloud.spawnEntity();
        } else {
            this.restoreCloud(cloud);
        }
        // 只重建客户端载体。缓存中的上下车关系不变，隐藏的骨骼也不应收到骑乘包。
        Entity vehicle = cloud.getVehicle();
        if (vehicle != null && vehicle.isValid() && !this.hidden.contains(vehicle.getEntityId())) {
            this.link(vehicle, cloud, vehicle.getPassengers().indexOf(cloud));
        }
        List<Entity> passengers = cloud.getPassengers();
        for (int i = 0; i < passengers.size(); i++) {
            Entity passenger = passengers.get(i);
            if (passenger.isValid() && !this.hidden.contains(passenger.getEntityId())) {
                this.link(cloud, passenger, i);
            }
        }
    }

    private void restoreCloud(AreaEffectCloudEntity cloud) {
        // Geyser 发送后会清空脏元数据。MEG 的挂载云使用默认粒子，半径由当前宽度还原。
        EntitySpawnContext context = new EntitySpawnContext(this.session, VanillaEntities.AREA_EFFECT_CLOUD, cloud.getEntityId(), cloud.uuid())
                .geyserId(cloud.geyserId()).position(cloud.getPosition()).motion(cloud.getMotion())
                .yaw(cloud.getYaw()).pitch(cloud.getPitch()).headYaw(cloud.getHeadYaw());
        AreaEffectCloudEntity copy = new AreaEffectCloudEntity(context);
        for (EntityFlag flag : EntityFlag.values()) {
            copy.setFlag(flag, cloud.getFlag(flag));
        }
        for (EntityDataType<?> type : EntityDataBehaviorRegistry.TRACKED_ENTITY_DATA) {
            copyMetadata(cloud, copy, type);
        }
        Float width = cloud.getMetadata().value(EntityDataTypes.WIDTH);
        if (width != null) {
            copy.getMetadata().put(EntityDataTypes.AREA_EFFECT_CLOUD_RADIUS, width * 0.5f);
        }
        copy.spawnEntity();
    }

    private static <T> void copyMetadata(Entity source, Entity destination, EntityDataType<T> type) {
        T value = source.getMetadata().value(type);
        if (value != null) {
            destination.getMetadata().put(type, value);
        }
    }

    private void link(Entity vehicle, Entity passenger, int index) {
        EntityLinkData.Type type = index == 0 ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER;
        SetEntityLinkPacket packet = new SetEntityLinkPacket();
        packet.setEntityLink(new EntityLinkData(vehicle.geyserId(), passenger.geyserId(), type, true, false, 0f));
        this.session.sendUpstreamPacket(packet);
    }

    boolean hasCarriers() {
        return !this.carriers.isEmpty();
    }

    void forget(long runtimeId) {
        this.carriers.remove(runtimeId);
    }

    void clear() {
        this.owners.clear();
        this.carriers.clear();
    }
}
