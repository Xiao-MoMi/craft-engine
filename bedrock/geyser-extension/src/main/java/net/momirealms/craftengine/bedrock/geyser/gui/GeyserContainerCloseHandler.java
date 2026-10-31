package net.momirealms.craftengine.bedrock.geyser.gui;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.ContainerClosePacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.inventory.InventoryHolder;
import org.geysermc.geyser.session.GeyserSession;

public final class GeyserContainerCloseHandler extends ChannelInboundHandlerAdapter {
    private static final String HANDLER_NAME = "craftengine-container-close";
    private final GeyserSession session;
    private long clientTick = -1;
    private long resumeTick;
    private BedrockPacketWrapper pendingClose;

    GeyserContainerCloseHandler(GeyserSession session) {
        this.session = session;
    }

    public static void install(GeyserSession session) {
        Channel channel = session.getUpstream().getSession().getPeer().getChannel();
        channel.pipeline().addBefore(session.getTickEventLoop(), BedrockPeer.NAME, HANDLER_NAME,
                new GeyserContainerCloseHandler(session));
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        if (message instanceof BedrockPacketWrapper wrapper && wrapper.getSenderSubClientId() == 0) {
            if (wrapper.getPacket() instanceof PlayerAuthInputPacket input) {
                this.clientTick = Math.max(this.clientTick, input.getTick());
                super.channelRead(context, message);
                if (this.pendingClose != null && this.clientTick >= this.resumeTick) {
                    BedrockPacketWrapper close = this.pendingClose;
                    this.pendingClose = null;
                    context.fireChannelRead(close);
                }
                return;
            }
            if (wrapper.getPacket() instanceof ContainerClosePacket close && this.shouldWait(close)) {
                if (this.pendingClose == null) {
                    // Bedrock confirms screen destruction before its next player tick clears the
                    // type-based server-close flag. Wait past that tick before Geyser opens a successor.
                    this.resumeTick = this.clientTick + 2;
                    this.pendingClose = wrapper;
                } else {
                    wrapper.release();
                }
                return;
            }
        }
        super.channelRead(context, message);
    }

    private boolean shouldWait(ContainerClosePacket packet) {
        if (this.clientTick < 0 || packet.getId() < 0 || packet.isServerInitiated() || !this.session.isClosingInventory()) {
            return false;
        }
        InventoryHolder<?> holder = this.session.getInventoryHolder();
        return holder != null && holder.pending() && holder.bedrockId() != packet.getId();
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        this.discardPending();
        super.channelInactive(context);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext context) throws Exception {
        this.discardPending();
        super.handlerRemoved(context);
    }

    private void discardPending() {
        if (this.pendingClose != null) {
            this.pendingClose.release();
            this.pendingClose = null;
        }
    }
}
