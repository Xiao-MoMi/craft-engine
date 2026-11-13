package net.momirealms.craftengine.bedrock.geyser.entity.display;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import org.cloudburstmc.protocol.bedrock.data.debugshape.DebugShape;
import org.cloudburstmc.protocol.bedrock.packet.DebugDrawerPacket;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.entity.BedrockEntityDefinitions;
import org.geysermc.geyser.entity.EntityTypeBase;
import org.geysermc.geyser.entity.VanillaEntityType;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GeyserTextDisplayManager extends PacketTranslator<ClientboundAddEntityPacket> {
    private final Map<GeyserSession, Long2ObjectLinkedOpenHashMap<DebugShape>> pending = new ConcurrentHashMap<>();
    private PacketTranslator<ClientboundAddEntityPacket> original;
    private VanillaEntityType<GeyserTextDisplayEntity> definition;

    @SuppressWarnings("unchecked")
    public void install() {
        EntityTypeBase.Builder<GeyserTextDisplayEntity> base = EntityTypeBase.baseBuilder(GeyserTextDisplayEntity.class);
        base.addTranslator(MetadataTypes.BYTE, GeyserTextDisplayEntity::setFlags)
                .addTranslator(MetadataTypes.INT, GeyserTextDisplayEntity::setAir)
                .addTranslator(MetadataTypes.OPTIONAL_COMPONENT, GeyserTextDisplayEntity::setCustomName)
                .addTranslator(MetadataTypes.BOOLEAN, GeyserTextDisplayEntity::setCustomNameVisible)
                .addTranslator(MetadataTypes.BOOLEAN, GeyserTextDisplayEntity::setSilent)
                .addTranslator(MetadataTypes.BOOLEAN, GeyserTextDisplayEntity::setGravity)
                .addTranslator(null).addTranslator(null);
        this.definition = VanillaEntityType.inherited(context -> new GeyserTextDisplayEntity(context, this), base.build())
                .type(EntityType.TEXT_DISPLAY).bedrockDefinition(BedrockEntityDefinitions.ARMOR_STAND)
                .addTranslator(MetadataTypes.INT, (entity, meta) -> entity.transform().startInterpolation((int) meta.getValue()))
                .addTranslator(MetadataTypes.INT, (entity, meta) -> entity.transform().duration((int) meta.getValue()))
                .addTranslator(MetadataTypes.INT, (entity, meta) -> entity.teleportDuration((int) meta.getValue()))
                .addTranslator(MetadataTypes.VECTOR3, GeyserTextDisplayEntity::setTranslation)
                .addTranslator(MetadataTypes.VECTOR3, (entity, meta) -> entity.transform().scale(meta.getValue()))
                .addTranslator(MetadataTypes.QUATERNION, (entity, meta) -> entity.transform().left(meta.getValue()))
                .addTranslator(MetadataTypes.QUATERNION, (entity, meta) -> entity.transform().right(meta.getValue()))
                .addTranslator(MetadataTypes.BYTE, (entity, meta) -> entity.billboard(meta.getValue()))
                .addTranslator(null)
                .addTranslator(MetadataTypes.FLOAT, (entity, meta) -> entity.viewRange(meta.getValue()))
                .addTranslator(null).addTranslator(null).addTranslator(null).addTranslator(null).addTranslator(null)
                .addTranslator(MetadataTypes.COMPONENT, GeyserTextDisplayEntity::setText)
                .addTranslator(MetadataTypes.INT, (entity, meta) -> entity.lineWidth((int) meta.getValue()))
                .addTranslator(MetadataTypes.INT, (entity, meta) -> entity.background((int) meta.getValue()))
                .addTranslator(MetadataTypes.BYTE, (entity, meta) -> entity.opacity(meta.getValue()))
                .addTranslator(MetadataTypes.BYTE, (entity, meta) -> entity.style(meta.getValue()))
                .build(false);
        this.original = (PacketTranslator<ClientboundAddEntityPacket>) Registries.JAVA_PACKET_TRANSLATORS.get(ClientboundAddEntityPacket.class);
        Registries.JAVA_PACKET_TRANSLATORS.register(ClientboundAddEntityPacket.class, this);
    }

    @Override
    public void translate(GeyserSession session, ClientboundAddEntityPacket packet) {
        if (packet.getType() != EntityType.TEXT_DISPLAY || session.getUpstream().getProtocolVersion() < 975) {
            this.original.translate(session, packet);
            return;
        }
        EntitySpawnContext context = EntitySpawnContext.fromPacket(session, this.definition, packet);
        if (!context.callServerSpawnEvent()) {
            return;
        }
        GeyserTextDisplayEntity entity = this.definition.factory().create(context);
        if (context.consumers() != null) {
            context.consumers().forEach(consumer -> consumer.accept(entity));
        }
        session.getEntityCache().spawnEntity(entity);
    }

    void queue(GeyserSession session, DebugShape shape) {
        Long2ObjectLinkedOpenHashMap<DebugShape> shapes = this.pending.get(session);
        if (shapes == null) {
            shapes = new Long2ObjectLinkedOpenHashMap<>();
            this.pending.put(session, shapes);
            session.executeInEventLoop(() -> this.flush(session));
        }
        shapes.put(shape.getId(), shape);
    }

    private void flush(GeyserSession session) {
        Long2ObjectLinkedOpenHashMap<DebugShape> shapes = this.pending.remove(session);
        if (shapes == null || session.isClosed()) {
            return;
        }
        DebugDrawerPacket packet = new DebugDrawerPacket();
        packet.getShapes().addAll(shapes.values());
        session.sendUpstreamPacket(packet);
    }

    public void disconnect(GeyserConnection connection) {
        this.pending.remove(connection);
    }

    public void uninstall() {
        if (this.original != null) {
            Registries.JAVA_PACKET_TRANSLATORS.get().replace(ClientboundAddEntityPacket.class, this, this.original);
            this.original = null;
        }
        this.pending.clear();
    }
}
