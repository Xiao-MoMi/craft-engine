package net.momirealms.craftengine.core.pack.host.impl;

import com.google.gson.JsonArray;
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
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

public final class DropboxHost implements ResourcePackHost {
    public static final ResourcePackHostFactory<DropboxHost> FACTORY = new Factory();
    private final String appKey;
    private final String appSecret;
    private final String uploadPath;
    private final ReentrantLock tokenLock = new ReentrantLock();

    private volatile String accessToken;
    private volatile String refreshToken;
    private volatile long expiresAt;
    private String cachedUrl;
    private String cachedSha1;

    public DropboxHost(String appKey, String appSecret, String refreshToken, String uploadPath) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.refreshToken = refreshToken;
        this.uploadPath = uploadPath.startsWith("/") ? uploadPath : "/" + uploadPath;

        this.readCacheFromDisk();
    }

    @Override
    public boolean canUpload() {
        return true;
    }

    @Override
    public ResourcePackHostType<DropboxHost> type() {
        return ResourcePackHosts.DROPBOX;
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
                String token = getOrRefreshToken();
                if (token == null) {
                    future.completeExceptionally(new RuntimeException("Failed to obtain Dropbox access token"));
                    return;
                }

                String localSha1 = HashUtils.calculateLocalFileSha1(resourcePackPath);
                JsonObject apiArg = new JsonObject();
                apiArg.addProperty("path", this.uploadPath);
                apiArg.addProperty("mode", "overwrite");

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://content.dropboxapi.com/2/files/upload"))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/octet-stream")
                        .header("Dropbox-API-Arg", GsonHelper.get().toJson(apiArg))
                        .POST(HttpRequest.BodyPublishers.ofFile(resourcePackPath))
                        .build();

                HttpClientManager.get().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> handleUploadResponse(response, localSha1, token, future))
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

    private void handleUploadResponse(HttpResponse<String> response, String localSha1, String token, CompletableFuture<Void> future) {
        if (response.statusCode() != 200) {
            fail(future, "Upload HTTP " + response.statusCode(), response.body());
            return;
        }

        try {
            this.cachedSha1 = localSha1;
            this.cachedUrl = fetchDirectDownloadUrl(token);
            saveCacheToDisk();
            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
    }

    private String fetchDirectDownloadUrl(String token) throws IOException, InterruptedException {
        JsonObject createLink = new JsonObject();
        createLink.addProperty("path", this.uploadPath);
        createLink.add("settings", new JsonObject());
        createLink.getAsJsonObject("settings").addProperty("requested_visibility", "public");

        HttpRequest createLinkRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.dropboxapi.com/2/sharing/create_shared_link_with_settings"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GsonHelper.get().toJson(createLink)))
                .build();

        HttpResponse<String> response = HttpClientManager.get().send(createLinkRequest, HttpResponse.BodyHandlers.ofString());

        try {
            if (response.statusCode() == 409) {
                JsonObject listLinks = new JsonObject();
                listLinks.addProperty("path", this.uploadPath);

                HttpRequest listLinksRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.dropboxapi.com/2/sharing/list_shared_links"))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(GsonHelper.get().toJson(listLinks)))
                        .build();

                HttpResponse<String> listResp = HttpClientManager.get().send(listLinksRequest, HttpResponse.BodyHandlers.ofString());
                JsonObject data = GsonHelper.parseJsonToJsonObject(listResp.body());
                JsonArray links = data.getAsJsonArray("links");
                if (!links.isEmpty()) {
                    return links.get(0).getAsJsonObject().get("url").getAsString().replace("dl=0", "dl=1");
                }
            }
            JsonObject responseData = GsonHelper.parseJsonToJsonObject(response.body());
            return responseData.get("url").getAsString().replace("dl=0", "dl=1");
        } catch (Exception e) {
            throw new RuntimeException("Dropbox Sharing API Error: " + response.body(), e);
        }
    }

    @Nullable
    private String getOrRefreshToken() throws IOException, InterruptedException {
        if (this.accessToken != null && System.currentTimeMillis() < expiresAt - 300000) {
            return this.accessToken;
        }

        tokenLock.lock();
        try {
            if (this.accessToken != null && System.currentTimeMillis() < expiresAt - 300000) {
                return this.accessToken;
            }

            String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                    (this.appKey + ":" + this.appSecret).getBytes(StandardCharsets.UTF_8)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.dropboxapi.com/oauth2/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", authHeader)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "grant_type=refresh_token&refresh_token=" + this.refreshToken
                    ))
                    .build();

            HttpResponse<String> response = HttpClientManager.get().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                CraftEngine.instance().logger().warn("Dropbox Token Refresh Failed: " + response.body());
                return null;
            }

            JsonObject data = GsonHelper.parseJsonToJsonObject(response.body());
            this.accessToken = data.get("access_token").getAsString();
            this.expiresAt = System.currentTimeMillis() + (data.get("expires_in").getAsLong() * 1000);
            if (data.has("refresh_token")) {
                this.refreshToken = data.get("refresh_token").getAsString();
            }

            saveCacheToDisk();
            return this.accessToken;
        } finally {
            tokenLock.unlock();
        }
    }

    private void fail(CompletableFuture<?> future, String reason, String body) {
        String msg = "DropboxHost Error: " + reason + (body != null ? " | Body: " + body : "");
        future.completeExceptionally(new RuntimeException(msg));
    }

    private void readCacheFromDisk() {
        Path cachePath = CraftEngine.instance().dataFolderPath().resolve("cache").resolve("dropbox.json");
        if (!Files.exists(cachePath)) return;
        try (InputStreamReader isr = new InputStreamReader(Files.newInputStream(cachePath), StandardCharsets.UTF_8)) {
            Map<String, Object> cache = GsonHelper.get().fromJson(isr, new TypeToken<Map<String, Object>>(){}.getType());
            this.cachedUrl = (String) cache.get("url");
            this.cachedSha1 = (String) cache.get("sha1");
            String cachedRefreshToken = (String) cache.get("refresh_token");
            if (cachedRefreshToken != null) this.refreshToken = cachedRefreshToken;
            this.accessToken = (String) cache.get("access_token");
            this.expiresAt = cache.containsKey("expires_at") ? ((Double) cache.get("expires_at")).longValue() : 0;
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to load Dropbox cache", e);
        }
    }

    private void saveCacheToDisk() {
        Path cachePath = CraftEngine.instance().dataFolderPath().resolve("cache").resolve("dropbox.json");
        try {
            Files.createDirectories(cachePath.getParent());
            Map<String, Object> cache = new HashMap<>();
            cache.put("url", this.cachedUrl);
            cache.put("sha1", this.cachedSha1);
            cache.put("refresh_token", this.refreshToken);
            cache.put("access_token", this.accessToken);
            cache.put("expires_at", this.expiresAt);
            Files.writeString(cachePath, GsonHelper.get().toJson(cache), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Failed to persist Dropbox cache", e);
        }
    }

    private static class Factory implements ResourcePackHostFactory<DropboxHost> {
        private static final String[] USE_ENVIRONMENT_VARIABLES = new String[] {"use_environment_variables", "use-environment-variables"};
        private static final String[] UPLOAD_PATH = new String[] {"upload_path", "upload-path"};
        private static final String[] APP_KEY = new String[] {"app_key", "app-key"};
        private static final String[] APP_SECRET = new String[] {"app_secret", "app-secret"};
        private static final String[] REFRESH_TOKEN = new String[] {"refresh_token", "refresh-token"};

        @Override
        public DropboxHost create(ConfigSection section) {
            boolean useEnv = section.getBoolean(USE_ENVIRONMENT_VARIABLES);
            String appKey = useEnv ? getNonNullEnvironmentVariable(section, "CE_DROPBOX_APP_KEY") : section.getNonEmptyString(APP_KEY);
            String appSecret = useEnv ? getNonNullEnvironmentVariable(section, "CE_DROPBOX_APP_SECRET") : section.getNonEmptyString(APP_SECRET);
            String refreshToken = useEnv ? getNonNullEnvironmentVariable(section, "CE_DROPBOX_REFRESH_TOKEN") : section.getNonEmptyString(REFRESH_TOKEN);
            String uploadPath = section.getNonNullString(UPLOAD_PATH);
            return new DropboxHost(appKey, appSecret, refreshToken, uploadPath);
        }
    }
}