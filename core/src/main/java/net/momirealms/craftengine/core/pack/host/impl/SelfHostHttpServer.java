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
import io.netty.handler.traffic.GlobalChannelTrafficShapingHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import net.momirealms.craftengine.core.pack.host.HttpClientManager;
import net.momirealms.craftengine.core.pack.host.ResourcePackDownloadData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.util.Pair;
import net.momirealms.craftengine.core.util.UUIDUtils;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.*;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public final class SelfHostHttpServer {
    private static final int REQUEST_TIMEOUT_SECONDS = 30;
    private static final int MAX_CONNECTIONS = 1024;
    private static final URI CLOUDFLARE = URI.create("https://www.cloudflare.com/cdn-cgi/trace");
    private static final URI CLOUDFLARE_CN = URI.create("https://www.cloudflare-cn.com/cdn-cgi/trace");
    private static final String LOCALHOST = "localhost";
    private static SelfHostHttpServer instance;
    private static String IP_CACHE = null;
    private final Semaphore connectionSlots = new Semaphore(MAX_CONNECTIONS);
    private final Cache<String, DownloadToken> oneTimePackUrls = Caffeine.newBuilder()
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
    private final AtomicLong totalConnections = new AtomicLong();
    private final AtomicLong blockedConnections = new AtomicLong();
    // Download callbacks must not wait for the monitor held during pack loading and server reloads.
    private final Object bandwidthLock = new Object();
    private final ChannelGroup activeDownloadChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private final ChannelGroup connections = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private volatile Bandwidth limitPerIp = Bandwidth.builder()
            .capacity(1)
            .refillGreedy(1, Duration.ofSeconds(1))
            .initialTokens(1)
            .build();
    private volatile String ip = "localhost";
    private volatile int port = -1;
    private volatile String protocol = "http";
    private volatile String url;
    private volatile boolean denyNonMinecraft = true;
    private volatile boolean useToken;
    private volatile boolean strictValidation = false;
    private volatile boolean useServerPort = false;
    private volatile boolean autoIp = false;
    private volatile boolean enabled = false;
    private volatile String forwardSecret;
    private volatile long globalUploadRateLimit = 0;
    private volatile long minDownloadSpeed = 50_000;
    private volatile GlobalChannelTrafficShapingHandler trafficShapingHandler;
    private ScheduledExecutorService virtualTrafficExecutor;
    private volatile Map<String, HostedPack> packs = new ConcurrentHashMap<>();
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

    private static String getIp() {
        if (IP_CACHE == null || LOCALHOST.equals(IP_CACHE)) {
            boolean inChina = Locale.getDefault() == Locale.SIMPLIFIED_CHINESE;
            IP_CACHE = fetchIp(inChina ? CLOUDFLARE_CN : CLOUDFLARE);
            if (LOCALHOST.equals(IP_CACHE)) {
                IP_CACHE = fetchIp(inChina ? CLOUDFLARE : CLOUDFLARE_CN);
            }
        }
        return IP_CACHE;
    }

    private static String fetchIp(URI uri) {
        HttpRequest request = HttpClientManager.requestBuilder().uri(uri).timeout(java.time.Duration.ofSeconds(10)).GET().build();
        java.net.http.HttpResponse<String> response;
        try {
            response = HttpClientManager.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            CraftEngine.instance().logger().warn("Failed to automatically obtain an IP address. Uri: " + uri, e);
            return LOCALHOST;
        }
        Properties props = new Properties();
        try {
            props.load(new StringReader(response.body()));
        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Failed to automatically obtain an IP address. Uri: " + uri + " Body: " + response.body(), e);
            return LOCALHOST;
        }
        if (!props.containsKey("ip")) {
            CraftEngine.instance().logger().warn("Failed to automatically obtain an IP address. Uri: " + uri + " Body: " + response.body());
            return LOCALHOST;
        }
        try {
            Pair<String, String> ip = verifyIp(props.getProperty("ip"));
            return ip.left();
        } catch (UnknownHostException e) {
            CraftEngine.instance().logger().warn("Failed to automatically obtain an IP address. Invalid IP address. Uri: " + uri + " Body: " + response.body());
            return LOCALHOST;
        }
    }

    private static Pair<String, String> verifyIp(String ip) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(ip);
        String verifiedIp = address.getHostAddress();
        if (address instanceof Inet6Address) {
            return Pair.of("[" + verifiedIp + "]", verifiedIp);
        }
        return Pair.of(verifiedIp, verifiedIp);
    }

    public synchronized void load(ConfigSection section, Map<String, Path> packPaths) {
        // url 拼接
        boolean autoIp = false;
        String ip = section.getString("ip", "auto");
        if ("auto".equalsIgnoreCase(ip)) {
            ip = getIp();
            autoIp = true;
        }

        int port;
        boolean useServerPort = false;
        if ("auto".equals(section.getString("port", "auto"))) {
            port = -1;
            useServerPort = true;
        } else {
            port = section.getInt("port", 8163);
            if (port <= 0) {
                throw new KnownResourceException("number.greater_than", section.assemblePath("port"), "port", "0");
            } else if (port > 65535) {
                throw new KnownResourceException("number.less_than", section.assemblePath("port"), "port", "65536");
            }
        }
        String url = section.getString("url", "");
        if (!url.isEmpty()) {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            if (!url.endsWith("/")) url += "/";
        }

        // 其他参数
        boolean oneTimeToken = section.getBoolean("one_time_token", true);
        String protocol = section.getString("protocol", "http");
        boolean denyNonMinecraftRequest = section.getBoolean("deny_non_minecraft_request", true);
        boolean strictValidation = section.getBoolean("strict_validation");

        // 流量控制
        Bandwidth limit = null;
        ConfigSection rateLimitingSection = section.getSection("rate_limiting");
        long maxBandwidthUsage = 0L;
        long minDownloadSpeed = 50_000L;
        if (rateLimitingSection != null) {
            ConfigValue qpsValue = rateLimitingSection.getValue("qps_per_ip");
            if (qpsValue != null) {
                ConfigValue[] splitValues = qpsValue.splitValuesRestrict("/", 2);
                int maxRequests = splitValues[0].getAsInt();
                int resetInterval = splitValues[1].getAsInt();
                limit = Bandwidth.builder()
                        .capacity(maxRequests)
                        .refillGreedy(maxRequests, Duration.ofSeconds(resetInterval))
                        .build();
            }
            maxBandwidthUsage = rateLimitingSection.getLong("max_bandwidth_per_second", 0);
            minDownloadSpeed = rateLimitingSection.getLong("min_download_speed_per_player", 50_000);
        }

        String forwardSecret = section.getString("forward_secret");

        Map<String, HostedPack> loadedPacks = new ConcurrentHashMap<>();
        for (Map.Entry<String, Path> entry : packPaths.entrySet()) {
            try {
                if (Files.isRegularFile(entry.getValue())) {
                    loadedPacks.put(entry.getKey(), readResourcePack(entry.getValue()));
                }
            } catch (IOException e) {
                CraftEngine.instance().logger().warn("Failed to load self-hosted resource pack " + entry.getKey(), e);
            }
        }

        // 更新单例
        updateProperties(
                ip, port, url, denyNonMinecraftRequest,
                protocol, limit, oneTimeToken,
                maxBandwidthUsage, minDownloadSpeed, strictValidation,
                useServerPort, autoIp, forwardSecret
        );
        this.packs = loadedPacks;
        this.oneTimePackUrls.asMap().values().removeIf(token -> {
            HostedPack current = loadedPacks.get(token.packId());
            return current == null || !token.hash().equals(current.hash());
        });
    }

    private void updateProperties(String ip,
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
        boolean reuseServer = this.enabled && this.useServerPort == useServerPort && this.port == port;
        if (!reuseServer) disable();
        this.ip = ip;
        this.autoIp = autoIp;
        this.url = url;
        this.denyNonMinecraft = denyNonMinecraft;
        this.protocol = protocol;
        if (!Objects.equals(this.limitPerIp, limitPerIp)) {
            this.limitPerIp = limitPerIp;
            this.ipRateLimiters.invalidateAll();
        }
        if (this.useToken != token || this.strictValidation != strictValidation) {
            this.oneTimePackUrls.invalidateAll();
        }
        this.useToken = token;
        this.strictValidation = strictValidation;
        this.useServerPort = useServerPort;
        this.forwardSecret = forwardSecret == null || forwardSecret.isBlank() ? null : forwardSecret;
        if (this.globalUploadRateLimit != globalUploadRateLimit || this.minDownloadSpeed != minDownloadSpeed) {
            this.globalUploadRateLimit = globalUploadRateLimit;
            this.minDownloadSpeed = minDownloadSpeed;
            if (this.trafficShapingHandler != null) {
                long initSize = globalUploadRateLimit <= 0 ? 0 : Math.max(minDownloadSpeed, globalUploadRateLimit);
                this.trafficShapingHandler.setWriteLimit(initSize);
                rebalanceBandwidth();
            }
        }
        if (reuseServer) return;
        this.port = port;
        try {
            if (useServerPort) {
                initializeServerPortHost();
            } else {
                initializeServer();
            }
        } catch (Exception e) {
            disable();
            throw new IllegalStateException("Failed to start self-host HTTP server", e);
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
        CraftEngine.instance().networkManager().setServerPortHost(this::initializeHttpPipeline);
        this.enabled = true;
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
                        initializeHttpPipeline(ch.pipeline());
                    }
                });
        try {
            this.serverChannel = b.bind(this.port).sync().channel();
            CraftEngine.instance().logger().info(TranslationManager.instance().plainTranslation("host.self.http_server_started", String.valueOf(this.port)));
            this.enabled = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while starting self-host HTTP server", e);
        }
    }

    void initializeHttpPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(new ConnectionHandler());
        if (this.trafficShapingHandler != null) {
            pipeline.addLast("trafficShaping", this.trafficShapingHandler);
        }
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new RequestHandler());
    }

    private void rebalanceBandwidth() {
        synchronized (this.bandwidthLock) {
            GlobalChannelTrafficShapingHandler handler = this.trafficShapingHandler;
            if (handler == null) return;
            long globalLimit = this.globalUploadRateLimit;
            if (globalLimit == 0) {
                handler.setWriteChannelLimit(0);
                return;
            }

            int activeCount = this.activeDownloadChannels.size();
            if (activeCount == 0) {
                handler.setWriteChannelLimit(globalLimit);
                return;
            }

            // 计算平均带宽：全局总量 / 当前人数
            long fairRate = globalLimit / activeCount;

            // 确保不低于最小保障速率（可选，防止除法导致过小）
            fairRate = Math.max(fairRate, this.minDownloadSpeed);

            // 更新 Handler 配置
            handler.setWriteChannelLimit(fairRate);
        }
    }

    @Nullable
    public ResourcePackDownloadData generateOneTimeUrl(NetWorkUser user, String packId) {
        if (!this.enabled) return null;
        HostedPack pack = this.packs.get(packId);
        if (pack == null) return null;

        UUID uuid = user.uuid();
        if (uuid == null) return null;

        InetAddress address = user.address();
        boolean localhost = this.autoIp && !CraftEngine.instance().platform().hasProxy() && address != null && address.isLoopbackAddress();

        if (!this.useToken) {
            return new ResourcePackDownloadData(url(localhost) + "download/" + packId, pack.uuid(), pack.hash());
        }

        String token = UUID.randomUUID().toString();
        this.oneTimePackUrls.put(token, new DownloadToken(packId, this.strictValidation ? uuid.toString().replace("-", "") : "", pack.hash()));
        return new ResourcePackDownloadData(
                url(localhost) + "download/" + packId + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8),
                pack.uuid(),
                pack.hash()
        );
    }

    public synchronized void disable() {
        if (this.useServerPort && this.enabled) {
            CraftEngine.instance().networkManager().setServerPortHost(null);
        }
        this.enabled = false;
        this.connections.close();
        this.activeDownloadChannels.clear();
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
        if (this.serverChannel != null) {
            this.serverChannel.close().awaitUninterruptibly();
            this.serverChannel = null;
        }
        if (this.bossGroup != null) {
            this.bossGroup.shutdownGracefully();
            this.bossGroup = null;
        }
        if (this.workerGroup != null) {
            this.workerGroup.shutdownGracefully();
            this.workerGroup = null;
        }
        this.ipRateLimiters.invalidateAll();
    }

    public synchronized void clearPacks() {
        this.packs.clear();
        this.oneTimePackUrls.invalidateAll();
    }

    public synchronized void readResourcePack(String packId, Path path) throws IOException {
        HostedPack pack = readResourcePack(path);
        this.packs.put(packId, pack);
        this.oneTimePackUrls.asMap().values().removeIf(token -> token.packId().equals(packId) && !token.hash().equals(pack.hash()));
    }

    private HostedPack readResourcePack(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(bytes);
            byte[] hashBytes = digest.digest();

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            String hash = hexString.toString();
            return new HostedPack(bytes, hash, UUID.nameUUIDFromBytes(hash.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 algorithm not available", e);
        }
    }

    private record HostedPack(byte[] bytes, String hash, UUID uuid) {
    }

    private record DownloadToken(String packId, String playerId, String hash) {
    }

    private class ConnectionHandler extends ChannelInboundHandlerAdapter {
        private ScheduledFuture<?> requestTimeout;
        private boolean acquired;

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            SelfHostHttpServer.this.totalConnections.incrementAndGet();
            // Limit admission before incomplete requests can occupy the shared connection slots.
            String clientIp = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
            if (!checkIpRateLimit(clientIp)) {
                SelfHostHttpServer.this.blockedConnections.incrementAndGet();
                ctx.close();
                return;
            }
            this.acquired = SelfHostHttpServer.this.connectionSlots.tryAcquire();
            if (!this.acquired) {
                SelfHostHttpServer.this.blockedConnections.incrementAndGet();
                ctx.close();
                return;
            }
            SelfHostHttpServer.this.connections.add(ctx.channel());
            // An absolute deadline also catches clients that keep sending individual bytes.
            this.requestTimeout = ctx.executor().schedule(() -> {
                SelfHostHttpServer.this.blockedConnections.incrementAndGet();
                ctx.close();
            }, REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        private boolean checkIpRateLimit(String clientIp) {
            Bandwidth limit = SelfHostHttpServer.this.limitPerIp;
            if (limit == null) return true;
            Bucket rateLimiter = SelfHostHttpServer.this.ipRateLimiters.get(clientIp, k -> Bucket.builder().addLimit(limit).build());
            assert rateLimiter != null;
            return rateLimiter.tryConsume(1);
        }

        private void receivedRequest(ChannelHandlerContext ctx) {
            cancelTimeout();
            // Serve one request per connection. Do not buffer pipelined requests during a download.
            ctx.channel().config().setAutoRead(false);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
            if (!this.acquired || !ctx.channel().isActive()) {
                ReferenceCountUtil.release(message);
                return;
            }
            super.channelRead(ctx, message);
        }

        private void cancelTimeout() {
            if (this.requestTimeout != null) {
                this.requestTimeout.cancel(false);
                this.requestTimeout = null;
            }
        }

        private void release() {
            cancelTimeout();
            if (this.acquired) {
                this.acquired = false;
                SelfHostHttpServer.this.connectionSlots.release();
            }
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) {
            release();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            release();
            super.channelInactive(ctx);
        }
    }

    private class RequestHandler extends SimpleChannelInboundHandler<HttpObject> {
        private boolean receivedRequest;

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            ctx.close();
        }

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
        protected void channelRead0(ChannelHandlerContext ctx, HttpObject message) {
            if (this.receivedRequest || !(message instanceof io.netty.handler.codec.http.HttpRequest request)) return;
            this.receivedRequest = true;
            ctx.pipeline().get(ConnectionHandler.class).receivedRequest(ctx);
            SelfHostHttpServer.this.totalRequests.incrementAndGet();

            try {
                if (!request.decoderResult().isSuccess()) {
                    sendError(ctx, HttpResponseStatus.BAD_REQUEST, "Bad Request");
                    return;
                }
                if (!HttpMethod.GET.equals(request.method())) {
                    sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED, "Method Not Allowed");
                    return;
                }
                // These endpoints never consume a body. Reject its headers before buffering any data.
                if (request.headers().contains(HttpHeaderNames.TRANSFER_ENCODING)
                        || HttpUtil.getContentLength(request, 0) != 0) {
                    sendError(ctx, HttpResponseStatus.BAD_REQUEST, "Request body not supported");
                    return;
                }
                QueryStringDecoder queryDecoder = new QueryStringDecoder(request.uri());
                String path = queryDecoder.path();
                String forwardSecret = SelfHostHttpServer.this.forwardSecret;

                if (path.startsWith("/download/")) {
                    handleDownload(ctx, request, queryDecoder, path.substring("/download/".length()));
                } else if ("/metrics".equals(path)) {
                    handleMetrics(ctx);
                } else if (forwardSecret != null && !forwardSecret.isBlank() && path.startsWith("/forward/")) {
                    handleForward(ctx, request, path.substring("/forward/".length()));
                } else {
                    sendError(ctx, HttpResponseStatus.NOT_FOUND, "Not Found");
                }
            } catch (Exception e) {
                CraftEngine.instance().logger().warn("Request handling failed", e);
                sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Error");
            }
        }

        private void handleDownload(ChannelHandlerContext ctx, io.netty.handler.codec.http.HttpRequest request, QueryStringDecoder queryDecoder, String packId) {
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
            HostedPack pack = SelfHostHttpServer.this.packs.get(packId);
            if (pack == null) {
                sendError(ctx, HttpResponseStatus.NOT_FOUND, "Pack Not Found");
                SelfHostHttpServer.this.blockedRequests.incrementAndGet();
                return;
            }

            // Consume the token only after all checks pass, against the exact version being sent.
            if (SelfHostHttpServer.this.useToken) {
                String token = queryDecoder.parameters().getOrDefault("token", Collections.emptyList()).stream().findFirst().orElse(null);
                String clientUUID = SelfHostHttpServer.this.strictValidation ? request.headers().get("X-Minecraft-UUID") : null;
                if (!validateToken(token, clientUUID, packId, pack)) {
                    sendError(ctx, HttpResponseStatus.FORBIDDEN, "Forbidden");
                    SelfHostHttpServer.this.blockedRequests.incrementAndGet();
                    return;
                }
            }

            // 新人来了，所有人的速度上限降低
            if (!SelfHostHttpServer.this.activeDownloadChannels.contains(ctx.channel())) {
                SelfHostHttpServer.this.activeDownloadChannels.add(ctx.channel());
                rebalanceBandwidth();
            }

            // 告诉客户端资源包大小
            long fileLength = pack.bytes().length;
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            HttpUtil.setContentLength(response, fileLength);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/zip");
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            ctx.write(response);

            // 发送分段资源包
            ChunkedStream chunkedStream = new ChunkedStream(new ByteArrayInputStream(pack.bytes()), 8192);
            HttpChunkedInput httpChunkedInput = new HttpChunkedInput(chunkedStream);
            ChannelFuture sendFileFuture = ctx.writeAndFlush(httpChunkedInput);
            sendFileFuture.addListener(ChannelFutureListener.CLOSE);

            // 下载结束后移除计数
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
                    + "blocked_requests " + SelfHostHttpServer.this.blockedRequests.get() + "\n"
                    + "# TYPE total_connections counter\n"
                    + "total_connections " + SelfHostHttpServer.this.totalConnections.get() + "\n"
                    + "# TYPE blocked_connections counter\n"
                    + "blocked_connections " + SelfHostHttpServer.this.blockedConnections.get();

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

        private void handleForward(ChannelHandlerContext ctx, io.netty.handler.codec.http.HttpRequest request, String packId) {
            String secret = request.headers().get("secret");
            String expectedSecret = SelfHostHttpServer.this.forwardSecret;
            if (expectedSecret == null || expectedSecret.isBlank() || !expectedSecret.equals(secret)) {
                sendError(ctx, HttpResponseStatus.UNAUTHORIZED, "Unauthorized");
                return;
            }
            HostedPack pack = SelfHostHttpServer.this.packs.get(packId);
            if (pack == null) {
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
                SelfHostHttpServer.this.oneTimePackUrls.put(token, new DownloadToken(packId, SelfHostHttpServer.this.strictValidation ? uuid.replace("-", "") : "", pack.hash()));
                jsonObject.addProperty("url", SelfHostHttpServer.this.url(false) + "download/" + packId + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8));
            } else {
                jsonObject.addProperty("url", SelfHostHttpServer.this.url(false) + "download/" + packId);
            }
            jsonObject.addProperty("uuid", pack.uuid().toString());
            jsonObject.addProperty("hash", pack.hash());
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

        private boolean validateToken(String token, String clientUUID, String packId, HostedPack pack) {
            if (token == null || token.length() != 36) return false;
            DownloadToken valid = SelfHostHttpServer.this.oneTimePackUrls.getIfPresent(token);
            boolean isValid = valid != null && valid.packId().equals(packId) && valid.hash().equals(pack.hash())
                    && (!SelfHostHttpServer.this.strictValidation || Objects.equals(valid.playerId(), clientUUID));
            if (isValid) {
                return SelfHostHttpServer.this.oneTimePackUrls.asMap().remove(token, valid);
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
    }
}
