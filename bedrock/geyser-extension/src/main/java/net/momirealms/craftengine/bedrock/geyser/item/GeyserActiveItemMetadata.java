package net.momirealms.craftengine.bedrock.geyser.item;

import net.momirealms.craftengine.bedrock.geyser.CraftEngineGeyserExtension;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;

final class GeyserActiveItemMetadata extends PacketTranslator<ClientboundSetEntityDataPacket> {
    private static final int LIVING_ENTITY_FLAGS = 8;
    private final PacketTranslator<ClientboundSetEntityDataPacket> delegate;

    private GeyserActiveItemMetadata(PacketTranslator<ClientboundSetEntityDataPacket> delegate) {
        this.delegate = delegate;
    }

    @SuppressWarnings("unchecked")
    static GeyserActiveItemMetadata install() {
        GeyserActiveItemMetadata handler = new GeyserActiveItemMetadata((PacketTranslator<ClientboundSetEntityDataPacket>)
                Registries.JAVA_PACKET_TRANSLATORS.get(ClientboundSetEntityDataPacket.class));
        Registries.JAVA_PACKET_TRANSLATORS.register(ClientboundSetEntityDataPacket.class, handler);
        return handler;
    }

    @Override
    public void translate(GeyserSession session, ClientboundSetEntityDataPacket packet) {
        if (packet.getEntityId() == session.getPlayerEntity().getEntityId()) {
            for (EntityMetadata<?, ?> value : packet.getMetadata()) {
                if (value.getId() == LIVING_ENTITY_FLAGS && value instanceof ByteEntityMetadata flags) {
                    GeyserPlayer player = CraftEngineGeyserExtension.instance().playerManager().get(session);
                    if (player != null) {
                        player.activeUse().update(flags.getPrimitiveValue());
                    }
                    break;
                }
            }
        }
        this.delegate.translate(session, packet);
    }

    void uninstall() {
        Registries.JAVA_PACKET_TRANSLATORS.get().replace(ClientboundSetEntityDataPacket.class, this, this.delegate);
    }
}
