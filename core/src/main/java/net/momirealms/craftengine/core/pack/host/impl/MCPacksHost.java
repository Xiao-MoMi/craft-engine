package net.momirealms.craftengine.core.pack.host.impl;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.momirealms.craftengine.core.pack.host.*;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class MCPacksHost implements ResourcePackHost {
    public static final ResourcePackHostFactory<MCPacksHost> FACTORY = new Factory();
    private final Path cacheFilePath;
    private volatile ResourcePackDownloadData cachedPack;

    private MCPacksHost(Path cacheFilePath) {
        this.cacheFilePath = cacheFilePath;
        this.readCacheFromDisk();
    }

    @Override
    public boolean canUpload() {
        return true;
    }

    @Override
    public ResourcePackHostType<MCPacksHost> type() {
        return ResourcePackHosts.MCPACKS;
    }

    @Override
    public CompletableFuture<List<ResourcePackDownloadData>> requestResourcePackDownloadLink(NetWorkUser user) {
        ResourcePackDownloadData pack = this.cachedPack;
        return CompletableFuture.completedFuture(pack == null ? List.of() : List.of(pack));
    }

    @Override
    public CompletableFuture<Void> upload(Path resourcePackPath) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        CraftEngine.instance().scheduler().executeAsync(() -> {
            try {
                String boundary = "CraftEngineBoundary" + UUID.randomUUID();
                HttpRequest request = HttpClientManager.requestBuilder()
                        .uri(URI.create("https://mcpacks.dev/api/v1/packs"))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(buildMultipartBody(resourcePackPath, boundary))
                        .build();

                HttpClientManager.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            ResourcePackDownloadData pack = parseUploadResponse(response.statusCode(), response.body());
                            this.cachedPack = pack;
                            saveCacheToDisk(pack);
                            future.complete(null);
                        })
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

    static HttpRequest.BodyPublisher buildMultipartBody(Path filePath, String boundary) throws IOException {
        String fileName = filePath.getFileName().toString()
                .replace("\r", "_").replace("\n", "_").replace("\"", "_");
        String start = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: application/zip\r\n\r\n";
        String end = "\r\n--" + boundary + "--\r\n";
        return HttpRequest.BodyPublishers.concat(
                HttpRequest.BodyPublishers.ofString(start, StandardCharsets.UTF_8),
                HttpRequest.BodyPublishers.ofFile(filePath),
                HttpRequest.BodyPublishers.ofString(end, StandardCharsets.UTF_8)
        );
    }

    static ResourcePackDownloadData parseUploadResponse(int statusCode, String body) {
        if (statusCode != 200 && statusCode != 201) {
            throw new IllegalStateException("MCPacksHost Error: Upload HTTP " + statusCode + " | Body: " + body);
        }
        JsonObject json = GsonHelper.get().fromJson(body, JsonObject.class);
        if (json == null || !json.has("success") || !json.get("success").getAsBoolean()) {
            throw new IllegalStateException("MCPacksHost Error: API returned error | Body: " + body);
        }
        JsonObject data = json.getAsJsonObject("data");
        if (data == null || !data.has("download_url") || !data.has("sha1")) {
            throw new IllegalStateException("MCPacksHost Error: Missing download data | Body: " + body);
        }
        return downloadData(data.get("download_url").getAsString(), data.get("sha1").getAsString());
    }

    private static ResourcePackDownloadData downloadData(String url, String sha1) {
        if (url == null || url.isBlank() || sha1 == null || sha1.isBlank()) {
            throw new IllegalStateException("MCPacksHost Error: Missing download URL or SHA-1");
        }
        UUID uuid = UUID.nameUUIDFromBytes(sha1.getBytes(StandardCharsets.UTF_8));
        return new ResourcePackDownloadData(url, uuid, sha1);
    }

    private void readCacheFromDisk() {
        if (!Files.exists(this.cacheFilePath)) return;
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(this.cacheFilePath), StandardCharsets.UTF_8)) {
            Map<String, String> cache = GsonHelper.get().fromJson(reader, new TypeToken<Map<String, String>>(){}.getType());
            this.cachedPack = downloadData(cache.get("url"), cache.get("sha1"));
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to load MCPacks cache", e);
        }
    }

    private void saveCacheToDisk(ResourcePackDownloadData pack) {
        try {
            Files.createDirectories(this.cacheFilePath.getParent());
            Map<String, String> cache = Map.of("url", pack.url(), "sha1", pack.sha1());
            Files.writeString(this.cacheFilePath, GsonHelper.get().toJson(cache), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Failed to persist MCPacks cache", e);
        }
    }

    private static class Factory implements ResourcePackHostFactory<MCPacksHost> {
        private static final String[] CACHE_FILE_NAME = ConfigKeys.of("cache_file_name");

        @Override
        public MCPacksHost create(String id, ConfigSection section) {
            Path cacheFilePath = CraftEngine.instance().dataFolderPath().resolve("cache")
                    .resolve(section.getValue(CACHE_FILE_NAME, it -> it.getAsNonEmptyString().replace("/", "_"), "mcpacks_" + id + ".json"));
            return new MCPacksHost(cacheFilePath);
        }
    }
}
