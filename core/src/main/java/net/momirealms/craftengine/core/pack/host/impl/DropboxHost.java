package net.momirealms.craftengine.core.pack.host.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.host.ResourcePackDownloadData;
import net.momirealms.craftengine.core.pack.host.ResourcePackHost;
import net.momirealms.craftengine.core.pack.host.ResourcePackHostFactory;
import net.momirealms.craftengine.core.pack.host.ResourcePackHosts;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.LocalizedException;
import net.momirealms.craftengine.core.util.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

public class DropboxHost implements ResourcePackHost {
    public static final Factory FACTORY = new Factory();
    private final String appKey;
    private final String appSecret;
    private final String uploadPath;
    private final ProxySelector proxy;
    private final ReentrantLock tokenLock = new ReentrantLock();

    private volatile String accessToken;
    private volatile String refreshToken;
    private volatile long expiresAt;
    private String url;
    private String sha1;

    public DropboxHost(String appKey, String appSecret, String refreshToken, String uploadPath, ProxySelector proxy) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.refreshToken = refreshToken;
        this.uploadPath = uploadPath;
        this.proxy = proxy;
        readCacheFromDisk();
    }

    public void readCacheFromDisk() {
        Path cachePath = CraftEngine.instance().dataFolderPath().resolve("cache").resolve("dropbox.json");
        if (!Files.exists(cachePath)) return;
        try (InputStream is = Files.newInputStream(cachePath)) {
            JsonObject cache = GsonHelper.parseJsonToJsonObject(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            this.url = getString(cache, "url");
            this.sha1 = getString(cache, "sha1");
            this.refreshToken = getString(cache, "refresh_token");
            this.accessToken = getString(cache, "access_token");
            this.expiresAt = getLong(cache, "expires_at");
            CraftEngine.instance().logger().info("[Dropbox] Loaded cached resource pack info");
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("[Dropbox] Failed to load cache " + cachePath, e);
        }
    }

    public void saveCacheToDisk() {
        JsonObject cache = new JsonObject();
        cache.addProperty("url", this.url);
        cache.addProperty("sha1", this.sha1);
        cache.addProperty("refresh_token", this.refreshToken);
        cache.addProperty("access_token", this.accessToken);
        cache.addProperty("expires_at", this.expiresAt);
        Path cachePath = CraftEngine.instance().dataFolderPath().resolve("cache").resolve("dropbox.json");
        try {
            Files.createDirectories(cachePath);
            Files.writeString(
                    cachePath,
                    GsonHelper.get().toJson(cache),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            CraftEngine.instance().logger().warn("[Dropbox] Failed to save cache", e);
        }
    }

    @Override
    public boolean canUpload() {
        return true;
    }

    @Override
    public Key type() {
        return ResourcePackHosts.DROPBOX;
    }

    @Override
    public CompletableFuture<List<ResourcePackDownloadData>> requestResourcePackDownloadLink(UUID player) {
        return CompletableFuture.completedFuture(Collections.singletonList(ResourcePackDownloadData.of(
                this.url, UUID.nameUUIDFromBytes(this.sha1.getBytes(StandardCharsets.UTF_8)), this.sha1
        )));
    }

    @Override
    public CompletableFuture<Void> upload(Path resourcePackPath) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        CraftEngine.instance().scheduler().executeAsync(() -> {
            try {
                String validToken = getOrRefreshToken();
                this.sha1 = HashUtils.calculateLocalFileSha1(resourcePackPath);

                try (HttpClient client = HttpClient.newBuilder().proxy(this.proxy).build()) {
                    JsonObject apiArg = new JsonObject();
                    apiArg.addProperty("path", this.uploadPath);
                    apiArg.addProperty("mode", "overwrite");

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://content.dropboxapi.com/2/files/upload"))
                            .header("Authorization", "Bearer " + validToken)
                            .header("Content-Type", "application/octet-stream")
                            .header("Dropbox-API-Arg", apiArg.toString())
                            .POST(HttpRequest.BodyPublishers.ofFile(resourcePackPath))
                            .build();

                    long startTime = System.currentTimeMillis();
                    CraftEngine.instance().logger().info("[Dropbox] Starting upload...");

                    client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                            .thenAccept(response -> {
                                long elapsed = System.currentTimeMillis() - startTime;
                                if (response.statusCode() == 200) {
                                    CraftEngine.instance().logger().info(
                                            "[Dropbox] Upload completed in " + elapsed + "ms");
                                    this.url = getDownloadUrl(validToken);
                                    saveCacheToDisk();
                                    future.complete(null);
                                } else {
                                    CraftEngine.instance().logger().warn(
                                            "[Dropbox] Upload failed (HTTP " + response.statusCode() + "): " + response.body());
                                    future.completeExceptionally(new RuntimeException(response.body()));
                                }
                            })
                            .exceptionally(ex -> {
                                CraftEngine.instance().logger().warn("[Dropbox] Upload error", ex);
                                future.completeExceptionally(ex);
                                return null;
                            });
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private String getDownloadUrl(String accessToken) {
        try (HttpClient client = HttpClient.newBuilder().proxy(this.proxy).build()) {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("path", this.uploadPath);
            requestBody.add("settings", new JsonObject());
            requestBody.getAsJsonObject("settings").addProperty("requested_visibility", "public");

            HttpRequest createLinkRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.dropboxapi.com/2/sharing/create_shared_link_with_settings"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = client.send(createLinkRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 409) {
                JsonObject listRequest = new JsonObject();
                listRequest.addProperty("path", this.uploadPath);

                HttpRequest listLinksRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.dropboxapi.com/2/sharing/list_shared_links"))
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(listRequest.toString()))
                        .build();

                HttpResponse<String> listResponse = client.send(listLinksRequest, HttpResponse.BodyHandlers.ofString());
                JsonObject listData = GsonHelper.parseJsonToJsonObject(listResponse.body());
                JsonArray links = listData.getAsJsonArray("links");
                if (!links.isEmpty()) {
                    return links.get(0).getAsJsonObject().get("url").getAsString().replace("dl=0", "dl=1");
                }
            }

            JsonObject responseData = GsonHelper.parseJsonToJsonObject(response.body());
            return responseData.get("url").getAsString().replace("dl=0", "dl=1");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to get download URL", e);
        }
    }

    private String getString(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsString() : null;
    }

    @SuppressWarnings("SameParameterValue")
    private long getLong(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsLong() : 0;
    }

    private String getOrRefreshToken() {
        if (System.currentTimeMillis() < expiresAt - 30000 && this.accessToken != null) {
            return this.accessToken;
        }
        this.tokenLock.lock();
        try {
            if (System.currentTimeMillis() < expiresAt - 30000 && this.accessToken != null) {
                return this.accessToken;
            }

            String credentials = this.appKey + ":" + this.appSecret;
            String authHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

            try (HttpClient client = HttpClient.newBuilder().proxy(this.proxy).build()) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.dropboxapi.com/oauth2/token"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Authorization", authHeader)
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "grant_type=refresh_token" +
                                        "&refresh_token=" + this.refreshToken
                        ))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new RuntimeException("Refresh failed: " + response.body());
                }

                JsonObject tokenData = GsonHelper.parseJsonToJsonObject(response.body());
                this.accessToken = tokenData.get("access_token").getAsString();
                this.expiresAt = System.currentTimeMillis() +
                        tokenData.get("expires_in").getAsLong() * 1000;

                if (tokenData.has("refresh_token")) {
                    this.refreshToken = tokenData.get("refresh_token").getAsString();
                }

                saveCacheToDisk();
                return this.accessToken;
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Token refresh failed", e);
            }
        } finally {
            this.tokenLock.unlock();
        }
    }

    public static class Factory implements ResourcePackHostFactory {

        @Override
        public ResourcePackHost create(Map<String, Object> arguments) {
            boolean useEnv = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("use-environment-variables", false), "use-environment-variables");
            String appKey = useEnv ? System.getenv("CE_DROPBOX_APP_KEY") : Optional.ofNullable(arguments.get("app-key")).map(String::valueOf).orElse(null);
            if (appKey == null || appKey.isEmpty()) {
                throw new LocalizedException("warning.config.host.dropbox.missing_app_key");
            }
            String appSecret = useEnv ? System.getenv("CE_DROPBOX_APP_SECRET") : Optional.ofNullable(arguments.get("app-secret")).map(String::valueOf).orElse(null);
            if (appSecret == null || appSecret.isEmpty()) {
                throw new LocalizedException("warning.config.host.dropbox.missing_app_secret");
            }
            String refreshToken = useEnv ? System.getenv("CE_DROPBOX_REFRESH_TOKEN") : Optional.ofNullable(arguments.get("refresh-token")).map(String::valueOf).orElse(null);
            if (refreshToken == null || refreshToken.isEmpty()) {
                throw new LocalizedException("warning.config.host.dropbox.missing_refresh_token");
            }
            String uploadPath = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("upload-path"), () -> new LocalizedException("warning.config.host.dropbox.missing_upload_path"));
            ProxySelector proxy = getProxySelector(MiscUtils.castToMap(arguments.get("proxy"), true));
            return new DropboxHost(appKey, appSecret, refreshToken, "/" + uploadPath, proxy);
        }
    }
}