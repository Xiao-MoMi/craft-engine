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
        Path directory = Files.createDirectories(this.cacheDirectory.resolve(version.version()));
        Path metadataPath = directory.resolve("version.json");
        if (!Files.isRegularFile(metadataPath)) {
            this.downloadFile(this.findVersion(version), metadataPath);
        }
        JsonObject metadata = GsonHelper.readJsonObjectFromFile(metadataPath);
        if (metadata == null || !version.version().equals(metadata.get("id").getAsString())) {
            throw new IOException("Invalid vanilla resource metadata for Minecraft " + version.version());
        }
        JsonObject client = metadata.getAsJsonObject("downloads").getAsJsonObject("client");
        Download download = new Download(
                URI.create(client.get("url").getAsString()),
                client.get("sha1").getAsString(),
                client.get("size").getAsLong()
        );
        Path archive = directory.resolve("client.jar");
        if (!download.matches(archive)) {
            this.downloadFile(download, archive);
        }
        this.downloadFonts(metadata.getAsJsonObject("assetIndex"), directory);
        return archive;
    }

    private void downloadFonts(JsonObject metadata, Path directory) throws IOException {
        Download index = new Download(URI.create(metadata.get("url").getAsString()), metadata.get("sha1").getAsString(), metadata.get("size").getAsLong());
        Path indexPath = directory.resolve("asset-index.json");
        if (!index.matches(indexPath)) {
            this.downloadFile(index, indexPath);
        }
        JsonObject objects = GsonHelper.readJsonObjectFromFile(indexPath).getAsJsonObject("objects");
        for (Map.Entry<String, JsonElement> entry : objects.entrySet()) {
            if (!entry.getKey().startsWith("minecraft/font/")) {
                continue;
            }
            JsonObject asset = entry.getValue().getAsJsonObject();
            String hash = asset.get("hash").getAsString();
            Download download = new Download(URI.create("https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash), hash, asset.get("size").getAsLong());
            Path target = directory.resolve("assets").resolve(entry.getKey());
            if (!download.matches(target)) {
                Files.createDirectories(target.getParent());
                this.downloadFile(download, target);
            }
        }
    }

    private Download findVersion(MinecraftVersion version) throws IOException {
        HttpRequest request = HttpClientManager.requestBuilder().uri(this.versionManifest).GET().build();
        HttpResponse<String> response;
        try {
            response = HttpClientManager.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while resolving Minecraft " + version.version(), e);
        }
        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch Minecraft version manifest: HTTP " + response.statusCode());
        }
        JsonObject manifest = JsonParser.parseString(response.body()).getAsJsonObject();
        for (JsonElement element : manifest.getAsJsonArray("versions")) {
            JsonObject entry = element.getAsJsonObject();
            if (version.version().equals(entry.get("id").getAsString())) {
                return new Download(URI.create(entry.get("url").getAsString()), entry.get("sha1").getAsString(), -1);
            }
        }
        throw new IOException("Minecraft version is not available in the official manifest: " + version.version());
    }

    private void downloadFile(Download download, Path target) throws IOException {
        Path temporary = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".part");
        try {
            HttpRequest request = HttpClientManager.requestBuilder().uri(download.uri()).GET().build();
            HttpResponse<Path> response;
            try {
                response = HttpClientManager.send(request, HttpResponse.BodyHandlers.ofFile(temporary));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while downloading " + download.uri(), e);
            }
            if (response.statusCode() != 200) {
                throw new IOException("Failed to download " + download.uri() + ": HTTP " + response.statusCode());
            }
            if (!download.matches(temporary)) {
                throw new IOException("Size or SHA-1 mismatch for " + download.uri());
            }
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private record Download(URI uri, String sha1, long size) {

        private boolean matches(Path path) throws IOException {
            return Files.isRegularFile(path) && (this.size < 0 || Files.size(path) == this.size) && this.sha1.equalsIgnoreCase(HashUtils.sha1(path));
        }
    }
}
