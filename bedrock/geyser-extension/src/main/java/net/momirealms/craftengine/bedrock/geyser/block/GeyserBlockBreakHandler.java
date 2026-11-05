package net.momirealms.craftengine.bedrock.geyser.block;

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.BlockBreakHandler;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPunchPacket;

import java.util.Objects;

public final class GeyserBlockBreakHandler extends BlockBreakHandler {
    private final GeyserWorldBlocks blocks;

    public GeyserBlockBreakHandler(GeyserSession session, GeyserWorldBlocks blocks) {
        super(session);
        this.blocks = blocks;
    }

    @Override
    public void handlePlayerAuthInputPacket(PlayerAuthInputPacket packet) {
        super.handlePlayerAuthInputPacket(packet);
        if (super.currentBlockState != null && this.blocks.contains(super.currentBlockState.javaId())) {
            // Java sends a swing every mining tick. Bedrock's animation packets arrive less often,
            // but CE uses swings to distinguish continued mining from a stopped/idle player.
            super.session.sendDownstreamGamePacket(ServerboundPunchPacket.INSTANCE);
            super.session.activateArmAnimationTicking();
        }
    }

    @Override
    protected float calculateBreakProgress(BlockState state, Vector3i position, GeyserItemStack stack) {
        return this.blocks.contains(state.javaId()) ? 0 : super.calculateBreakProgress(state, position, stack);
    }

    @Override
    protected void handleContinueDestroy(Vector3i position,
                                         BlockState state,
                                         Direction face,
                                         boolean bedrockDestroyed,
                                         boolean sendParticles,
                                         long tick) {
        GeyserItemStack item = super.session.getPlayerInventory().getItemInHand();
        boolean sameItem = super.currentItemStack != null && super.currentItemStack.getJavaId() == item.getJavaId() && Objects.equals(super.currentItemStack.getComponents(), item.getComponents());
        if (!this.blocks.contains(state.javaId()) || !position.equals(super.currentBlockPos) || !sameItem) {
            super.handleContinueDestroy(position, state, face, bedrockDestroyed, sendParticles, tick);
            return;
        }
        super.currentBlockFace = face;
        if (bedrockDestroyed) {
            BlockUtils.restoreCorrectBlock(super.session, position, state);
        }
        if (sendParticles) {
            BlockUtils.spawnBlockBreakParticles(super.session, face, position, state);
        }
    }
}
