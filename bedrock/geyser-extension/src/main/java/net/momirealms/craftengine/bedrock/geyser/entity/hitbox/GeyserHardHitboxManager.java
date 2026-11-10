package net.momirealms.craftengine.bedrock.geyser.entity.hitbox;

import net.momirealms.craftengine.bedrock.geyser.CraftEngineGeyserExtension;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.momirealms.craftengine.bedrock.network.HardHitboxPayload;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent;
import org.geysermc.geyser.entity.BedrockEntityDefinition;
import org.geysermc.geyser.entity.type.living.monster.ShulkerEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GeyserHardHitboxManager {
    private final CraftEngineGeyserExtension extension;
    private final Map<GeyserConnection, Int2ObjectMap<HardHitboxPayload>> pending = new ConcurrentHashMap<>();

    public GeyserHardHitboxManager(CraftEngineGeyserExtension extension) {
        this.extension = extension;
    }

    public void register(GeyserConnection connection, HardHitboxPayload hitbox) {
        this.pending.computeIfAbsent(connection, key -> new Int2ObjectOpenHashMap<>()).put(hitbox.entityId(), hitbox);
    }

    public void disconnect(GeyserConnection connection) {
        this.pending.remove(connection);
    }

    public void disable() {
        this.pending.clear();
    }

    @SuppressWarnings("UnstableApiUsage")
    public void onSpawn(ServerSpawnEntityEvent event) {
        Int2ObjectMap<HardHitboxPayload> hitboxes = this.pending.get(event.connection());
        HardHitboxPayload hitbox = hitboxes == null ? null : hitboxes.remove(event.entityId());
        if (hitbox == null) {
            return;
        }
        BedrockEntityDefinition carrier = this.extension.itemDisplays().emptyCarrier();
        if (carrier != null) {
            event.definition(carrier);
        }
        event.preSpawnConsumer(entity -> {
            if (entity instanceof ShulkerEntity shulker) {
                shulker.setFlag(EntityFlag.INVISIBLE, true);
                shulker.setFlag(EntityFlag.RENDER_WHEN_INVISIBLE, false);
                shulker.setFlag(EntityFlag.HAS_GRAVITY, false);
                shulker.getMetadata().updateOverride(EntityDataTypes.WIDTH, (float) (hitbox.width() + 0.0001));
                shulker.getMetadata().updateOverride(EntityDataTypes.HEIGHT, (float) hitbox.height());
            }
        });
    }
}
