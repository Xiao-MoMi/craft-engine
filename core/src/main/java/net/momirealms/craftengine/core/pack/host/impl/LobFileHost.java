package net.momirealms.craftengine.core.pack.host.impl;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import net.momirealms.craftengine.core.pack.host.*;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class LobFileHost implements ResourcePackHost {
    public static final ResourcePackHostFactory<LobFileHost> FACTORY = new Factory();
    private final String apiKey;
    private AccountInfo accountInfo;

    private String cachedUrl;
    private String cachedSha1;

    public LobFileHost(String apiKey) {
        this.apiKey = apiKey;

        this.readCacheFromDisk();
    }

    @Override
    public boolean canUpload() {
        return true;
    }

    @Override
    public ResourcePackHostType<LobFileHost> type() {
        return ResourcePackHosts.LOBFILE;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public CompletableFuture<List<ResourcePackDownloadData>> requestResourcePackDownloadLink(NetWorkUser user) {
        if (this.cachedUrl == null || this.cachedSha1 == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        UUID uuid = UUID.nameUUIDFromBytes(this.cachedSha1.getBytes(StandardCharsets.UTF_8));
        return CompletableFuture.completedFuture(List.of(new ResourcePackDownloadData(this.cachedUrl, uuid, this.cachedSha1)));
    }

    @Override
    public CompletableFuture<Void> upload(Path resourcePackPath) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        CraftEngine.instance().scheduler().executeAsync(() -> {
            try {
                Map<String, String> hashes = calculateHashes(resourcePackPath);
                String sha1Hash = hashes.get("SHA-1");
                String sha256Hash = hashes.get("SHA-256");

                String boundary = "LobFileBoundary" + System.currentTimeMillis();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://lobfile.com/api/v3/upload.php"))
                        .header("X-API-Key", this.apiKey)
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(buildMultipartBody(resourcePackPath, sha256Hash, boundary))
                        .build();

                HttpClientManager.get().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> handleUploadResponse(response, sha1Hash, future))
                        .exceptionally(ex -> {
                            future.completeExceptionally(ex);
                            return null;
                        });

            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private void handleUploadResponse(HttpResponse<String> response, String localSha1, CompletableFuture<Void> future) {
        if (response.statusCode() != 200) {
            fail(future, "Upload HTTP " + response.statusCode(), response.body());
            return;
        }

        try {
            Map<String, Object> json = GsonHelper.parseJsonToMap(response.body());
            if (Boolean.TRUE.equals(json.get("success"))) {
                this.cachedUrl = (String) json.get("url");
                this.cachedSha1 = localSha1;
                saveCacheToDisk();

                fetchAccountInfo().whenComplete((info, t) -> {
                    if (info != null) {
                        Optional.ofNullable(getSpaceUsageText()).ifPresent(text -> {
                            CraftEngine.instance().logger().info(text);
                        });
                    }
                    future.complete(null);
                });
            } else {
                fail(future, "API returned error", (String) json.get("error"));
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
    }

    public CompletableFuture<AccountInfo> fetchAccountInfo() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://lobfile.com/api/v3/rest/get-account-info"))
                .header("X-API-Key", this.apiKey)
                .GET()
                .build();

        return HttpClientManager.get().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        AccountInfo info = GsonHelper.get().fromJson(response.body(), AccountInfo.class);
                        if (info.success()) {
                            this.accountInfo = info;
                            return info;
                        }
                    }
                    return null;
                });
    }

    private Map<String, String> calculateHashes(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

        try (InputStream is = Files.newInputStream(path);
             DigestInputStream dis1 = new DigestInputStream(is, sha1);
             DigestInputStream dis2 = new DigestInputStream(dis1, sha256)) {
            byte[] buffer = new byte[8192];
            while (dis2.read(buffer) != -1);
        }

        Map<String, String> hashes = new HashMap<>();
        hashes.put("SHA-1", HexFormat.of().formatHex(sha1.digest()));
        hashes.put("SHA-256", HexFormat.of().formatHex(sha256.digest()));
        return hashes;
    }

    private HttpRequest.BodyPublisher buildMultipartBody(Path filePath, String sha256Hash, String boundary) throws IOException {
        byte[] fileBytes = Files.readAllBytes(filePath);
        String fileName = filePath.getFileName().toString();

        StringBuilder sb = new StringBuilder();
        // File Part
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
        sb.append("Content-Type: application/octet-stream\r\n\r\n");
        byte[] part1 = sb.toString().getBytes(StandardCharsets.UTF_8);

        // SHA-256 Part
        sb.setLength(0);
        sb.append("\r\n--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"sha_256\"\r\n\r\n");
        sb.append(sha256Hash).append("\r\n");
        sb.append("--").append(boundary).append("--\r\n");
        byte[] part2 = sb.toString().getBytes(StandardCharsets.UTF_8);

        return HttpRequest.BodyPublishers.ofByteArrays(List.of(part1, fileBytes, part2));
    }

    @Nullable
    public String getSpaceUsageText() {
        if (this.accountInfo == null) return null;
        return TranslationManager.instance().plainTranslation(
                "host.lobfile.storage_usage",
                String.valueOf(this.accountInfo.account().usage().spaceUsed() / 1_000_000),
                String.valueOf(this.accountInfo.account().limits().spaceQuota() / 1_000_000),
                String.valueOf(this.accountInfo.account().usage().slotsUsed()),
                String.valueOf(this.accountInfo.account().limits().slotsQuota())
        );
    }

    private void fail(CompletableFuture<?> future, String reason, String body) {
        String msg = "LobFileHost Error: " + reason + (body != null ? " | Body: " + body : "");
        future.completeExceptionally(new RuntimeException(msg));
    }

    private void readCacheFromDisk() {
        Path cachePath = CraftEngine.instance().dataFolderPath().resolve("cache").resolve("lobfile.json");
        if (!Files.exists(cachePath)) return;
        try (InputStreamReader isr = new InputStreamReader(Files.newInputStream(cachePath), StandardCharsets.UTF_8)) {
            Map<String, String> cache = GsonHelper.get().fromJson(isr, new TypeToken<Map<String, String>>(){}.getType());
            this.cachedUrl = cache.get("url");
            this.cachedSha1 = cache.get("sha1");
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to load Lobfile cache", e);
        }
    }

    private void saveCacheToDisk() {
        Path cachePath = CraftEngine.instance().dataFolderPath().resolve("cache").resolve("lobfile.json");
        try {
            Files.createDirectories(cachePath.getParent());
            Map<String, String> cache = new HashMap<>();
            cache.put("url", this.cachedUrl != null ? this.cachedUrl : "");
            cache.put("sha1", this.cachedSha1 != null ? this.cachedSha1 : "");
            Files.writeString(cachePath, GsonHelper.get().toJson(cache), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Failed to persist Lobfile cache", e);
        }
    }

    private static class Factory implements ResourcePackHostFactory<LobFileHost> {
        private static final String[] USE_ENVIRONMENT_VARIABLES = new String[] {"use_environment_variables", "use-environment-variables"};
        private static final String[] API_KEY = new String[] {"api_key", "api-key"};

        @Override
        public LobFileHost create(ConfigSection section) {
            boolean useEnv = section.getBoolean(USE_ENVIRONMENT_VARIABLES);
            String apiKey = useEnv ? getNonNullEnvironmentVariable(section, "CE_LOBFILE_API_KEY") : section.getNonEmptyString(API_KEY);
            return new LobFileHost(apiKey);
        }
    }

    public record AccountInfo(
            boolean success,
            Account account
    ) {}

    public record Account(
            Info info,
            Limits limits,
            Usage usage
    ) {}

    public record Info(
            String email,
            String level,
            @SerializedName("api_key") String apiKey,
            @SerializedName("time_created") String timeCreated
    ) {}

    public record Limits(
            @SerializedName("space_quota") long spaceQuota,
            @SerializedName("slots_quota") long slotsQuota,
            @SerializedName("max_file_size") long maxFileSize,
            @SerializedName("max_file_download_speed") long maxFileDownloadSpeed
    ) {}

    public record Usage(
            @SerializedName("space_used") long spaceUsed,
            @SerializedName("slots_used") long slotsUsed
    ) {}
}