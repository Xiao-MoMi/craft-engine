package net.momirealms.craftengine.core.pack.host.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.google.gson.JsonObject;
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
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.traffic.GlobalChannelTrafficShapingHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import net.momirealms.craftengine.core.pack.host.ResourcePackDownloadData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.util.UUIDUtils;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class SelfHostHttpServer {
    // 传输过程中彻底停住(既不读也不写)多久就关掉。这里用 allIdle 而不是读超时是有意的:
    // 请求发完之后客户端在整个下载期间都不会再发东西, 读超时会把正常的大包下载掐断,
    // 而 allIdle 期间服务端一直在写, 不算空闲。
    private static final int IDLE_TIMEOUT_SECONDS = 30;
    // 从连接建立到完整请求解析出来的硬性上限。allIdle 单独是不够的: 它被任何一个入站字节重置,
    // 所以每29秒滴一个字节的连接永远不会空闲 —— 这正是经典 Slowloris 的做法。请求阶段单独限时,
    // 之后不再有任何计时器约束传输, 200MB的资源包照常下载。
    // 对应 nginx 的 client_header_timeout / Apache 的 mod_reqtimeout。
    private static final int REQUEST_TIMEOUT_SECONDS = 15;
    // 聚合后请求体的上限。请求行和头部不归它管, 那是 HttpServerCodec 的 4096/8192。
    // 这些接口都不读请求体, 所以这个值只是给畸形请求留的余量。
    private static final int MAX_HTTP_CONTENT_LENGTH = 16 * 1024;
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
    private boolean useServerPort = false;
    private boolean autoIp = false;
    private boolean enabled = false;
    private String forwardSecret;

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
                                 boolean strictValidation,
                                 boolean useServerPort,
                                 boolean autoIp,
                                 String forwardSecret) {
        this.ip = ip;
        this.autoIp = autoIp;
        this.url = url;
        this.denyNonMinecraft = denyNonMinecraft;
        this.protocol = protocol;
        this.limitPerIp = limitPerIp;
        this.useToken = token;
        this.strictValidation = strictValidation;
        this.useServerPort = useServerPort;
        this.forwardSecret = forwardSecret;
        if (this.globalUploadRateLimit != globalUploadRateLimit || this.minDownloadSpeed != minDownloadSpeed) {
            this.globalUploadRateLimit = globalUploadRateLimit;
            this.minDownloadSpeed = minDownloadSpeed;
            if (this.trafficShapingHandler != null) {
                long initSize = globalUploadRateLimit <= 0 ? 0 : Math.max(minDownloadSpeed, globalUploadRateLimit);
                this.trafficShapingHandler.setWriteLimit(initSize);
                this.trafficShapingHandler.setWriteChannelLimit(initSize);
            }
        }
        if (useServerPort) {
            disable();
            this.port = port;
            initializeServerPortHost();
        } else {
            if (this.port == port && this.serverChannel != null && this.enabled) return;
            disable();
            this.port = port;
            initializeServer();
        }
    }

    public String url(boolean localhost) {
        if (this.url != null && !this.url.isEmpty()) {
            return this.url;
        }
        if (this.useServerPort) {
            return this.protocol + "://" + (localhost ? "localhost" : this.ip) + ":" + CraftEngine.instance().platform().getServerPort() + "/";
        } else {
            return this.protocol + "://" + (localhost ? "localhost" : this.ip) + ":" + this.port + "/";
        }
    }

    private void initializeServerPortHost() {
        long initSize = this.globalUploadRateLimit <= 0 ? 0 : Math.max(this.minDownloadSpeed, this.globalUploadRateLimit);
        this.virtualTrafficExecutor = Executors.newScheduledThreadPool(1, Thread.ofVirtual().factory());
        this.trafficShapingHandler = new GlobalChannelTrafficShapingHandler(
                this.virtualTrafficExecutor,
                initSize,
                0, // 全局读取不限
                initSize, // 默认单通道和总体一致
                0, // 单通道读取不限
                100, // checkInterval (ms)
                10_000 // maxTime (ms)
        );
        CraftEngine.instance().networkManager().setServerPortHost(pipeline -> {
            pipeline.addLast(new IdleStateHandler(0, 0, IDLE_TIMEOUT_SECONDS));
            pipeline.addLast("trafficShaping", SelfHostHttpServer.this.trafficShapingHandler);
            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast(new ChunkedWriteHandler());
            pipeline.addLast(new HttpObjectAggregator(MAX_HTTP_CONTENT_LENGTH));
            pipeline.addLast(new RequestDeadlineHandler());
            pipeline.addLast(new RequestHandler());
        });
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
                        pipeline.addLast(new IdleStateHandler(0, 0, IDLE_TIMEOUT_SECONDS));
                        pipeline.addLast("trafficShaping", SelfHostHttpServer.this.trafficShapingHandler);
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new ChunkedWriteHandler());
                        pipeline.addLast(new HttpObjectAggregator(MAX_HTTP_CONTENT_LENGTH));
                        pipeline.addLast(new RequestDeadlineHandler());
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

    /**
     * 给请求阶段一个硬性期限。连上就开始计时, 完整请求解析出来就取消。
     * <p>
     * 之所以不能只靠 IdleStateHandler: 它的 allIdle 会被任何入站字节重置, 于是慢速滴字节的连接
     * 永远不算空闲。这里的期限和滴多慢无关。取消之后传输阶段完全不受计时器约束。
     * <p>
     * 超大请求被聚合器拒掉(413)时不会产生 FullHttpRequest, 所以期限保持有效, 那条连接也会被关掉。
     * <p>
     * 每条pipeline一个实例, 状态不跨连接共享, 所以不能标 @Sharable。
     */
    private static final class RequestDeadlineHandler extends ChannelInboundHandlerAdapter {
        private ScheduledFuture<?> deadline;

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            // 用 handlerAdded 而不是 channelActive: server-port 模式下这套pipeline是装到
            // 一条已经激活的连接上的, channelActive 不会再触发。
            this.deadline = ctx.executor().schedule(() -> { ctx.close(); }, REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof FullHttpRequest) {
                cancel();
            }
            ctx.fireChannelRead(msg);
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) {
            cancel();
        }

        private void cancel() {
            ScheduledFuture<?> current = this.deadline;
            if (current != null) {
                this.deadline = null;
                current.cancel(false);
            }
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
                } else if (SelfHostHttpServer.this.forwardSecret != null && "/forward".equals(path)) {
                    handleForward(ctx, request);
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

        private void handleForward(ChannelHandlerContext ctx, FullHttpRequest request) {
            String secret = request.headers().get("secret");
            if (secret == null || !secret.equals(SelfHostHttpServer.this.forwardSecret)) {
                sendError(ctx, HttpResponseStatus.UNAUTHORIZED, "Unauthorized");
                return;
            }
            if (SelfHostHttpServer.this.resourcePackBytes == null) {
                sendError(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE, "No resource pack available");
                return;
            }
            String uuid = request.headers().get("uuid");
            if (uuid == null || !UUIDUtils.validateUUID(uuid)) {
                sendError(ctx, HttpResponseStatus.BAD_REQUEST, "Incorrect UUID");
                return;
            }
            JsonObject jsonObject = new JsonObject();
            if (SelfHostHttpServer.this.useToken) {
                String token = UUID.randomUUID().toString();
                SelfHostHttpServer.this.oneTimePackUrls.put(token, SelfHostHttpServer.this.strictValidation ? uuid.replace("-", "") : "");
                jsonObject.addProperty("url", SelfHostHttpServer.this.url(false) + "download?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8));
            } else {
                jsonObject.addProperty("url", SelfHostHttpServer.this.url(false) + "download");
            }
            jsonObject.addProperty("uuid", SelfHostHttpServer.this.packUUID.toString());
            jsonObject.addProperty("hash", SelfHostHttpServer.this.packHash);
            String json = jsonObject.toString();
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
            response.headers()
                    .set(HttpHeaderNames.CONTENT_TYPE, "application/json")
                    .set(HttpHeaderNames.CONTENT_LENGTH, json.length());
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
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof IdleStateEvent) {
                // 既不读也不写了。要么是请求发了一半就不动了, 要么是下载已经停住,
                // 无论哪种, 这条连接都只是在占着socket和聚合器缓冲。
                ctx.close();
                return;
            }
            ctx.fireUserEventTriggered(evt);
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
    public ResourcePackDownloadData generateOneTimeUrl(NetWorkUser user) {
        if (this.resourcePackBytes == null) return null;

        UUID uuid = user.uuid();
        if (uuid == null) return null;

        InetAddress address = user.address();
        boolean localhost = this.autoIp && !CraftEngine.instance().platform().hasProxy() && address != null && address.isLoopbackAddress();

        if (!this.useToken) {
            return new ResourcePackDownloadData(url(localhost) + "download", this.packUUID, this.packHash);
        }

        String token = UUID.randomUUID().toString();
        this.oneTimePackUrls.put(token, this.strictValidation ? uuid.toString().replace("-", "") : "");
        return new ResourcePackDownloadData(
                url(localhost) + "download?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8),
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