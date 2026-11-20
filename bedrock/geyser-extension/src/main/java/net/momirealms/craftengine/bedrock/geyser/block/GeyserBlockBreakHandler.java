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
import org.jetbrains.annotations.NotNull;

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
        BlockState state = super.currentBlockState;
        if (state != null && this.blocks.contains(state.javaId())) {
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
    protected void handleContinueDestroy(@NotNull Vector3i position,
                                         BlockState state,
                                         @NotNull Direction face,
                                         boolean bedrockDestroyed,
                                         boolean sendParticles,
                                         long tick) {
        if (!this.blocks.contains(state.javaId()) || !position.equals(super.currentBlockPos) || !this.isHoldingSameItem()) {
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

    private boolean isHoldingSameItem() {
        GeyserItemStack previous = super.currentItemStack;
        if (previous == null) {
            return false;
        }
        GeyserItemStack held = super.session.getPlayerInventory().getItemInHand();
        return previous == held || (previous.getJavaId() == held.getJavaId() && Objects.equals(previous.getComponents(), held.getComponents()));
    }
}
