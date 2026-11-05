package net.momirealms.craftengine.bedrock.geyser.display;

import net.momirealms.craftengine.bedrock.FurnitureShulkerUUID;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent;
import org.geysermc.geyser.entity.BedrockEntityDefinition;
import org.geysermc.geyser.entity.type.living.monster.ShulkerEntity;

final class GeyserFurnitureHitboxes {

    private GeyserFurnitureHitboxes() {
    }

    @SuppressWarnings("UnstableApiUsage")
    static void onSpawn(ServerSpawnEntityEvent event, GeyserItemDisplays displays) {
        boolean cube = FurnitureShulkerUUID.isCube(event.uuid());
        if (!cube && !FurnitureShulkerUUID.isShulker(event.uuid())) {
            return;
        }
        BedrockEntityDefinition carrier = cube ? displays.emptyCarrier() : null;
        if (carrier != null) {
            // The display type has empty geometry at shape 0. Each standalone cube uses its normal square collision box.
            event.definition(carrier);
        }
        event.preSpawnConsumer(entity -> {
            if (entity instanceof ShulkerEntity shulker) {
                shulker.setFlag(EntityFlag.INVISIBLE, true);
                shulker.setFlag(EntityFlag.RENDER_WHEN_INVISIBLE, false);
                if (cube) {
                    float side = FurnitureShulkerUUID.cubeSide(event.uuid());
                    shulker.setFlag(EntityFlag.HAS_GRAVITY, false);
                    shulker.getMetadata().updateOverride(EntityDataTypes.WIDTH, side);
                    shulker.getMetadata().updateOverride(EntityDataTypes.HEIGHT, side);
                }
            }
        });
    }
}
