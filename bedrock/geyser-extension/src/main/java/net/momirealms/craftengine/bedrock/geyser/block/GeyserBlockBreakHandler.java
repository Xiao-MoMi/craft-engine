package net.momirealms.craftengine.bedrock.geyser.block;

import net.momirealms.craftengine.bedrock.geyser.item.GeyserItemManager;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.BlockBreakHandler;
import org.geysermc.geyser.translator.item.CustomItemTranslator;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPunchPacket;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class GeyserBlockBreakHandler extends BlockBreakHandler {
    private static final int DESTROY_DELAY_TICKS = 5;
    private final GeyserWorldBlocks blocks;
    private final GeyserItemManager items;
    private long resumeMiningTick;
    private boolean pendingStart;

    public GeyserBlockBreakHandler(GeyserSession session, GeyserWorldBlocks blocks, GeyserItemManager items) {
        super(session);
        this.blocks = blocks;
        this.items = items;
    }

    @Override
    public void handlePlayerAuthInputPacket(PlayerAuthInputPacket packet) {
        super.handlePlayerAuthInputPacket(packet);
        BlockState state = super.currentBlockState;
        if (!this.pendingStart && state != null && this.blocks.contains(state.javaId())) {
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
    protected void destroyBlock(BlockState state, Vector3i position, Direction direction, boolean instamine) {
        GeyserItemStack tool = super.currentItemStack;
        if (!instamine && super.serverSideBlockBreaking && tool.has(DataComponentTypes.TOOL) && this.canDestroyBlock(state)) {
            ItemDefinition definition = CustomItemTranslator.getCustomItem(super.session, tool.getAmount(), tool.getAllComponents(), tool.getMapping(super.session));
            if (definition != null && this.items.isRegisteredItem(definition.getIdentifier())) {
                this.resumeMiningTick = super.session.getClientTicks() + DESTROY_DELAY_TICKS + 1;
            }
        }
        super.destroyBlock(state, position, direction, instamine);
    }

    @Override
    protected void handleStartBreak(@NotNull Vector3i position, @NotNull BlockState state, Direction face, long tick) {
        this.resumeMiningTick = 0;
        if (this.pendingStart) {
            this.clearCurrentVariables();
        }
        super.handleStartBreak(position, state, face, tick);
    }

    @Override
    protected void handleContinueDestroy(@NotNull Vector3i position,
                                         BlockState state,
                                         @NotNull Direction face,
                                         boolean bedrockDestroyed,
                                         boolean sendParticles,
                                         long tick) {
        if (tick < this.resumeMiningTick) {
            // 记录基岩端的目标，让 Geyser 能处理松手、换块和客户端预测。到期前不向 Java 发起挖掘。
            this.pendingStart = true;
            super.currentBlockPos = position;
            super.currentBlockState = state;
            super.currentBlockFace = face;
            if (bedrockDestroyed) {
                BlockUtils.restoreCorrectBlock(super.session, position, state);
            }
            return;
        }
        if (this.pendingStart) {
            if (state.is(Blocks.AIR) || !this.canBreak(position, state, PlayerActionType.BLOCK_CONTINUE_DESTROY)) {
                this.clearCurrentVariables();
                BlockUtils.stopBreakAndRestoreBlock(super.session, position, state);
                return;
            }
            if (bedrockDestroyed) {
                BlockUtils.restoreCorrectBlock(super.session, position, state);
            }
            this.handleStartBreak(position, state, face, tick);
            return;
        }
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

    @Override
    protected void clearCurrentVariables() {
        super.clearCurrentVariables();
        this.pendingStart = false;
    }

    @Override
    public void reset() {
        super.reset();
        this.resumeMiningTick = 0;
    }
}
