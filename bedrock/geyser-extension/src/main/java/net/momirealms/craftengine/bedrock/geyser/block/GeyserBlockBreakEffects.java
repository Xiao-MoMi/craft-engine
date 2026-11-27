package net.momirealms.craftengine.bedrock.geyser.block;

import net.momirealms.craftengine.bedrock.geyser.network.GeyserJavaPackets;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.level.event.BreakBlockEventData;
import org.geysermc.mcprotocollib.protocol.data.game.level.event.LevelEventType;
import org.geysermc.mcprotocollib.protocol.data.game.level.event.UnknownLevelEventData;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelEventPacket;

public final class GeyserBlockBreakEffects implements GeyserJavaPackets.Interceptor<ClientboundLevelEventPacket> {
    private final GeyserWorldBlocks blocks;

    public GeyserBlockBreakEffects(GeyserWorldBlocks blocks) {
        this.blocks = blocks;
    }

    @Override
    public void translate(GeyserPlayer player, ClientboundLevelEventPacket packet, PacketTranslator<ClientboundLevelEventPacket> original) {
        GeyserSession session = player.session();
        if (packet.getEvent() == LevelEventType.PARTICLES_AND_SOUND_DESTROY_BLOCK) {
            int state = switch (packet.getData()) {
                case UnknownLevelEventData data -> data.getData();
                case BreakBlockEventData data -> data.getBlockState();
                default -> -1;
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
        original.translate(session, packet);
    }
}
