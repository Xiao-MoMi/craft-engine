package net.momirealms.craftengine.core.pack.host.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedStream;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.traffic.GlobalChannelTrafficShapingHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import net.momirealms.craftengine.core.pack.host.ResourcePackDownloadData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class SelfHostHttpServer {
    private static SelfHostHttpServer instance;
    private final Cache<String, String> oneTimePackUrls = Caffeine.newBuilder()
            .maximumSize(1024)
            .scheduler(Scheduler.systemScheduler())
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();
    private final Cache<String, Bucket> ipRateLimiters = Caffeine.newBuilder()
            .maximumSize(1024)
            .scheduler(Scheduler.systemScheduler())
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong blockedRequests = new AtomicLong();

    private Bandwidth limitPerIp = Bandwidth.builder()
            .capacity(1)
            .refillGreedy(1, Duration.ofSeconds(1))
            .initialTokens(1)
            .build();

    private String ip = "localhost";
    private int port = -1;
    private String protocol = "http";
    private String url;
    private boolean denyNonMinecraft = true;
    private boolean useToken;
    private boolean strictValidation = false;
    private boolean enabled = false;

    private long globalUploadRateLimit = 0;
    private long minDownloadSpeed = 50_000;
    private GlobalChannelTrafficShapingHandler trafficShapingHandler;
    private ScheduledExecutorService virtualTrafficExecutor;
    private final ChannelGroup activeDownloadChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private byte[] resourcePackBytes;
    private String packHash;
    private UUID packUUID;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    private SelfHostHttpServer() {
        if (instance != null) {
            throw new IllegalStateException("SelfHostHttpServer is already initialized.");
        }
    }

    public static SelfHostHttpServer instance() {
        if (instance == null) {
            instance = new SelfHostHttpServer();
        }
        return instance;
    }

    public void updateProperties(String ip,
                                 int port,
                                 String url,
                                 boolean denyNonMinecraft,
                                 String protocol,
                                 Bandwidth limitPerIp,
                                 boolean token,
                                 long globalUploadRateLimit,
                                 long minDownloadSpeed,
                                 boolean strictValidation) {
        this.ip = ip;
        this.url = url;
        this.denyNonMinecraft = denyNonMinecraft;
        this.protocol = protocol;
        this.limitPerIp = limitPerIp;
        this.useToken = token;
        this.strictValidation = strictValidation;
        if (this.globalUploadRateLimit != globalUploadRateLimit || this.minDownloadSpeed != minDownloadSpeed) {
            this.globalUploadRateLimit = globalUploadRateLimit;
            this.minDownloadSpeed = minDownloadSpeed;
            if (this.trafficShapingHandler != null) {
                long initSize = globalUploadRateLimit <= 0 ? 0 : Math.max(minDownloadSpeed, globalUploadRateLimit);
                this.trafficShapingHandler.setWriteLimit(initSize);
                this.trafficShapingHandler.setWriteChannelLimit(initSize);
            }
        }
        if (this.port == port && this.serverChannel != null && this.enabled) return;
        disable();

        this.port = port;
        initializeServer();
    }

    public String url() {
        if (this.url != null && !this.url.isEmpty()) {
            return this.url;
        }
        return this.protocol + "://" + this.ip + ":" + this.port + "/";
    }

    private void initializeServer() {
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        this.virtualTrafficExecutor = Executors.newScheduledThreadPool(1, Thread.ofVirtual().factory());
        long initSize = this.globalUploadRateLimit <= 0 ? 0 : Math.max(this.minDownloadSpeed, this.globalUploadRateLimit);
        this.trafficShapingHandler = new GlobalChannelTrafficShapingHandler(
                this.virtualTrafficExecutor,
                initSize,
                0, // 全局读取不限
                initSize, // 默认单通道和总体一致
                0, // 单通道读取不限
                100, // checkInterval (ms)
                10_000 // maxTime (ms)
        );
        ServerBootstrap b = new ServerBootstrap();
        b.group(this.bossGroup, this.workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("trafficShaping", SelfHostHttpServer.this.trafficShapingHandler);
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new ChunkedWriteHandler());
                        pipeline.addLast(new HttpObjectAggregator(1048576));
                        pipeline.addLast(new RequestHandler());
                    }
                });
        try {
            this.serverChannel = b.bind(this.port).sync().channel();
            CraftEngine.instance().logger().info(TranslationManager.instance().plainTranslation("host.self.http_server_started", String.valueOf(this.port)));
            this.enabled = true;
        } catch (InterruptedException e) {
            CraftEngine.instance().logger().warn("Failed to start Netty server", e);
            Thread.currentThread().interrupt();
        }
    }

    @ChannelHandler.Sharable
    private class RequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            // 有人走了，其他人的速度上限提高
            if (SelfHostHttpServer.this.activeDownloadChannels.contains(ctx.channel())) {
                SelfHostHttpServer.this.activeDownloadChannels.remove(ctx.channel());
                rebalanceBandwidth();
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            SelfHostHttpServer.this.totalRequests.incrementAndGet();

            try {
                String clientIp = ((InetSocketAddress) ctx.channel().remoteAddress())
                        .getAddress().getHostAddress();

                if (!checkIpRateLimit(clientIp)) {
                    sendError(ctx, HttpResponseStatus.TOO_MANY_REQUESTS, "Forbidden");
                    SelfHostHttpServer.this.blockedRequests.incrementAndGet();
                    return;
                }

                QueryStringDecoder queryDecoder = new QueryStringDecoder(request.uri());
                String path = queryDecoder.path();

                if ("/download".equals(path)) {
                    handleDownload(ctx, request, queryDecoder);
                } else if ("/metrics".equals(path)) {
                    handleMetrics(ctx);
                } else {
                    sendError(ctx, HttpResponseStatus.NOT_FOUND, "Not Found");
                }
            } catch (Exception e) {
                CraftEngine.instance().logger().warn("Request handling failed", e);
                sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Error");
            }
        }

        private void handleDownload(ChannelHandlerContext ctx, FullHttpRequest request, QueryStringDecoder queryDecoder) {
            // 使用一次性token
            if (SelfHostHttpServer.this.useToken) {
                String token = queryDecoder.parameters().getOrDefault("token", Collections.emptyList()).stream().findFirst().orElse(null);
                String clientUUID = SelfHostHttpServer.this.strictValidation ? request.headers().get("X-Minecraft-UUID") : null;
                if (!validateToken(token, clientUUID)) {
                    sendError(ctx, HttpResponseStatus.FORBIDDEN, "Forbidden");
                    SelfHostHttpServer.this.blockedRequests.incrementAndGet();
                    return;
                }
            }

            // 不是Minecraft客户端
            if (SelfHostHttpServer.this.denyNonMinecraft) {
                String userAgent = request.headers().get(HttpHeaderNames.USER_AGENT);
                boolean nonMinecraftClient = userAgent == null || !userAgent.startsWith("Minecraft Java/");
                if (SelfHostHttpServer.this.strictValidation && !nonMinecraftClient) {
                    String clientVersion = request.headers().get("X-Minecraft-Version");
                    nonMinecraftClient = !Objects.equals(clientVersion, userAgent.substring("Minecraft Java/".length()));
                }
                if (nonMinecraftClient) {
                    sendError(ctx, HttpResponseStatus.FORBIDDEN, "Forbidden");
                    SelfHostHttpServer.this.blockedRequests.incrementAndGet();
                    return;
                }
            }

            // 没有资源包
            if (SelfHostHttpServer.this.resourcePackBytes == null) {
                sendError(ctx, HttpResponseStatus.NOT_FOUND, "Pack Not Found");
                SelfHostHttpServer.this.blockedRequests.incrementAndGet();
                return;
            }

            // 新人来了，所有人的速度上限降低
            if (!SelfHostHttpServer.this.activeDownloadChannels.contains(ctx.channel())) {
                SelfHostHttpServer.this.activeDownloadChannels.add(ctx.channel());
                rebalanceBandwidth();
            }

            // 告诉客户端资源包大小
            long fileLength = SelfHostHttpServer.this.resourcePackBytes.length;
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            HttpUtil.setContentLength(response, fileLength);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/zip");
            boolean keepAlive = HttpUtil.isKeepAlive(request);
            if (keepAlive) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
            ctx.write(response);

            // 发送分段资源包
            ChunkedStream chunkedStream = new ChunkedStream(new ByteArrayInputStream(SelfHostHttpServer.this.resourcePackBytes), 8192);
            HttpChunkedInput httpChunkedInput = new HttpChunkedInput(chunkedStream);
            ChannelFuture sendFileFuture = ctx.writeAndFlush(httpChunkedInput);
            if (!keepAlive) {
                sendFileFuture.addListener(ChannelFutureListener.CLOSE);
            }

            // 监听下载完成（成功或失败），以便在下载结束后（如果不关闭连接）也能移除计数
            // 注意：如果是 Keep-Alive，连接不会断，但下载结束了。
            // 为了精确控制，可以在这里监听 operationComplete
            sendFileFuture.addListener((ChannelFutureListener) future -> {
                if (SelfHostHttpServer.this.activeDownloadChannels.contains(ctx.channel())) {
                    SelfHostHttpServer.this.activeDownloadChannels.remove(ctx.channel());
                    rebalanceBandwidth();
                }
            });
        }

        private void handleMetrics(ChannelHandlerContext ctx) {
            String metrics = "# TYPE total_requests counter\n"
                    + "total_requests " + SelfHostHttpServer.this.totalRequests.get() + "\n"
                    + "# TYPE blocked_requests counter\n"
                    + "blocked_requests " + SelfHostHttpServer.this.blockedRequests.get();

            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(metrics, CharsetUtil.UTF_8)
            );
            response.headers()
                    .set(HttpHeaderNames.CONTENT_TYPE, "text/plain")
                    .set(HttpHeaderNames.CONTENT_LENGTH, metrics.length());

            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

        private boolean checkIpRateLimit(String clientIp) {
            if (SelfHostHttpServer.this.limitPerIp == null) return true;
            Bucket rateLimiter = SelfHostHttpServer.this.ipRateLimiters.get(clientIp, k -> Bucket.builder().addLimit(SelfHostHttpServer.this.limitPerIp).build());
            assert rateLimiter != null;
            return rateLimiter.tryConsume(1);
        }

        private boolean validateToken(String token, String clientUUID) {
            if (token == null || token.length() != 36) return false;
            String valid = SelfHostHttpServer.this.oneTimePackUrls.getIfPresent(token);
            boolean isValid = SelfHostHttpServer.this.strictValidation ? Objects.equals(valid, clientUUID) : valid != null;
            if (isValid) {
                SelfHostHttpServer.this.oneTimePackUrls.invalidate(token);
                return true;
            }
            return false;
        }

        private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    status,
                    Unpooled.copiedBuffer(message, CharsetUtil.UTF_8)
            );
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            ctx.close();
        }
    }

    private synchronized void rebalanceBandwidth() {
        if (this.globalUploadRateLimit == 0) {
            this.trafficShapingHandler.setWriteChannelLimit(0);
            return;
        }

        int activeCount = this.activeDownloadChannels.size();
        if (activeCount == 0) {
            this.trafficShapingHandler.setWriteChannelLimit(this.globalUploadRateLimit);
            return;
        }

        // 计算平均带宽：全局总量 / 当前人数
        long fairRate = this.globalUploadRateLimit / activeCount;

        // 确保不低于最小保障速率（可选，防止除法导致过小）
        fairRate = Math.max(fairRate, this.minDownloadSpeed);

        // 更新 Handler 配置
        this.trafficShapingHandler.setWriteChannelLimit(fairRate);
    }

    @Nullable
    public ResourcePackDownloadData generateOneTimeUrl(UUID user) {
        if (this.resourcePackBytes == null) return null;

        if (!this.useToken) {
            return new ResourcePackDownloadData(url() + "download", this.packUUID, this.packHash);
        }

        String token = UUID.randomUUID().toString();
        this.oneTimePackUrls.put(token, this.strictValidation ? user.toString().replace("-", "") : "");
        return new ResourcePackDownloadData(
                url() + "download?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8),
                this.packUUID,
                this.packHash
        );
    }

    public void disable() {
        this.enabled = false;
        // 释放流量整形资源
        if (this.trafficShapingHandler != null) {
            this.trafficShapingHandler.release();
            this.trafficShapingHandler = null;
        }
        // 关闭专用线程池
        if (this.virtualTrafficExecutor != null) {
            this.virtualTrafficExecutor.shutdown();
            this.virtualTrafficExecutor = null;
        }
        this.activeDownloadChannels.close();
        if (this.serverChannel != null) {
            this.serverChannel.close().awaitUninterruptibly();
            this.bossGroup.shutdownGracefully();
            this.workerGroup.shutdownGracefully();
            this.serverChannel = null;
        }
    }

    public void readResourcePack(Path path) {
        try {
            if (Files.exists(path)) {
                this.resourcePackBytes = Files.readAllBytes(path);
                calculateHash();
            } else {
                this.resourcePackBytes = null;
            }
        } catch (IOException e) {
            CraftEngine.instance().logger().error("Failed to load resource pack", e);
        }
    }

    private void calculateHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(this.resourcePackBytes);
            byte[] hashBytes = digest.digest();

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            this.packHash = hexString.toString();
            this.packUUID = UUID.nameUUIDFromBytes(this.packHash.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            CraftEngine.instance().logger().error("SHA-1 algorithm not available", e);
        }
    }
}