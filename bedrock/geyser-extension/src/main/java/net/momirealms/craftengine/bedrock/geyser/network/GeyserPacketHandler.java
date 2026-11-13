package net.momirealms.craftengine.bedrock.geyser.network;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;

public final class GeyserPacketHandler extends ChannelDuplexHandler {
    private final GeyserPlayer player;

    public GeyserPacketHandler(GeyserPlayer player) {
        this.player = player;
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        if (message instanceof BedrockPacketWrapper wrapper && wrapper.getSenderSubClientId() == 0 && !this.player.isClosed()) {
            boolean cancelled;
            try {
                cancelled = this.player.network().receive(wrapper);
            } catch (RuntimeException | Error e) {
                wrapper.release();
                throw e;
            }
            if (cancelled) {
                wrapper.release();
                return;
            }
        }
        super.channelRead(context, message);
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
        if (message instanceof BedrockPacketWrapper wrapper && wrapper.getTargetSubClientId() == 0 && !this.player.isClosed()) {
            boolean cancelled;
            try {
                cancelled = this.player.network().send(wrapper);
            } catch (RuntimeException | Error e) {
                wrapper.release();
                throw e;
            }
            if (cancelled) {
                wrapper.release();
                promise.trySuccess();
                return;
            }
        }
        super.write(context, message, promise);
    }
}
