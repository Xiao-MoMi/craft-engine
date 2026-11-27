package net.momirealms.craftengine.bedrock.geyser.entity.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserDyeProperties;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserPacketEvent;
import net.momirealms.craftengine.bedrock.network.NativeModelPayload;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
import org.geysermc.geyser.api.entity.property.BatchPropertyUpdater;
import org.geysermc.geyser.api.entity.property.type.GeyserFloatEntityProperty;
import org.geysermc.geyser.api.entity.property.type.GeyserIntEntityProperty;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.BedrockEntityDefinition;
import org.geysermc.geyser.entity.VanillaEntities;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.GeyserSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class GeyserNativeModels {
    private final Map<String, Model> models = new LinkedHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(-2000000000);

    public GeyserNativeModels(JsonObject manifest) {
        if (manifest != null) {
            for (Map.Entry<String, JsonElement> entry : manifest.entrySet()) {
                this.models.put(entry.getKey(), new Model(entry.getValue().getAsJsonObject()));
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public void register(GeyserDefineEntitiesEvent event) {
        for (Model model : this.models.values()) {
            model.definition = CustomEntityDefinition.of(model.id);
            event.register(model.definition);
        }
    }

    public void properties(GeyserDefineEntityPropertiesEvent event) {
        this.models.values().forEach(model -> model.register(event));
    }

    public Connection createConnection(GeyserSession session) {
        return this.models.isEmpty() ? null : new Connection(session);
    }

    public final class Connection {
        private static final int REMOVED = 1;
        private static final int ADDED = 2;
        private final GeyserSession session;
        private final Map<UUID, Instance> instances = new HashMap<>();
        private final ConcurrentHashMap<Integer, Integer> hidden = new ConcurrentHashMap<>();
        private final Int2IntOpenHashMap displayChanges = new Int2IntOpenHashMap();
        private final Map<Long, Integer> targets = new ConcurrentHashMap<>();
        private final ModelEngineAnchors anchors;

        private Connection(GeyserSession session) {
            this.session = session;
            this.anchors = new ModelEngineAnchors(session, this.hidden.keySet());
        }

        public void onAddEntity(GeyserPacketEvent<AddEntityPacket> event) {
            if (!this.hidden.isEmpty()) {
                Entity entity = this.session.getEntityCache().getEntityByGeyserId(event.packet().getRuntimeEntityId());
                if (entity != null && this.hidden.containsKey(entity.getEntityId())) {
                    event.setCancelled(true);
                    return;
                }
            }
            this.anchors.onSpawn(event);
        }

        public boolean hasModelAnchors() {
            return this.anchors.hasCarriers();
        }

        public void onEntityData(GeyserPacketEvent<SetEntityDataPacket> event) {
            this.anchors.onEntityData(event);
        }

        public void onRemoveEntity(GeyserPacketEvent<RemoveEntityPacket> event) {
            this.anchors.forget(event.packet().getUniqueEntityId());
        }

        public void onTransaction(GeyserPacketEvent<InventoryTransactionPacket> event) {
            Integer target = this.targets.get(event.packet().getRuntimeEntityId());
            if (target != null) {
                Entity entity = target == 0 ? null : this.session.getEntityCache().getEntityByJavaId(target);
                if (entity != null) {
                    InventoryTransactionPacket packet = event.packet().clone();
                    packet.setRuntimeEntityId(entity.geyserId());
                    event.setPacket(packet);
                } else {
                    event.setCancelled(true);
                }
            }
        }

        public void clear() {
            this.instances.clear();
            this.hidden.clear();
            this.targets.clear();
            this.anchors.clear();
        }

        public void accept(NativeModelPayload.NativeMessage message) {
            switch (message) {
                case NativeModelPayload.RemoveNative remove -> this.remove(remove.instance());
                case NativeModelPayload.NativeState state -> {
                    Instance instance = this.instances.get(state.instance());
                    if (instance == null) {
                        this.update(null, state, null);
                    } else if (!instance.state.model().equals(state.model())) {
                        this.remove(state.instance());
                        this.update(null, state, null);
                    } else if (!state.equals(instance.state)) {
                        this.update(instance, state, state.changesSince(instance.state));
                    }
                }
                case NativeModelPayload.Delta delta -> {
                    Instance instance = this.instances.get(delta.instance());
                    // 切换维度会清空本地实例，收到下一份完整状态后才能恢复增量同步。
                    if (instance != null) {
                        this.update(instance, delta.applyTo(instance.state), delta);
                    }
                }
            }
        }

        private void remove(UUID id) {
            Instance instance = this.instances.remove(id);
            if (instance != null) {
                this.targets.remove(instance.entity.geyserId());
                this.session.getEntityCache().removeEntity(instance.entity);
                this.replaceHidden(instance.state.displays(), List.of());
                this.anchors.replace(instance.state.anchors(), List.of());
            }
        }

        private void update(Instance instance, NativeModelPayload.NativeState state, NativeModelPayload.Delta delta) {
            Model model = GeyserNativeModels.this.models.get(state.model());
            if (model == null) {
                return;
            }
            NativeModelPayload.Pose pose = state.pose();
            boolean startedHurt = state.hurt() && (instance == null || !instance.state.hurt());
            if (instance == null) {
                EntitySpawnContext context = new EntitySpawnContext(this.session, VanillaEntities.ARMOR_STAND,
                        GeyserNativeModels.this.nextId.decrementAndGet(), state.instance())
                        .bedrockEntityDefinition((BedrockEntityDefinition) model.definition)
                        .position(Vector3f.from(pose.x(), pose.y(), pose.z()))
                        .yaw(pose.yaw())
                        .headYaw(pose.yaw())
                        .pitch(pose.pitch());
                Entity entity = new Entity(context);
                entity.updatePropertiesBatched(batch -> model.apply(batch, state, null), false);
                instance = new Instance(entity, state);
                this.instances.put(state.instance(), instance);
                this.replaceHidden(List.of(), state.displays());
                this.anchors.replace(List.of(), state.anchors());
                this.session.getEntityCache().spawnEntity(entity);
            } else {
                List<Integer> previousDisplays = instance.state.displays();
                List<UUID> previousAnchors = instance.state.anchors();
                instance.state = state;
                if (delta.displays() != null) {
                    this.replaceHidden(previousDisplays, state.displays());
                }
                if (delta.anchors() != null) {
                    this.anchors.replace(previousAnchors, state.anchors());
                }
                if (delta.pose() != null) {
                    instance.entity.moveAbsolute(Vector3f.from(pose.x(), pose.y(), pose.z()), pose.yaw(), pose.pitch(), pose.yaw(), true, false);
                }
                if (delta.scale() != null || delta.color() != null || delta.animations() != null || delta.hiddenBones() != null) {
                    instance.entity.updatePropertiesBatched(batch -> model.apply(batch, state, delta), true);
                }
            }
            if (startedHurt) {
                EntityEventPacket hurt = new EntityEventPacket();
                hurt.setRuntimeEntityId(instance.entity.geyserId());
                hurt.setType(EntityEventType.HURT);
                this.session.sendUpstreamPacket(hurt);
            }
            if (delta == null || delta.target() != null) {
                this.targets.put(instance.entity.geyserId(), state.target());
            }
        }

        private void replaceHidden(List<Integer> previous, List<Integer> current) {
            this.displayChanges.clear();
            for (int id : previous) {
                this.displayChanges.put(id, REMOVED);
            }
            for (int id : current) {
                this.displayChanges.put(id, this.displayChanges.get(id) | ADDED);
            }
            // 只统计当前实例新增或移出的 ID。先提交所有引用计数，再重建客户端实体，以便生成包看到最终状态。
            for (Int2IntMap.Entry change : Int2IntMaps.fastIterable(this.displayChanges)) {
                int id = change.getIntKey();
                if (change.getIntValue() == ADDED) {
                    if (this.hidden.merge(id, 1, Integer::sum) != 1) {
                        change.setValue(0);
                    }
                } else if (change.getIntValue() == REMOVED) {
                    if (this.hidden.computeIfPresent(id, (key, count) -> count == 1 ? null : count - 1) != null) {
                        change.setValue(0);
                    }
                }
            }
            for (Int2IntMap.Entry change : Int2IntMaps.fastIterable(this.displayChanges)) {
                int transition = change.getIntValue();
                if (transition != REMOVED && transition != ADDED) {
                    continue;
                }
                Entity entity = this.session.getEntityCache().getEntityByJavaId(change.getIntKey());
                if (entity == null) {
                    continue;
                }
                if (transition == ADDED) {
                    RemoveEntityPacket remove = new RemoveEntityPacket();
                    remove.setUniqueEntityId(entity.geyserId());
                    this.session.sendUpstreamPacket(remove);
                } else {
                    entity.spawnEntity();
                }
            }
        }
    }

    private static final class Instance {
        private final Entity entity;
        private NativeModelPayload.NativeState state;

        private Instance(Entity entity, NativeModelPayload.NativeState state) {
            this.entity = entity;
            this.state = state;
        }
    }

    private static final class Model {
        private final Identifier id;
        private final boolean supportsDye;
        private final Object2IntOpenHashMap<String> animationIndices = new Object2IntOpenHashMap<>();
        private final int animationCount;
        private final Map<String, Integer> boneIndices = new HashMap<>();
        private final List<GeyserIntEntityProperty> animationProperties = new ArrayList<>();
        private final List<GeyserIntEntityProperty> boneProperties = new ArrayList<>();
        private CustomEntityDefinition definition;
        private GeyserDyeProperties dye;
        private GeyserFloatEntityProperty sx;
        private GeyserFloatEntityProperty sy;
        private GeyserFloatEntityProperty sz;

        private Model(JsonObject data) {
            this.id = Identifier.of(data.get("identifier").getAsString());
            this.supportsDye = data.get("dye").getAsBoolean();
            JsonArray animations = data.getAsJsonArray("animations");
            this.animationCount = animations.size();
            this.animationIndices.defaultReturnValue(-1);
            for (int i = 0; i < this.animationCount; i++) {
                this.animationIndices.putIfAbsent(animations.get(i).getAsString(), i);
            }
            JsonObject bones = data.getAsJsonObject("bones");
            for (Map.Entry<String, JsonElement> entry : bones.entrySet()) {
                String alias = entry.getValue().getAsString();
                int index = Integer.parseInt(alias, 1, alias.length(), 10);
                if (alias.charAt(0) != 'b' || index < 0 || index >= bones.size()) {
                    throw new IllegalArgumentException("Invalid bone index for " + entry.getKey() + ": " + alias);
                }
                this.boneIndices.put(entry.getKey(), index);
            }
        }

        private void register(GeyserDefineEntityPropertiesEvent event) {
            this.animationProperties.clear();
            this.boneProperties.clear();
            if (this.supportsDye) {
                this.dye = GeyserDyeProperties.register(event, this.id, "ce:model_dye");
            }
            this.sx = event.registerFloatProperty(this.id, Identifier.of("ce:model_sx"), -1000, 1000, 1f);
            this.sy = event.registerFloatProperty(this.id, Identifier.of("ce:model_sy"), -1000, 1000, 1f);
            this.sz = event.registerFloatProperty(this.id, Identifier.of("ce:model_sz"), -1000, 1000, 1f);
            for (int i = 0; i < (this.animationCount + 8) / 9; i++) {
                this.animationProperties.add(event.registerIntegerProperty(this.id, Identifier.of("ce:model_anim" + i), 0, 262143, 0));
            }
            for (int i = 0; i < (this.boneIndices.size() + 18) / 19; i++) {
                this.boneProperties.add(event.registerIntegerProperty(this.id, Identifier.of("ce:model_bone" + i), 0, 524287, 0));
            }
        }

        private void apply(BatchPropertyUpdater batch, NativeModelPayload.NativeState state, NativeModelPayload.Delta delta) {
            if (this.dye != null && (delta == null || delta.color() != null)) {
                this.dye.write(batch, state.color());
            }
            if (delta == null || delta.scale() != null) {
                batch.update(this.sx, state.scale().x());
                batch.update(this.sy, state.scale().y());
                batch.update(this.sz, state.scale().z());
            }
            if (delta == null || delta.animations() != null) {
                this.applyAnimations(batch, state.animations());
            }
            if (delta == null || delta.hiddenBones() != null) {
                this.applyHiddenBones(batch, state.hiddenBones());
            }
        }

        private void applyAnimations(BatchPropertyUpdater batch, List<NativeModelPayload.Animation> animations) {
            int[] modes = new int[this.animationProperties.size()];
            for (NativeModelPayload.Animation animation : animations) {
                int index = this.animationIndices.getInt(animation.name());
                if (index >= 0) {
                    modes[index / 9] |= animation.mode() << (index % 9 * 2);
                }
            }
            for (int i = 0; i < modes.length; i++) {
                batch.update(this.animationProperties.get(i), modes[i]);
            }
        }

        private void applyHiddenBones(BatchPropertyUpdater batch, List<String> bones) {
            int[] hidden = new int[this.boneProperties.size()];
            for (String name : bones) {
                Integer index = this.boneIndices.get(name);
                if (index != null) {
                    hidden[index / 19] |= 1 << (index % 19);
                }
            }
            for (int i = 0; i < hidden.length; i++) {
                batch.update(this.boneProperties.get(i), hidden[i]);
            }
        }
    }
}
