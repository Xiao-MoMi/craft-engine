package net.momirealms.craftengine.core.pack.host.impl;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.momirealms.craftengine.core.pack.host.*;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.HashUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

public final class OneDriveHost implements ResourcePackHost {
    public static final ResourcePackHostFactory<OneDriveHost> FACTORY = new Factory();
    private final String clientId;
    private final String clientSecret;
    private final String uploadPath;
    private final ReentrantLock tokenLock = new ReentrantLock();

    private String refreshToken;
    private String accessToken;
    private long expiresAt;

    private String cachedSha1;
    private String cachedFileId;

    public OneDriveHost(String clientId, String clientSecret, String refreshToken, String uploadPath) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
        this.uploadPath = uploadPath;

        this.readCacheFromDisk();
    }

    @Override
    public boolean canUpload() {
        return true;
    }

    @Override
    public ResourcePackHostType<OneDriveHost> type() {
        return ResourcePackHosts.ONEDRIVE;
    }

    @Override
    public CompletableFuture<List<ResourcePackDownloadData>> requestResourcePackDownloadLink(NetWorkUser user) {
        CompletableFuture<List<ResourcePackDownloadData>> future = new CompletableFuture<>();

        if (this.cachedFileId == null || this.cachedSha1 == null) {
            future.completeExceptionally(new IllegalStateException("OneDrive host is not initialized. Please upload first."));
            return future;
        }

        CraftEngine.instance().scheduler().executeAsync(() -> {
            try {
                String token = getOrRefreshAccessToken();
                if (token == null) {
                    future.completeExceptionally(new RuntimeException("Failed to obtain OneDrive access token"));
                    return;
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://graph.microsoft.com/v1.0/drive/items/" + this.cachedFileId))
                        .header("Authorization", "Bearer " + token)
                        .GET()
                        .build();

                HttpClientManager.get().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> handleDownloadLinkResponse(response, future))
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

    private void handleDownloadLinkResponse(HttpResponse<String> response, CompletableFuture<List<ResourcePackDownloadData>> future) {
        if (response.statusCode() != 200) {
            fail(future, "GetItem HTTP " + response.statusCode(), response.body());
            return;
        }

        try {
            JsonObject json = GsonHelper.parseJsonToJsonObject(response.body());
            if (json.has("@microsoft.graph.downloadUrl")) {
                String downloadUrl = json.get("@microsoft.graph.downloadUrl").getAsString();
                UUID uuid = UUID.nameUUIDFromBytes(this.cachedSha1.getBytes(StandardCharsets.UTF_8));
                future.complete(List.of(new ResourcePackDownloadData(downloadUrl, uuid, this.cachedSha1)));
            } else {
                fail(future, "Response missing download URL", response.body());
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
    }

    @Override
    public CompletableFuture<Void> upload(Path resourcePackPath) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        CraftEngine.instance().scheduler().executeAsync(() -> {
            try {
                String token = getOrRefreshAccessToken();
                if (token == null) {
                    future.completeExceptionally(new RuntimeException("Failed to obtain OneDrive access token"));
                    return;
                }

                String localSha1 = HashUtils.calculateLocalFileSha1(resourcePackPath);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://graph.microsoft.com/v1.0/drive/root:/" + this.uploadPath + ":/content"))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/octet-stream")
                        .PUT(HttpRequest.BodyPublishers.ofFile(resourcePackPath))
                        .build();

                HttpClientManager.get().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            if (response.statusCode() == 200 || response.statusCode() == 201) {
                                JsonObject json = GsonHelper.parseJsonToJsonObject(response.body());
                                this.cachedFileId = json.get("id").getAsString();
                                this.cachedSha1 = localSha1;
                                saveCacheToDisk();
                                future.complete(null);
                            } else {
                                fail(future, "Upload HTTP " + response.statusCode(), response.body());
                            }
                        })
                        .exceptionally(t -> {
                            future.completeExceptionally(t);
                            return null;
                        });

            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    @Nullable
    private String getOrRefreshAccessToken() throws IOException, InterruptedException {
        if (this.accessToken != null && System.currentTimeMillis() < expiresAt - 300000) { // 提前刷新
            return this.accessToken;
        }

        this.tokenLock.lock();
        try {
            if (this.accessToken != null && System.currentTimeMillis() < expiresAt - 300000) {
                return this.accessToken;
            }

            String formData = "client_id=" + URLEncoder.encode(this.clientId, StandardCharsets.UTF_8) +
                    "&client_secret=" + URLEncoder.encode(this.clientSecret, StandardCharsets.UTF_8) +
                    "&refresh_token=" + URLEncoder.encode(this.refreshToken, StandardCharsets.UTF_8) +
                    "&grant_type=refresh_token" +
                    "&scope=Files.ReadWrite.All+offline_access";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://login.microsoftonline.com/common/oauth2/v2.0/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> response = HttpClientManager.get().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                CraftEngine.instance().logger().warn("OneDrive Token Refresh Failed: " + response.body());
                return null;
            }

            JsonObject json = GsonHelper.parseJsonToJsonObject(response.body());
            this.accessToken = json.get("access_token").getAsString();
            this.refreshToken = json.get("refresh_token").getAsString();
            long expiresInSeconds = json.get("expires_in").getAsLong();
            this.expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L);

            saveCacheToDisk();
            return this.accessToken;
        } finally {
            this.tokenLock.unlock();
        }
    }

    private void fail(CompletableFuture<?> future, String reason, String body) {
        String msg = "OneDriveHost Error: " + reason + (body != null ? " | Body: " + body : "");
        future.completeExceptionally(new RuntimeException(msg));
    }

    private void readCacheFromDisk() {
        Path cachePath = CraftEngine.instance().dataFolderPath().resolve("cache").resolve("onedrive.json");
        if (!Files.exists(cachePath)) return;
        try (InputStreamReader isr = new InputStreamReader(Files.newInputStream(cachePath), StandardCharsets.UTF_8)) {
            Map<String, String> cache = GsonHelper.get().fromJson(isr, new TypeToken<Map<String, String>>(){}.getType());
            this.refreshToken = cache.getOrDefault("refresh-token", this.refreshToken);
            this.accessToken = cache.get("access-token");
            this.expiresAt = Long.parseLong(cache.getOrDefault("expires-at", "0"));
            this.cachedSha1 = cache.get("sha1");
            this.cachedFileId = cache.get("file-id");
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to load OneDrive cache", e);
        }
    }

    private void saveCacheToDisk() {
        Path cachePath = CraftEngine.instance().dataFolderPath().resolve("cache").resolve("onedrive.json");
        try {
            Files.createDirectories(cachePath.getParent());
            Map<String, String> cache = new HashMap<>();
            cache.put("refresh-token", this.refreshToken);
            cache.put("access-token", this.accessToken != null ? this.accessToken : "");
            cache.put("expires-at", String.valueOf(this.expiresAt));
            cache.put("sha1", this.cachedSha1 != null ? this.cachedSha1 : "");
            cache.put("file-id", this.cachedFileId != null ? this.cachedFileId : "");
            Files.writeString(cachePath, GsonHelper.get().toJson(cache), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Failed to persist OneDrive cache", e);
        }
    }

    private static class Factory implements ResourcePackHostFactory<OneDriveHost> {
        private static final String[] USE_ENVIRONMENT_VARIABLES = new String[] {"use_environment_variables", "use-environment-variables"};
        private static final String[] CLIENT_ID = new String[] {"client_id", "client-id"};
        private static final String[] CLIENT_SECRET = new String[] {"client_secret", "client-secret"};
        private static final String[] REFRESH_TOKEN = new String[] {"refresh_token", "refresh-token"};
        private static final String[] UPLOAD_PATH = new String[] {"upload_path", "upload-path"};

        @Override
        public OneDriveHost create(ConfigSection section) {
            boolean useEnv = section.getBoolean(USE_ENVIRONMENT_VARIABLES);
            String clientId = useEnv ? getNonNullEnvironmentVariable(section, "CE_ONEDRIVE_CLIENT_ID") : section.getNonEmptyString(CLIENT_ID);
            String clientSecret = useEnv ? getNonNullEnvironmentVariable(section, "CE_ONEDRIVE_CLIENT_SECRET") : section.getNonEmptyString(CLIENT_SECRET);
            String refreshToken = useEnv ? getNonNullEnvironmentVariable(section, "CE_ONEDRIVE_REFRESH_TOKEN") : section.getNonEmptyString(REFRESH_TOKEN);
            String uploadPath = section.getString(UPLOAD_PATH, "resource_pack.zip");
            return new OneDriveHost(clientId, clientSecret, refreshToken, uploadPath);
        }
    }
}