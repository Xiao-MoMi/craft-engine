package net.momirealms.craftengine.bedrock.geyser.entity;

import net.momirealms.craftengine.bedrock.geyser.entity.display.GeyserModelDisplayManager;
import net.momirealms.craftengine.bedrock.geyser.entity.display.GeyserTextDisplayManager;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserJavaPackets;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.network.HardHitboxPayload;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.api.entity.type.GeyserEntity;
import org.geysermc.geyser.entity.BedrockEntityDefinition;
import org.geysermc.geyser.entity.VanillaEntities;
import org.geysermc.geyser.entity.VanillaEntityType;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.living.monster.ShulkerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;

public final class GeyserEntitySpawnTranslator implements GeyserJavaPackets.Interceptor<ClientboundAddEntityPacket> {
    private static final int ENTITY_TYPE_COUNT = EntityType.values().length;

    private final SpawnHandler[] handlers = new SpawnHandler[ENTITY_TYPE_COUNT];

    GeyserEntitySpawnTranslator(GeyserModelDisplayManager modelDisplays, GeyserTextDisplayManager textDisplays) {
        this.registerModel(EntityType.ITEM_DISPLAY, modelDisplays.itemDefinition());
        this.registerModel(EntityType.BLOCK_DISPLAY, modelDisplays.blockDefinition());
        this.handlers[EntityType.TEXT_DISPLAY.ordinal()] = (player, packet) -> spawnText(player, packet, textDisplays);
        BedrockEntityDefinition carrier = modelDisplays.emptyCarrier();
        this.handlers[EntityType.SHULKER.ordinal()] = (player, packet) -> spawnHardHitbox(player, packet, carrier);
    }

    @Override
    public void translate(GeyserPlayer player, ClientboundAddEntityPacket packet, PacketTranslator<ClientboundAddEntityPacket> original) {
        SpawnHandler handler = this.handlers[packet.getType().ordinal()];
        if (handler == null || !handler.spawn(player, packet)) {
            original.translate(player.session(), packet);
        }
    }

    private void registerModel(EntityType type, @Nullable VanillaEntityType<?> definition) {
        if (definition != null) {
            this.handlers[type.ordinal()] = (player, packet) -> spawnModel(player.session(), packet, definition);
        }
    }

    private static boolean spawnModel(GeyserSession session, ClientboundAddEntityPacket packet, VanillaEntityType<?> definition) {
        EntitySpawnContext context = spawnContext(session, packet, definition);
        if (context != null) {
            spawn(context, definition.factory().create(context));
        }
        return true;
    }

    private static boolean spawnText(GeyserPlayer player, ClientboundAddEntityPacket packet, GeyserTextDisplayManager textDisplays) {
        GeyserSession session = player.session();
        if (!textDisplays.supports(session)) {
            return false;
        }
        EntitySpawnContext context = spawnContext(session, packet, textDisplays.definition());
        if (context != null) {
            spawn(context, textDisplays.create(context, player));
        }
        return true;
    }

    private static boolean spawnHardHitbox(GeyserPlayer player, ClientboundAddEntityPacket packet, @Nullable BedrockEntityDefinition carrier) {
        HardHitboxPayload hitbox = player.takeHitbox(packet.getEntityId());
        if (hitbox == null) {
            return false;
        }

        GeyserSession session = player.session();
        VanillaEntityType<ShulkerEntity> definition = VanillaEntities.SHULKER;
        EntitySpawnContext context = EntitySpawnContext.fromPacket(session, definition, packet);
        if (carrier != null) {
            context.bedrockEntityDefinition(carrier);
        }
        if (!context.callServerSpawnEvent()) {
            return true;
        }

        ShulkerEntity shulker = definition.factory().create(context);
        shulker.setFlag(EntityFlag.INVISIBLE, true);
        shulker.setFlag(EntityFlag.RENDER_WHEN_INVISIBLE, false);
        shulker.setFlag(EntityFlag.HAS_GRAVITY, false);
        shulker.getMetadata().updateOverride(EntityDataTypes.WIDTH, (float) (hitbox.width() + 0.0001));
        shulker.getMetadata().updateOverride(EntityDataTypes.HEIGHT, (float) hitbox.height());
        spawn(context, shulker);
        return true;
    }

    @Nullable
    private static EntitySpawnContext spawnContext(GeyserSession session, ClientboundAddEntityPacket packet, VanillaEntityType<?> definition) {
        EntitySpawnContext context = EntitySpawnContext.fromPacket(session, definition, packet);
        // 已由 CraftEngine 接管，事件取消时直接结束，避免回退后再次触发生成事件
        return context.callServerSpawnEvent() ? context : null;
    }

    private static void spawn(EntitySpawnContext context, Entity entity) {
        Collection<Consumer<GeyserEntity>> consumers = context.consumers();
        if (consumers != null) {
            for (Consumer<GeyserEntity> consumer : consumers) {
                consumer.accept(entity);
            }
        }
        context.session().getEntityCache().spawnEntity(entity);
    }

    @FunctionalInterface
    private interface SpawnHandler {

        boolean spawn(GeyserPlayer player, ClientboundAddEntityPacket packet);
    }
}
