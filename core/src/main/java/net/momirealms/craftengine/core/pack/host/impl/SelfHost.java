package net.momirealms.craftengine.core.pack.host.impl;

import io.github.bucket4j.Bandwidth;
import net.momirealms.craftengine.core.pack.host.*;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SelfHost implements ResourcePackHost {
    public static final ResourcePackHostFactory<SelfHost> FACTORY = new Factory();
    private static final SelfHost INSTANCE = new SelfHost();

    public SelfHost() {
        SelfHostHttpServer.instance().readResourcePack(Config.fileToUpload());
    }

    @Override
    public CompletableFuture<List<ResourcePackDownloadData>> requestResourcePackDownloadLink(NetWorkUser user) {
        ResourcePackDownloadData data = SelfHostHttpServer.instance().generateOneTimeUrl(user.uuid());
        if (data == null) return CompletableFuture.completedFuture(List.of());
        return CompletableFuture.completedFuture(List.of(data));
    }

    @Override
    public CompletableFuture<Void> upload(Path resourcePackPath) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        CraftEngine.instance().scheduler().executeAsync(() -> {
            try {
                SelfHostHttpServer.instance().readResourcePack(resourcePackPath);
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public boolean canUpload() {
        return true;
    }

    @Override
    public ResourcePackHostType<SelfHost> type() {
        return ResourcePackHosts.SELF;
    }

    private static class Factory implements ResourcePackHostFactory<SelfHost> {
        private static final String[] ONE_TIME_TOKEN = new String[]{"one_time_token", "one-time-token"};
        private static final String[] DENY_NON_MINECRAFT_REQUEST = new String[]{"deny_non_minecraft_request", "deny-non-minecraft-request"};
        private static final String[] STRICT_VALIDATION = new String[]{"strict_validation", "strict-validation"};
        private static final String[] RATE_LIMITING = new String[]{"rate_limiting", "rate-limiting"};
        private static final String[] QPS_PER_IP = new String[]{"qps_per_ip", "qps-per-ip"};
        private static final String[] MAX_BANDWIDTH_PER_SECOND = new String[]{"max_bandwidth_per_second", "max-bandwidth-per-second"};
        private static final String[] MIN_DOWNLOAD_SPEED_PER_PLAYER = new String[]{"min_download_speed_per_player", "min-download-speed-per-player"};

        @Override
        public SelfHost create(ConfigSection section) {
            SelfHostHttpServer selfHostHttpServer = SelfHostHttpServer.instance();

            // url 拼接
            String ip = section.getNonEmptyString("ip");
            int port = section.getInt("port", 8163);
            if (port <= 0) {
                throw new KnownResourceException("number.greater_than", section.assemblePath("port"), "port", "0");
            } else if (port > 65535) {
                throw new KnownResourceException("number.less_than", section.assemblePath("port"), "port", "65536");
            }
            String url = section.getString("url", "");
            if (!url.isEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "http://" + url;
                }
                if (!url.endsWith("/")) url  += "/";
            }

            // 其他参数
            boolean oneTimeToken = section.getBoolean(ONE_TIME_TOKEN, true);
            String protocol = section.getString("protocol", "http");
            boolean denyNonMinecraftRequest = section.getBoolean(DENY_NON_MINECRAFT_REQUEST, true);
            boolean strictValidation = section.getBoolean(STRICT_VALIDATION);

            // 流量控制
            Bandwidth limit = null;
            ConfigSection rateLimitingSection = section.getSection(RATE_LIMITING);
            long maxBandwidthUsage = 0L;
            long minDownloadSpeed = 50_000L;
            if (rateLimitingSection != null) {
                ConfigValue qpsValue = rateLimitingSection.getValue(QPS_PER_IP);
                if (qpsValue != null) {
                    ConfigValue[] splitValues = qpsValue.splitValuesRestrict("/", 2);
                    int maxRequests = splitValues[0].getAsInt();
                    int resetInterval = splitValues[1].getAsInt();
                    limit = Bandwidth.builder()
                            .capacity(maxRequests)
                            .refillGreedy(maxRequests, Duration.ofSeconds(resetInterval))
                            .build();
                }
                maxBandwidthUsage = section.getLong(MAX_BANDWIDTH_PER_SECOND, 0);
                minDownloadSpeed = section.getLong(MIN_DOWNLOAD_SPEED_PER_PLAYER, 50_000);
            }

            // 更新单例
            selfHostHttpServer.updateProperties(ip, port, url, denyNonMinecraftRequest, protocol, limit, oneTimeToken, maxBandwidthUsage, minDownloadSpeed, strictValidation);
            return INSTANCE;
        }
    }
}
