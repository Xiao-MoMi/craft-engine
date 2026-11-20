package net.momirealms.craftengine.bedrock.geyser.block;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.level.event.BreakBlockEventData;
import org.geysermc.mcprotocollib.protocol.data.game.level.event.LevelEventType;
import org.geysermc.mcprotocollib.protocol.data.game.level.event.UnknownLevelEventData;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelEventPacket;

final class GeyserBlockBreakEffects extends PacketTranslator<ClientboundLevelEventPacket> {
    private final PacketTranslator<ClientboundLevelEventPacket> delegate;
    private final GeyserWorldBlocks blocks;

    GeyserBlockBreakEffects(PacketTranslator<ClientboundLevelEventPacket> delegate, GeyserWorldBlocks blocks) {
        this.delegate = delegate;
        this.blocks = blocks;
    }

    @SuppressWarnings("unchecked")
    static GeyserBlockBreakEffects install(GeyserWorldBlocks blocks) {
        PacketTranslator<ClientboundLevelEventPacket> delegate = (PacketTranslator<ClientboundLevelEventPacket>) Registries.JAVA_PACKET_TRANSLATORS.get(ClientboundLevelEventPacket.class);
        GeyserBlockBreakEffects effects = new GeyserBlockBreakEffects(delegate, blocks);
        Registries.JAVA_PACKET_TRANSLATORS.register(ClientboundLevelEventPacket.class, effects);
        return effects;
    }

    void uninstall() {
        Registries.JAVA_PACKET_TRANSLATORS.get().replace(ClientboundLevelEventPacket.class, this, this.delegate);
    }

    @Override
    public void translate(GeyserSession session, ClientboundLevelEventPacket packet) {
        if (packet.getEvent() == LevelEventType.PARTICLES_AND_SOUND_DESTROY_BLOCK) {
            int state = switch (packet.getData()) {
                case UnknownLevelEventData data -> data.getData();
                case BreakBlockEventData data -> data.getBlockState();
                case null, default -> -1;
            };
            if (state >= 0 && this.blocks.contains(state)) {
                // CraftEngine 已单独发送破坏音效，这里只补 Geyser 未翻译的方块粒子
                Vector3i position = packet.getPosition();
                LevelEventPacket effect = new LevelEventPacket();
                effect.setType(LevelEvent.PARTICLE_DESTROY_BLOCK_NO_SOUND);
                effect.setPosition(Vector3f.from(position.getX() + 0.5f, position.getY() + 0.5f, position.getZ() + 0.5f));
                effect.setData(session.getBlockMappings().getBedrockBlockId(state));
                session.sendUpstreamPacket(effect);
                return;
            }
        }
        this.delegate.translate(session, packet);
    }
}
