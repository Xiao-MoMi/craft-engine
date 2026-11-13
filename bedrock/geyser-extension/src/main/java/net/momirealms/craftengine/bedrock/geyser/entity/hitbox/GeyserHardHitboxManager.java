package net.momirealms.craftengine.bedrock.geyser.entity.hitbox;

import net.momirealms.craftengine.bedrock.geyser.CraftEngineGeyserExtension;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.network.HardHitboxPayload;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent;
import org.geysermc.geyser.entity.BedrockEntityDefinition;
import org.geysermc.geyser.entity.type.living.monster.ShulkerEntity;

public final class GeyserHardHitboxManager {
    private final CraftEngineGeyserExtension extension;

    public GeyserHardHitboxManager(CraftEngineGeyserExtension extension) {
        this.extension = extension;
    }

    @SuppressWarnings("UnstableApiUsage")
    public void onSpawn(ServerSpawnEntityEvent event) {
        GeyserPlayer player = this.extension.playerManager().get(event.connection());
        HardHitboxPayload hitbox = player == null ? null : player.takeHitbox(event.entityId());
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
