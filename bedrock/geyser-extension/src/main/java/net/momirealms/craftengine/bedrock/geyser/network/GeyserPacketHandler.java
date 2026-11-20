package net.momirealms.craftengine.bedrock.geyser.network;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutor;
import net.momirealms.craftengine.bedrock.geyser.gui.GeyserContainerCloseHandler;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;

import java.util.concurrent.RejectedExecutionException;

public final class GeyserPacketHandler extends ChannelDuplexHandler {
    private final GeyserPlayer player;
    private final EventExecutor sessionLoop;
    private final GeyserContainerCloseHandler containerClose;

    public GeyserPacketHandler(GeyserPlayer player, boolean delayContainerClose) {
        this.player = player;
        this.sessionLoop = player.session().getTickEventLoop();
        this.containerClose = delayContainerClose ? new GeyserContainerCloseHandler(player.session()) : null;
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) {
        this.dispatch(context, () -> this.receive(context, message), message, null);
    }

    private void receive(ChannelHandlerContext context, Object message) {
        BedrockPacketWrapper delayed = null;
        if (message instanceof BedrockPacketWrapper wrapper && wrapper.getSenderSubClientId() == 0 && !this.player.isClosed()) {
            try {
                if (GeyserBedrockPackets.receive(this.player, wrapper)) {
                    wrapper.release();
                    return;
                }
                if (this.containerClose != null) {
                    if (this.containerClose.defer(wrapper)) {
                        return;
                    }
                    if (wrapper.getPacket() instanceof PlayerAuthInputPacket input) {
                        delayed = this.containerClose.afterInput(input);
                    }
                }
            } catch (Throwable error) {
                wrapper.release();
                context.fireExceptionCaught(error);
                return;
            }
        }
        context.fireChannelRead(message);
        if (delayed != null) {
            context.fireChannelRead(delayed);
        }
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) {
        this.dispatch(context, () -> this.send(context, message, promise), message, promise);
    }

    private void send(ChannelHandlerContext context, Object message, ChannelPromise promise) {
        if (message instanceof BedrockPacketWrapper wrapper && wrapper.getTargetSubClientId() == 0 && !this.player.isClosed()) {
            try {
                if (GeyserBedrockPackets.send(this.player, wrapper)) {
                    wrapper.release();
                    promise.trySuccess();
                    return;
                }
            } catch (Throwable error) {
                wrapper.release();
                promise.tryFailure(error);
                return;
            }
        }
        context.write(message, promise);
    }

    // 完成一次读取、刷新和关闭也排入同一队列，防止越过尚在会话线程处理的包。
    @Override
    public void channelReadComplete(ChannelHandlerContext context) {
        this.dispatch(context, context::fireChannelReadComplete, null, null);
    }

    @Override
    public void flush(ChannelHandlerContext context) {
        this.dispatch(context, context::flush, null, null);
    }

    @Override
    public void close(ChannelHandlerContext context, ChannelPromise promise) {
        this.afterPending(() -> context.close(promise));
    }

    @Override
    public void disconnect(ChannelHandlerContext context, ChannelPromise promise) {
        this.afterPending(() -> context.disconnect(promise));
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) {
        this.afterPending(() -> {
            this.discardPendingClose();
            context.fireChannelInactive();
        });
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext context) {
        this.afterPending(context::fireChannelUnregistered);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext context, Object event) {
        this.dispatch(context, () -> context.fireUserEventTriggered(event), event, null);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext context) {
        this.afterPending(this::discardPendingClose);
    }

    private void dispatch(ChannelHandlerContext context, Runnable task, Object message, ChannelPromise promise) {
        if (this.sessionLoop.inEventLoop()) {
            task.run();
            return;
        }
        try {
            this.sessionLoop.execute(task);
        } catch (RejectedExecutionException error) {
            ReferenceCountUtil.release(message);
            if (promise == null) {
                context.fireExceptionCaught(error);
            } else {
                promise.tryFailure(error);
            }
        }
    }

    private void discardPendingClose() {
        if (this.containerClose != null) {
            this.containerClose.discardPending();
        }
    }

    private void afterPending(Runnable task) {
        if (this.sessionLoop.inEventLoop()) {
            task.run();
            return;
        }
        try {
            this.sessionLoop.execute(task);
        } catch (RejectedExecutionException error) {
            this.sessionLoop.terminationFuture().addListener(done -> task.run());
        }
    }
}
