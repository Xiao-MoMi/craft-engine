package net.momirealms.craftengine.core.pack.bedrock;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.momirealms.craftengine.bedrock.BedrockPackLanguages;
import net.momirealms.craftengine.core.pack.AbstractPackManager;
import net.momirealms.craftengine.core.pack.mcmeta.Overlay;
import net.momirealms.craftengine.core.pack.mcmeta.PackMeta;
import net.momirealms.craftengine.core.util.IndexedZipFile;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;

public final class JavaResourcePack implements AutoCloseable {
    private static final int MAX_PATH = 260;
    private final MinecraftVersion version;
    private final Path root;

    private JavaResourcePack(Path root, MinecraftVersion version) {
        this.version = version;
        this.root = root;
    }

    private static void extractResources(IndexedZipFile archive, PackMeta metadata, MinecraftVersion version, Path root) throws IOException {
        Map<String, Integer> overlayPriorities = new HashMap<>();
        int priority = 0;
        for (Overlay overlay : metadata.overlays().overlays()) {
            if (version.isAtOrAbove(MinecraftVersion.V1_20_2) && overlay.test(version)) {
                overlayPriorities.put(overlay.directory(), ++priority);
            }
        }
        Map<String, ResourceEntry> resources = new HashMap<>();
        // Entries are unordered; compare declared overlay priorities while building the final index in one pass.
        for (ZipEntry entry : archive.entries()) {
            if (entry.isDirectory() && entry.getSize() == 0) {
                continue;
            }
            String name = entry.getName();
            if (name.length() >= MAX_PATH) {
                continue;
            }
            if (name.startsWith("assets/")) {
                indexResource(name, entry, 0, resources);
            }
            int separator = name.indexOf('/');
            if (separator < 0 || !name.startsWith("assets/", separator + 1)) {
                continue;
            }
            Integer overlayPriority = overlayPriorities.get(name.substring(0, separator));
            if (overlayPriority != null) {
                indexResource(name.substring(separator + 1), entry, overlayPriority, resources);
            }
        }
        Path assets = Files.createDirectories(root.resolve("assets"));
        for (Map.Entry<String, ResourceEntry> resource : resources.entrySet()) {
            Path target = root.resolve(resource.getKey()).normalize();
            if (!target.startsWith(assets)) {
                throw new IOException("Resource path outside assets: " + resource.getKey());
            }
            Files.createDirectories(target.getParent());
            try (InputStream input = archive.getInputStream(resource.getValue().entry())) {
                Files.copy(input, target);
            }
        }
    }

    private static void indexResource(String path, ZipEntry entry, int priority, Map<String, ResourceEntry> resources) {
        if (entry.isDirectory()) {
            path = path.substring(0, path.length() - 1);
        }
        ResourceEntry previous = resources.get(path);
        // Protected entries become files; an exact entry wins within the same overlay.
        if (previous == null || priority > previous.priority()
                || (priority == previous.priority() && previous.entry().isDirectory() && !entry.isDirectory())) {
            resources.put(path, new ResourceEntry(entry, priority));
        }
    }

    public static JavaResourcePack openZip(Path source, MinecraftVersion version) throws IOException {
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.forCurrentPlatform());
        try {
            Path root = fileSystem.getPath("resource_pack").toAbsolutePath();
            try (IndexedZipFile archive = new IndexedZipFile(source)) {
                ZipEntry entry = archive.getEntry("pack.mcmeta");
                if (entry == null) {
                    throw new IOException("Missing pack.mcmeta in " + source);
                }
                JsonObject metadata;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(archive.getInputStream(entry), StandardCharsets.UTF_8))) {
                    metadata = JsonParser.parseReader(reader).getAsJsonObject();
                }
                if (metadata == null || !(metadata.get("pack") instanceof JsonObject)) {
                    throw new IOException("Missing pack.mcmeta in " + source);
                }
                extractResources(archive, new PackMeta(metadata), version, root);
            }
            return new JavaResourcePack(root, version);
        } catch (IOException | RuntimeException | Error exception) {
            fileSystem.close();
            throw exception;
        }
    }

    @Override
    public void close() throws IOException {
        this.root.getFileSystem().close();
    }

    @Nullable
    public JsonObject readJson(String path) throws IOException {
        InputStream resource = this.openResource(path);
        if (resource == null) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    public MinecraftVersion version() {
        return this.version;
    }

    public Set<String> languageResources() throws IOException {
        Set<String> paths = new TreeSet<>();
        try (DirectoryStream<Path> namespaces = Files.newDirectoryStream(this.root.resolve("assets"))) {
            for (Path namespace : namespaces) {
                Path languages = namespace.resolve("lang");
                if (!Files.isDirectory(languages)) {
                    continue;
                }
                try (DirectoryStream<Path> files = Files.newDirectoryStream(languages, "*.json")) {
                    for (Path file : files) {
                        String name = file.getFileName().toString();
                        if (Files.isRegularFile(file) && BedrockPackLanguages.isLocale(name.substring(0, name.length() - 5))) {
                            paths.add("assets/" + namespace.getFileName() + "/lang/" + name);
                        }
                    }
                }
            }
        }
        return paths;
    }

    @Nullable
    public InputStream openResource(String path) throws IOException {
        Path resource = this.root.resolve(path);
        return Files.isRegularFile(resource) ? Files.newInputStream(resource) : null;
    }

    public JsonObject readModel(Key model) throws IOException {
        JsonObject json = this.readJson("assets/" + model.namespace() + "/models/" + model.value() + ".json");
        if (json == null) {
            JsonObject internal = this.internalModel(model);
            if (internal != null) {
                return internal.deepCopy();
            }
            throw new IOException("Missing Java model " + model + " for " + this.version.version());
        }
        return json;
    }

    @Nullable
    private JsonObject internalModel(Key model) {
        if (!model.isMinecraftNamespace()) {
            return null;
        }
        if (model.value().startsWith("block/")) {
            return AbstractPackManager.PRESET_MODELS_BLOCK.get(Key.minecraft(model.value().substring("block/".length())));
        }
        if (model.value().startsWith("item/")) {
            Key item = Key.minecraft(model.value().substring("item/".length()));
            return this.version.isAtOrAbove(MinecraftVersion.V1_21_4)
                    ? AbstractPackManager.PRESET_MODERN_MODELS_ITEM.get(item)
                    : AbstractPackManager.PRESET_LEGACY_MODELS_ITEM.get(item);
        }
        return null;
    }

    private record ResourceEntry(ZipEntry entry, int priority) {
    }
}
