package net.momirealms.craftengine.core.pack.host;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class ResourcePackHost {
    private static ResourcePackHost instance;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private Channel serverChannel;
    private String ip = null;
    private int port = 0;
    private Path resourcePackPath = null;

    public String url() {
        return ConfigManager.hostProtocol() + "://" + ip + ":" + port + "/";
    }

    public void enable(String ip, int port, Path resourcePackPath) {
        if (ip.equals(this.ip) && port == this.port && resourcePackPath.equals(this.resourcePackPath)) {
            if (serverChannel != null && serverChannel.isActive()) {
                return;
            }
        }
        disable();

        this.ip = ip;
        this.port = port;
        this.resourcePackPath = resourcePackPath;

        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(this.bossGroup, this.workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(@NotNull Channel ch) {
                            ch.pipeline()
                                    .addLast(new HttpServerCodec())
                                    .addLast(new HttpObjectAggregator(65536))
                                    .addLast(new ChunkedWriteHandler())
                                    .addLast(new ResourcePackHandler());
                        }
                    })
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = bootstrap.bind(ip, port).sync();
            serverChannel = future.channel();
            CraftEngine.instance().logger().info("Netty resource pack server running on " + ip + ":" + port);
        } catch (InterruptedException e) {
            CraftEngine.instance().logger().warn("Failed to start Netty server", e);
            Thread.currentThread().interrupt();
        }
    }

    public void disable() {
        if (serverChannel != null) {
            serverChannel.close().awaitUninterruptibly();
            serverChannel = null;
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
    }

    public boolean isAlive() {
        return serverChannel != null && serverChannel.isActive();
    }

    public static ResourcePackHost instance() {
        if (instance == null) {
            instance = new ResourcePackHost();
        }
        return instance;
    }

    private class ResourcePackHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
            if (!Files.exists(resourcePackPath)) {
                CraftEngine.instance().logger().warn("ResourcePack not found: " + resourcePackPath);
                sendError(ctx, HttpResponseStatus.NOT_FOUND);
                return;
            }

            DefaultHttpResponse response = new DefaultHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK
            );
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, Files.size(resourcePackPath));
            ctx.write(response);

            try (RandomAccessFile file = new RandomAccessFile(resourcePackPath.toFile(), "r")) {
                FileChannel channel = file.getChannel();
                ctx.write(new DefaultFileRegion(channel, 0, channel.size()), ctx.newProgressivePromise());
                ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            } catch (IOException e) {
                sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            }
        }

        private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    status
            );
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            CraftEngine.instance().logger().warn("Netty server error", cause);
            ctx.close();
        }
    }
}