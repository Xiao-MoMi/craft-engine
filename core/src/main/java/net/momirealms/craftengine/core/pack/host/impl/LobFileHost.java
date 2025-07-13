package net.momirealms.craftengine.core.pack.host.impl;

import com.google.gson.reflect.TypeToken;
import net.momirealms.craftengine.core.pack.host.ResourcePackDownloadData;
import net.momirealms.craftengine.core.pack.host.ResourcePackHost;
import net.momirealms.craftengine.core.pack.host.ResourcePackHostFactory;
import net.momirealms.craftengine.core.pack.host.ResourcePackHosts;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.LocalizedException;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class LobFileHost implements ResourcePackHost {
    public static final Factory FACTORY = new Factory();
    private final String apiKey;
    private final ProxySelector proxy;
    private AccountInfo accountInfo;

    private String url;
    private String sha1;
    private UUID uuid;

    public LobFileHost(String apiKey, ProxySelector proxy) {
        this.apiKey = apiKey;
        this.proxy = proxy;
        this.readCacheFromDisk();
    }

    @Override
    public boolean canUpload() {
        return true;
    }

    @Override
    public Key type() {
        return ResourcePackHosts.LOBFILE;
    }

    public void readCacheFromDisk() {
        Path cachePath = CraftEngine.instance().dataFolderPath().resolve("cache").resolve("lobfile.json");
        if (!Files.exists(cachePath) || !Files.isRegularFile(cachePath)) return;
        try (InputStream is = Files.newInputStream(cachePath)) {
            Map<String, String> cache = GsonHelper.get().fromJson(
                    new InputStreamReader(is),
                    new TypeToken<Map<String, String>>(){}.getType()
            );
            this.url = cache.get("url");
            this.sha1 = cache.get("sha1");
            String uuidString = cache.get("uuid");
            if (uuidString != null && !uuidString.isEmpty()) {
                this.uuid = UUID.fromString(uuidString);
            }
            CraftEngine.instance().logger().info("[LobFile] Loaded cached resource pack info");
        } catch (Exception e) {
            CraftEngine.instance().logger().warn(
                    "[LobFile] Failed to read cache file: " + e.getMessage());
        }
    }

    public void saveCacheToDisk() {
        Map<String, String> cache = new HashMap<>();
        cache.put("url", this.url);
        cache.put("sha1", this.sha1);
        cache.put("uuid", this.uuid != null ? this.uuid.toString() : "");
        Path cachePath = CraftEngine.instance().dataFolderPath().resolve("cache").resolve("lobfile.json");
        try {
            Files.createDirectories(cachePath.getParent());
            Files.writeString(
                    cachePath,
                    GsonHelper.get().toJson(cache),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            CraftEngine.instance().logger().warn(
                    "[LobFile] Failed to save cache: " + e.getMessage());
        }
    }

    public String getSpaceUsageText() {
        if (this.accountInfo == null) return "Usage data not available";
        return String.format("Storage: %d/%d MB (%.1f%% used)",
                this.accountInfo.getSpaceUsed() / 1_000_000,
                this.accountInfo.getSpaceQuota() / 1_000_000,
                (this.accountInfo.getSpaceUsed() * 100.0) / this.accountInfo.getSpaceQuota()
        );
    }

    @Override
    public CompletableFuture<List<ResourcePackDownloadData>> requestResourcePackDownloadLink(UUID player) {
        if (url == null) return CompletableFuture.completedFuture(Collections.emptyList());
        return CompletableFuture.completedFuture(List.of(ResourcePackDownloadData.of(this.url, this.uuid, this.sha1)));
    }

    @Override
    public CompletableFuture<Void> upload(Path resourcePackPath) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        long totalStartTime = System.currentTimeMillis();
        CraftEngine.instance().scheduler().executeAsync(() -> {
            try {
                Map<String, String> hashes = calculateHashes(resourcePackPath);
                String sha1Hash = hashes.get("SHA-1");
                String sha256Hash = hashes.get("SHA-256");

                try (HttpClient client = HttpClient.newBuilder().proxy(this.proxy).build()) {
                    String boundary = UUID.randomUUID().toString();

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://lobfile.com/api/v3/upload.php"))
                            .header("X-API-Key", this.apiKey)
                            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                            .POST(buildMultipartBody(resourcePackPath, sha256Hash, boundary))
                            .build();

                    long uploadStart = System.currentTimeMillis();
                    CraftEngine.instance().logger().info("[LobFile] Starting file upload...");

                    client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                            .thenAccept(response -> {
                                long uploadTime = System.currentTimeMillis() - uploadStart;
                                CraftEngine.instance().logger().info(
                                        "[LobFile] Upload request completed in " + uploadTime + "ms");

                                handleUploadResponse(response, future, sha1Hash);
                            })
                            .exceptionally(ex -> {
                                long totalTime = System.currentTimeMillis() - totalStartTime;
                                CraftEngine.instance().logger().severe(
                                        "[LobFile] Upload failed after " + totalTime + "ms", ex);
                                future.completeExceptionally(ex);
                                return null;
                            });
                }
            } catch (IOException | NoSuchAlgorithmException e) {
                long totalTime = System.currentTimeMillis() - totalStartTime;
                CraftEngine.instance().logger().severe(
                        "[LobFile] Upload preparation failed after " + totalTime + "ms", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<AccountInfo> fetchAccountInfo() {
        try (HttpClient client = HttpClient.newBuilder().proxy(this.proxy).build()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://lobfile.com/api/v3/rest/get-account-info"))
                    .header("X-API-Key", this.apiKey)
                    .GET()
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            AccountInfo info = GsonHelper.get().fromJson(response.body(), AccountInfo.class);
                            if (info.isSuccess()) {
                                this.accountInfo = info;
                                return info;
                            }
                        }
                        throw new RuntimeException("Failed to fetch account info: " + response.statusCode());
                    });
        }
    }

    @SuppressWarnings("all")
    private Map<String, String> calculateHashes(Path path) throws IOException, NoSuchAlgorithmException {
        Map<String, String> hashes = new HashMap<>();
        MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
        MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");

        try (InputStream is = Files.newInputStream(path);
             DigestInputStream dis = new DigestInputStream(is, sha1Digest)) {
            DigestInputStream dis2 = new DigestInputStream(dis, sha256Digest);

            while (dis2.read() != -1) ;

            hashes.put("SHA-1", bytesToHex(sha1Digest.digest()));
            hashes.put("SHA-256", bytesToHex(sha256Digest.digest()));
        }
        return hashes;
    }

    private HttpRequest.BodyPublisher buildMultipartBody(Path filePath, String sha256Hash, String boundary) throws IOException {
        List<byte[]> parts = new ArrayList<>();
        String filePartHeader = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + filePath.getFileName() + "\"\r\n" +
                "Content-Type: application/octet-stream\r\n\r\n";
        parts.add(filePartHeader.getBytes());

        parts.add(Files.readAllBytes(filePath));
        parts.add("\r\n".getBytes());

        String sha256Part = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"sha_256\"\r\n\r\n" +
                sha256Hash + "\r\n";
        parts.add(sha256Part.getBytes());

        String endBoundary = "--" + boundary + "--\r\n";
        parts.add(endBoundary.getBytes());

        return HttpRequest.BodyPublishers.ofByteArrays(parts);
    }

    private void handleUploadResponse(
            HttpResponse<String> response,
            CompletableFuture<Void> future,
            String localSha1
    ) {
        try {
            if (response.statusCode() == 200) {
                Map<String, Object> json = GsonHelper.parseJsonToMap(response.body());
                if (Boolean.TRUE.equals(json.get("success"))) {
                    this.url = (String) json.get("url");
                    this.sha1 = localSha1;
                    this.uuid = UUID.randomUUID();
                    saveCacheToDisk();
                    CraftEngine.instance().logger().info("[LobFile] Upload success! Resource pack URL: " + this.url);
                    fetchAccountInfo()
                            .thenAccept(info -> {
                                CraftEngine.instance().logger().info("[LobFile] Account usage updated: " + getSpaceUsageText());
                                future.complete(null);
                            })
                            .exceptionally(ex -> {
                                CraftEngine.instance().logger().warn("[LobFile] Usage check failed (upload still succeeded): ", ex);
                                future.complete(null);
                                return null;
                            });
                } else {
                    future.completeExceptionally(new RuntimeException((String) json.get("error")));
                }
            } else {
                future.completeExceptionally(new RuntimeException("Upload failed: " + response.statusCode()));
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static class Factory implements ResourcePackHostFactory {

        @Override
        public ResourcePackHost create(Map<String, Object> arguments) {
            boolean useEnv = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("use-environment-variables", false), "use-environment-variables");
            String apiKey = useEnv ? System.getenv("CE_LOBFILE_API_KEY") : Optional.ofNullable(arguments.get("api-key")).map(String::valueOf).orElse(null);
            if (apiKey == null || apiKey.isEmpty()) {
                throw new LocalizedException("warning.config.host.lobfile.missing_api_key");
            }
            ProxySelector proxy = getProxySelector(MiscUtils.castToMap(arguments.get("proxy"), true));
            return new LobFileHost(apiKey, proxy);
        }
    }

    @SuppressWarnings({"all"})
    public static class AccountInfo {
        private boolean success;
        private Map<String, Object> account_info;
        private Map<String, Integer> account_limits;
        private Map<String, Integer> account_usage;

        public String getEmail() {
            return (String) this.account_info.get("email");
        }

        public int getSpaceQuota() {
            return this.account_limits.getOrDefault("space_quota", 0);
        }

        public int getSpaceUsed() {
            return this.account_usage.getOrDefault("space_used", 0);
        }

        public int getSlotsUsed() {
            return this.account_usage.getOrDefault("slots_used", 0);
        }

        public boolean isSuccess() {
            return this.success;
        }
    }
}