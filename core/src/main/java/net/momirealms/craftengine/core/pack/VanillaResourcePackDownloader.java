package net.momirealms.craftengine.core.pack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.momirealms.craftengine.core.pack.host.HttpClientManager;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.HashUtils;
import net.momirealms.craftengine.core.util.MinecraftVersion;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public final class VanillaResourcePackDownloader {
    private static final URI VERSION_MANIFEST = URI.create("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");
    private final Path cacheDirectory;
    private final URI versionManifest;

    public VanillaResourcePackDownloader(Path cacheDirectory) {
        this(cacheDirectory, VERSION_MANIFEST);
    }

    VanillaResourcePackDownloader(Path cacheDirectory, URI versionManifest) {
        this.cacheDirectory = cacheDirectory;
        this.versionManifest = versionManifest;
    }

    public synchronized Path download(MinecraftVersion version) throws IOException {
        Path directory = this.cacheDirectory.resolve(version.version());
        JsonObject metadata = this.readVersionMetadata(version.version(), directory);
        JsonObject client = metadata.getAsJsonObject("downloads").getAsJsonObject("client");
        Path archive = ensureDownloaded(Download.fromMetadata(client), directory.resolve("client.jar"));
        downloadFonts(metadata.getAsJsonObject("assetIndex"), directory);
        return archive;
    }

    private JsonObject readVersionMetadata(String version, Path directory) throws IOException {
        Path metadataPath = directory.resolve("version.json");
        // A cached version.json lets a complete cache work without contacting the version manifest.
        if (!Files.isRegularFile(metadataPath)) {
            ensureDownloaded(this.findVersion(version), metadataPath);
        }
        JsonObject metadata = GsonHelper.readJsonObjectFromFile(metadataPath);
        if (metadata == null || !version.equals(metadata.get("id").getAsString())) {
            throw new IOException("Invalid vanilla resource metadata for Minecraft " + version);
        }
        return metadata;
    }

    private static void downloadFonts(JsonObject assetIndex, Path directory) throws IOException {
        Path indexPath = ensureDownloaded(Download.fromMetadata(assetIndex), directory.resolve("asset-index.json"));
        JsonObject objects = GsonHelper.readJsonObjectFromFile(indexPath).getAsJsonObject("objects");
        Path assets = directory.resolve("assets");
        for (Map.Entry<String, JsonElement> entry : objects.entrySet()) {
            if (!entry.getKey().startsWith("minecraft/font/")) {
                continue;
            }
            Download font = Download.fromAsset(entry.getValue().getAsJsonObject());
            ensureDownloaded(font, assets.resolve(entry.getKey()));
        }
    }

    private Download findVersion(String version) throws IOException {
        String body = fetch(this.versionManifest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonObject manifest = JsonParser.parseString(body).getAsJsonObject();
        for (JsonElement element : manifest.getAsJsonArray("versions")) {
            JsonObject entry = element.getAsJsonObject();
            if (version.equals(entry.get("id").getAsString())) {
                return new Download(URI.create(entry.get("url").getAsString()), entry.get("sha1").getAsString(), -1);
            }
        }
        throw new IOException("Minecraft version is not available in the official manifest: " + version);
    }

    private static Path ensureDownloaded(Download download, Path target) throws IOException {
        if (download.matches(target)) {
            return target;
        }
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".part");
        try {
            fetch(download.uri(), HttpResponse.BodyHandlers.ofFile(temporary));
            if (!download.matches(temporary)) {
                throw new IOException("Size or SHA-1 mismatch for " + download.uri());
            }
            return Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static <T> T fetch(URI uri, HttpResponse.BodyHandler<T> handler) throws IOException {
        HttpRequest request = HttpClientManager.requestBuilder().uri(uri).GET().build();
        try {
            HttpResponse<T> response = HttpClientManager.send(request, handler);
            if (response.statusCode() != 200) {
                throw new IOException("Failed to download " + uri + ": HTTP " + response.statusCode());
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while downloading " + uri, e);
        }
    }

    private record Download(URI uri, String sha1, long size) {

        private static Download fromMetadata(JsonObject entry) {
            URI uri = URI.create(entry.get("url").getAsString());
            return new Download(uri, entry.get("sha1").getAsString(), entry.get("size").getAsLong());
        }

        private static Download fromAsset(JsonObject entry) {
            String hash = entry.get("hash").getAsString();
            URI uri = URI.create("https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash);
            return new Download(uri, hash, entry.get("size").getAsLong());
        }

        private boolean matches(Path path) throws IOException {
            return Files.isRegularFile(path)
                    && (this.size < 0 || Files.size(path) == this.size)
                    && this.sha1.equalsIgnoreCase(HashUtils.sha1(path));
        }
    }
}
