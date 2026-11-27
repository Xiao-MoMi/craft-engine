package net.momirealms.craftengine.bedrock.geyser.gui;

import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.ContainerClosePacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.inventory.InventoryHolder;
import org.geysermc.geyser.session.GeyserSession;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Obsolete
public final class GeyserContainerCloseHandler {
    private final GeyserSession session;
    private long clientTick = -1;
    private long resumeTick;
    private BedrockPacketWrapper pendingClose;

    public GeyserContainerCloseHandler(GeyserSession session) {
        this.session = session;
    }

    public boolean defer(BedrockPacketWrapper wrapper) {
        if (!(wrapper.getPacket() instanceof ContainerClosePacket close) || !this.shouldWait(close)) {
            return false;
        }
        if (this.pendingClose == null) {
            // Bedrock clears the type-based server-close flag after its next input tick.
            this.resumeTick = this.clientTick + 2;
            this.pendingClose = wrapper;
        } else {
            wrapper.release();
        }
        return true;
    }

    @Nullable
    public BedrockPacketWrapper afterInput(PlayerAuthInputPacket input) {
        this.clientTick = Math.max(this.clientTick, input.getTick());
        if (this.clientTick < this.resumeTick) {
            return null;
        }
        BedrockPacketWrapper close = this.pendingClose;
        this.pendingClose = null;
        return close;
    }

    private boolean shouldWait(ContainerClosePacket packet) {
        if (this.clientTick < 0 || packet.getId() < 0 || packet.isServerInitiated() || !this.session.isClosingInventory()) {
            return false;
        }
        InventoryHolder<?> holder = this.session.getInventoryHolder();
        return holder != null && holder.pending() && holder.bedrockId() != packet.getId();
    }

    public void discardPending() {
        if (this.pendingClose != null) {
            this.pendingClose.release();
            this.pendingClose = null;
        }
    }
}
