package net.momirealms.craftengine.bedrock.geyser.entity.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserDyeProperties;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.network.NativeModelPayload;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

    public void register(GeyserDefineEntitiesEvent event) {
        for (Model model : this.models.values()) {
            model.definition = CustomEntityDefinition.of(model.id);
            event.register(model.definition);
        }
    }

    public void properties(GeyserDefineEntityPropertiesEvent event) {
        this.models.values().forEach(model -> model.register(event));
    }

    public void accept(GeyserPlayer player, NativeModelPayload state) {
        GeyserSession session = player.session();
        session.ensureInEventLoop(() -> {
            if (!session.isClosed()) {
                this.connections.computeIfAbsent(session, ignored -> new Connection(player)).accept(state);
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

        private void accept(NativeModelPayload state) {
            Model model = GeyserNativeModels.this.models.get(state.model());
            if (model == null) {
                return;
            }
            if (state.removed()) {
                Instance instance = this.instances.remove(state.instance());
                if (instance != null) {
                    this.targets.remove(instance.entity.geyserId());
                    this.session.getEntityCache().removeEntity(instance.entity);
                    this.refreshHidden();
                }
                return;
            }
            Instance instance = this.instances.get(state.instance());
            if (instance == null) {
                EntitySpawnContext context = new EntitySpawnContext(this.session, VanillaEntities.ARMOR_STAND,
                        GeyserNativeModels.this.nextId.decrementAndGet(), state.instance())
                        .bedrockEntityDefinition((BedrockEntityDefinition) model.definition)
                        .position(Vector3f.from(state.x(), state.y(), state.z()))
                        .yaw(state.yaw())
                        .headYaw(state.yaw())
                        .pitch(state.pitch());
                Entity entity = new Entity(context);
                entity.updatePropertiesBatched(batch -> model.apply(batch, state), false);
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
                instance.entity.moveAbsolute(Vector3f.from(state.x(), state.y(), state.z()), state.yaw(), state.pitch(), state.yaw(), true, false);
                instance.entity.updatePropertiesBatched(batch -> model.apply(batch, state), true);
            }
            this.targets.put(instance.entity.geyserId(), state.target());
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
        private NativeModelPayload state;

        private Instance(Entity entity, NativeModelPayload state) {
            this.entity = entity;
            this.state = state;
        }
    }

    private static final class Model {
        private final Identifier id;
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
            this.dye = GeyserDyeProperties.register(event, this.id, "ce:model_dye");
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

        private void apply(BatchPropertyUpdater batch, NativeModelPayload state) {
            this.dye.write(batch, state.color());
            batch.update(this.sx, state.scaleX());
            batch.update(this.sy, state.scaleY());
            batch.update(this.sz, state.scaleZ());
            int[] modes = new int[this.animationProperties.size()];
            for (NativeModelPayload.Animation animation : state.animations()) {
                int index = this.animations.indexOf(animation.name());
                if (index >= 0) {
                    modes[index / 9] |= animation.mode() << (index % 9 * 2);
                }
            }
            for (int i = 0; i < modes.length; i++) {
                batch.update(this.animationProperties.get(i), modes[i]);
            }
            int[] hidden = new int[this.boneProperties.size()];
            for (String name : state.hiddenBones()) {
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
