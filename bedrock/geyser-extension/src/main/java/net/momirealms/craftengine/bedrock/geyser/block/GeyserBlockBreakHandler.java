package net.momirealms.craftengine.bedrock.geyser.block;

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.BlockBreakHandler;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPunchPacket;

import java.util.Objects;

public final class GeyserBlockBreakHandler extends BlockBreakHandler {
    private final GeyserWorldBlocks blocks;
    private final GeyserMiningProgress miningProgress;
    private long lastSwingTick = Long.MIN_VALUE;

    public GeyserBlockBreakHandler(GeyserSession session, GeyserWorldBlocks blocks) {
        super(session);
        this.blocks = blocks;
        this.miningProgress = new GeyserMiningProgress(session);
    }

    public void updateMiningProgress(int breakerId, int x, int y, int z, float progress, float speed) {
        Vector3i position = Vector3i.from(x, y, z);
        if (progress >= 0 && breakerId == Integer.MAX_VALUE - this.session.getPlayerEntity().getEntityId()
                && !position.equals(this.currentBlockPos)) {
            return;
        }
        this.miningProgress.update(breakerId, position, progress, speed);
    }

    @Override
    protected void clearCurrentVariables() {
        if (this.currentBlockPos != null) {
            this.miningProgress.forget(Integer.MAX_VALUE - this.session.getPlayerEntity().getEntityId(), this.currentBlockPos);
        }
        super.clearCurrentVariables();
    }

    @Override
    public void reset() {
        this.miningProgress.reset();
        super.reset();
    }

    @Override
    public void handlePlayerAuthInputPacket(PlayerAuthInputPacket packet) {
        super.handlePlayerAuthInputPacket(packet);
        if (this.currentBlockState != null && this.blocks.contains(this.currentBlockState.javaId())
                && packet.getTick() != this.lastSwingTick) {
            // Java sends a swing every mining tick. Bedrock's animation packets arrive less often,
            // but CE uses swings to distinguish continued mining from a stopped/idle player.
            this.lastSwingTick = packet.getTick();
            this.session.sendDownstreamGamePacket(ServerboundPunchPacket.INSTANCE);
            this.session.activateArmAnimationTicking();
        }
    }

    @Override
    protected float calculateBreakProgress(BlockState state, Vector3i position, GeyserItemStack stack) {
        return this.blocks.contains(state.javaId()) ? 0 : super.calculateBreakProgress(state, position, stack);
    }

    @Override
    protected void handleContinueDestroy(Vector3i position, BlockState state, Direction face,
                                         boolean bedrockDestroyed, boolean sendParticles, long tick) {
        boolean custom = this.blocks.contains(state.javaId());
        if (!custom) {
            if (this.currentBlockState != null && this.blocks.contains(this.currentBlockState.javaId())) {
                this.cancelCurrent();
                if (!sendParticles && !bedrockDestroyed) {
                    return;
                }
            }
            super.handleContinueDestroy(position, state, face, bedrockDestroyed, sendParticles, tick);
            return;
        }
        GeyserItemStack item = this.session.getPlayerInventory().getItemInHand();
        boolean sameItem = this.currentItemStack != null && this.currentItemStack.getJavaId() == item.getJavaId()
                && Objects.equals(this.currentItemStack.getComponents(), item.getComponents());
        if (!position.equals(this.currentBlockPos) || state != this.currentBlockState || !sameItem) {
            this.cancelCurrent();
            this.handleStartBreak(position, state, face, tick);
        }
        this.currentBlockFace = face;
        if (bedrockDestroyed) {
            BlockUtils.restoreCorrectBlock(this.session, position, state);
        }
        if (sendParticles) {
            BlockUtils.spawnBlockBreakParticles(this.session, face, position, state);
        }
        // Do not send FINISH_DIGGING or overwrite CE's crack packets with Geyser's carrier-based timer.
    }

    private void cancelCurrent() {
        if (this.currentBlockPos != null) {
            this.session.sendDownstreamGamePacket(new ServerboundPlayerActionPacket(PlayerAction.CANCEL_DIGGING,
                    this.currentBlockPos, Direction.DOWN.mcpl(), 0));
            BlockUtils.sendBedrockStopBlockBreak(this.session, this.currentBlockPos.toFloat());
            this.getDestructionStageCache().invalidate(this.currentBlockPos);
        }
        this.clearCurrentVariables();
    }
}
