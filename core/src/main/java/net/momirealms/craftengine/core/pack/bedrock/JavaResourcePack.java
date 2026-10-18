package net.momirealms.craftengine.core.pack.bedrock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.bedrock.BedrockPackLanguages;
import net.momirealms.craftengine.core.pack.mcmeta.Overlay;
import net.momirealms.craftengine.core.pack.mcmeta.PackMeta;
import net.momirealms.craftengine.core.util.GsonHelper;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;

public final class JavaResourcePack {
    private static final int MAX_PATH = 260;
    private final MinecraftVersion version;
    private final Path root;
    private final Path vanillaRoot;

    private JavaResourcePack(Path root, Path vanillaRoot, MinecraftVersion version) {
        this.version = version;
        this.root = root;
        this.vanillaRoot = vanillaRoot;
    }

    private static void extractResources(IndexedZipFile archive, List<Overlay> overlays, MinecraftVersion version, Path root) throws IOException {
        Map<String, Integer> overlayPriorities = new HashMap<>();
        int priority = 0;
        for (Overlay overlay : overlays) {
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
        if (previous == null || priority > previous.priority() || (priority == previous.priority() && previous.entry().isDirectory() && !entry.isDirectory())) {
            resources.put(path, new ResourceEntry(entry, priority));
        }
    }

    public static JavaResourcePack openZip(Path source, Path vanilla, MinecraftVersion version, FileSystem fileSystem) throws IOException {
        Path root = fileSystem.getPath("resource_pack").toAbsolutePath();
        Path vanillaRoot = fileSystem.getPath("vanilla").toAbsolutePath();
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
            extractResources(archive, new PackMeta(metadata).overlays().overlays(), version, root);
        }
        try (IndexedZipFile archive = new IndexedZipFile(vanilla)) {
            extractResources(archive, List.of(), version, vanillaRoot);
        }
        return new JavaResourcePack(root, vanillaRoot, version);
    }

    @Nullable
    public JsonObject readJson(String path) throws IOException {
        return readJson(this.resolveResource(path));
    }

    public List<JsonObject> readAtlas(String path) throws IOException {
        List<JsonObject> sources = new ObjectArrayList<>(2);
        JsonObject vanilla = readJson(this.vanillaRoot.resolve(path));
        if (vanilla != null) {
            sources.add(vanilla);
        }
        JsonObject custom = readJson(this.root.resolve(path));
        if (custom != null) {
            sources.add(custom);
        }
        return sources;
    }

    @Nullable
    public JsonObject readResourceMetadata(String path) throws IOException {
        // A replacement resource must not inherit metadata from a lower-priority pack.
        if (Files.isRegularFile(this.root.resolve(path))) {
            return readJson(this.root.resolve(path + ".mcmeta"));
        }
        return this.readJson(path + ".mcmeta");
    }

    @Nullable
    private static JsonObject readJson(Path path) throws IOException {
        return Files.isRegularFile(path) ? GsonHelper.readJsonObjectFromFile(path) : null;
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
    InputStream openCustomResource(String path) throws IOException {
        Path resource = this.root.resolve(path);
        return Files.isRegularFile(resource) ? Files.newInputStream(resource) : null;
    }

    @Nullable
    public InputStream openResource(String path) throws IOException {
        Path resource = this.resolveResource(path);
        return Files.isRegularFile(resource) ? Files.newInputStream(resource) : null;
    }

    private Path resolveResource(String path) {
        Path resource = this.root.resolve(path);
        return Files.isRegularFile(resource) ? resource : this.vanillaRoot.resolve(path);
    }

    public JsonObject readModel(Key model) throws IOException {
        JsonObject json = this.readJson("assets/" + model.namespace() + "/models/" + model.value() + ".json");
        if (json == null) {
            throw new IOException("Missing Java model " + model + " for " + this.version.version());
        }
        return json;
    }

    private record ResourceEntry(ZipEntry entry, int priority) {
    }
}
