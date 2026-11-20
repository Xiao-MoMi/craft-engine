package net.momirealms.craftengine.bedrock.geyser.entity.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserDyeProperties;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.network.NativeModelPayload;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.ChangeDimensionPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;
import org.cloudburstmc.protocol.bedrock.packet.RemoveEntityPacket;
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
    private final Map<GeyserSession, Connection> connections = new ConcurrentHashMap<>();
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

    public void accept(GeyserPlayer player, NativeModelPayload.NativeMessage message) {
        GeyserSession session = player.session();
        session.ensureInEventLoop(() -> {
            if (!session.isClosed()) {
                this.connections.computeIfAbsent(session, ignored -> new Connection(player)).accept(message);
            }
        });
    }

    public void disconnect(GeyserSession session) {
        this.connections.remove(session);
    }

    private final class Connection {
        private final GeyserSession session;
        private final Map<UUID, Instance> instances = new HashMap<>();
        private final Set<Integer> hidden = ConcurrentHashMap.newKeySet();
        private final Map<Long, Integer> targets = new ConcurrentHashMap<>();

        private Connection(GeyserPlayer player) {
            this.session = player.session();
            player.network().onSend(AddEntityPacket.class, event -> {
                Entity entity = this.session.getEntityCache().getEntityByGeyserId(event.packet().getRuntimeEntityId());
                if (entity != null && this.hidden.contains(entity.getEntityId())) {
                    event.setCancelled(true);
                }
            });
            player.network().onReceive(InventoryTransactionPacket.class, event -> {
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
            });
            player.network().onSend(ChangeDimensionPacket.class, event -> {
                this.instances.clear();
                this.hidden.clear();
                this.targets.clear();
            });
        }

        private void accept(NativeModelPayload.NativeMessage message) {
            switch (message) {
                case NativeModelPayload.RemoveNative remove -> this.remove(remove.instance());
                case NativeModelPayload.NativeState state -> {
                    Instance instance = this.instances.get(state.instance());
                    if (instance != null && !instance.state.model().equals(state.model())) {
                        this.remove(state.instance());
                        instance = null;
                    }
                    this.update(instance, state, null);
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
                this.refreshHidden();
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
                this.refreshHidden();
                this.session.getEntityCache().spawnEntity(entity);
            } else {
                boolean displaysChanged = !instance.state.displays().equals(state.displays());
                instance.state = state;
                if (displaysChanged) {
                    this.refreshHidden();
                }
                if (delta == null || delta.pose() != null) {
                    instance.entity.moveAbsolute(Vector3f.from(pose.x(), pose.y(), pose.z()), pose.yaw(), pose.pitch(), pose.yaw(), true, false);
                }
                if (delta == null || delta.scale() != null || delta.color() != null || delta.animations() != null || delta.hiddenBones() != null) {
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

        private void refreshHidden() {
            Set<Integer> active = new HashSet<>();
            for (Instance instance : this.instances.values()) {
                active.addAll(instance.state.displays());
            }
            Set<Integer> restored = new HashSet<>(this.hidden);
            restored.removeAll(active);
            Set<Integer> added = new HashSet<>(active);
            added.removeAll(this.hidden);
            this.hidden.clear();
            this.hidden.addAll(active);
            // Keep Java entities cached so metadata and interaction IDs remain valid while their renderers are hidden.
            for (int id : added) {
                Entity entity = this.session.getEntityCache().getEntityByJavaId(id);
                if (entity != null) {
                    RemoveEntityPacket remove = new RemoveEntityPacket();
                    remove.setUniqueEntityId(entity.geyserId());
                    this.session.sendUpstreamPacket(remove);
                }
            }
            for (int id : restored) {
                Entity entity = this.session.getEntityCache().getEntityByJavaId(id);
                if (entity != null) {
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
        private final List<String> animations = new ArrayList<>();
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
            data.getAsJsonArray("animations").forEach(name -> this.animations.add(name.getAsString()));
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
            if (this.supportsDye) {
                this.dye = GeyserDyeProperties.register(event, this.id, "ce:model_dye");
            }
            this.sx = event.registerFloatProperty(this.id, Identifier.of("ce:model_sx"), -1000, 1000, 1f);
            this.sy = event.registerFloatProperty(this.id, Identifier.of("ce:model_sy"), -1000, 1000, 1f);
            this.sz = event.registerFloatProperty(this.id, Identifier.of("ce:model_sz"), -1000, 1000, 1f);
            for (int i = 0; i < (this.animations.size() + 8) / 9; i++) {
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
                int index = this.animations.indexOf(animation.name());
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
