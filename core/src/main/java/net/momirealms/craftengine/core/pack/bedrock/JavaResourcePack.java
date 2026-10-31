package net.momirealms.craftengine.core.pack.bedrock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.pack.mcmeta.Overlay;
import net.momirealms.craftengine.core.pack.mcmeta.PackMeta;
import net.momirealms.craftengine.core.util.*;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

// 单版本、去除overlay后的资源包视图
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
        Set<String> placeholders = new HashSet<>();
        // Protected ZIPs can contain empty files at paths needed as directories by real resources.
        for (String path : resources.keySet()) {
            int separator = path.lastIndexOf('/');
            while (separator > 0) {
                String parent = path.substring(0, separator);
                ResourceEntry entry = resources.get(parent);
                if (entry != null && entry.entry().getSize() == 0) {
                    placeholders.add(parent);
                }
                separator = path.lastIndexOf('/', separator - 1);
            }
        }
        for (String placeholder : placeholders) {
            resources.remove(placeholder);
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
            ZipEntry icon = archive.getEntry("pack.png");
            if (icon != null) {
                try (InputStream input = archive.getInputStream(icon)) {
                    Files.copy(input, root.resolve("pack.png"));
                }
            }
        }
        try (IndexedZipFile archive = new IndexedZipFile(vanilla)) {
            extractResources(archive, List.of(), version, vanillaRoot);
        }
        // Indexed font assets replace the empty definitions shipped inside client.jar.
        Path fontAssets = vanilla.resolveSibling("assets").resolve("minecraft/font");
        if (Files.isDirectory(fontAssets)) {
            try (Stream<Path> files = Files.walk(fontAssets)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    Path target = vanillaRoot.resolve("assets/minecraft/font").resolve(CharacterUtils.replaceBackslashWithSlash(fontAssets.relativize(file).toString()));
                    Files.createDirectories(target.getParent());
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        return new JavaResourcePack(root, vanillaRoot, version);
    }

    @Nullable
    public JsonObject readJson(String path) throws IOException {
        return readJson(this.resolveResource(path));
    }

    @Nullable
    public JsonObject readParticleJson(Key key) throws IOException {
        return this.readJson(ResourcePackUtils.particleDefinitionPath(key));
    }

    public List<JsonObject> readAtlas(Key atlas) throws IOException {
        String path = "assets/" + atlas.namespace() + "/atlases/" + atlas.value() + ".json";
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

    public Map<Key, List<FontResource>> fonts() throws IOException {
        Map<Key, List<FontResource>> fonts = new TreeMap<>(Comparator.comparing(Key::asString));
        this.collectFonts(this.root, true, fonts);
        this.collectFonts(this.vanillaRoot, false, fonts);
        return fonts;
    }

    private void collectFonts(Path root, boolean custom, Map<Key, List<FontResource>> fonts) throws IOException {
        Path assets = root.resolve("assets");
        if (!Files.isDirectory(assets)) {
            return;
        }
        try (DirectoryStream<Path> namespaces = Files.newDirectoryStream(assets)) {
            for (Path namespace : namespaces) {
                Path directory = namespace.resolve("font");
                if (!Files.isDirectory(directory)) {
                    continue;
                }
                try (Stream<Path> files = Files.walk(directory)) {
                    for (Path file : files.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".json")).toList()) {
                        String path = CharacterUtils.replaceBackslashWithSlash(directory.relativize(file).toString());
                        Key id = Key.of(namespace.getFileName().toString(), path.substring(0, path.length() - 5));
                        fonts.computeIfAbsent(id, key -> new ObjectArrayList<>(2)).add(new FontResource(readJson(file), custom));
                    }
                }
            }
        }
    }

    public record FontResource(JsonObject definition, boolean custom) {
    }

    public JsonObject translations() throws IOException {
        JsonObject translations = new JsonObject();
        Path assets = this.root.resolve("assets");
        if (!Files.isDirectory(assets)) {
            return translations;
        }
        try (Stream<Path> namespaces = Files.list(assets)) {
            List<Path> sorted = namespaces.filter(Files::isDirectory)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString())).toList();
            for (Path namespace : sorted) {
                Path directory = namespace.resolve("lang");
                if (!Files.isDirectory(directory)) {
                    continue;
                }
                try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.json")) {
                    for (Path file : files) {
                        String name = file.getFileName().toString();
                        if (!Files.isRegularFile(file) || name.equals("deprecated.json")) {
                            continue;
                        }
                        String locale = name.substring(0, name.length() - 5);
                        JsonObject language = translations.getAsJsonObject(locale);
                        if (language == null) {
                            language = new JsonObject();
                            translations.add(locale, language);
                        }
                        try {
                            JsonObject source = readJson(file);
                            if (source == null) {
                                throw new IOException("Missing or non-object language file " + file);
                            }
                            for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
                                language.addProperty(entry.getKey(), entry.getValue().getAsString());
                            }
                        } catch (RuntimeException exception) {
                            throw new IOException("Failed to read language file " + file, exception);
                        }
                    }
                }
            }
        }
        return GsonHelper.sortObjectKeys(translations).getAsJsonObject();
    }

    public Map<Key, JsonObject> itemDefinitions() throws IOException {
        Map<Key, JsonObject> definitions = new TreeMap<>(Comparator.comparing(Key::asString));
        Path assets = this.root.resolve("assets");
        if (!Files.isDirectory(assets)) {
            return definitions;
        }
        try (DirectoryStream<Path> namespaces = Files.newDirectoryStream(assets)) {
            for (Path namespace : namespaces) {
                Path items = namespace.resolve("items");
                if (!Files.isDirectory(items)) {
                    continue;
                }
                try (Stream<Path> files = Files.walk(items)) {
                    Iterator<Path> iterator = files.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".json")).iterator();
                    while (iterator.hasNext()) {
                        Path file = iterator.next();
                        String path = CharacterUtils.replaceBackslashWithSlash(items.relativize(file).toString());
                        Key key = Key.of(namespace.getFileName().toString(), path.substring(0, path.length() - 5));
                        JsonObject definition;
                        try {
                            definition = readJson(file);
                        } catch (IOException | JsonParseException exception) {
                            throw new IOException("Failed to read item definition " + key + " at " + ResourcePackUtils.itemDefinitionPath(key), exception);
                        }
                        if (definition == null) {
                            throw new IOException("Missing or non-object item definition " + key + " at " + ResourcePackUtils.itemDefinitionPath(key));
                        }
                        definitions.put(key, definition);
                    }
                }
            }
        }
        return definitions;
    }

    @Nullable
    public InputStream openCustomResource(String path) throws IOException {
        Path resource = this.root.resolve(path);
        return Files.isRegularFile(resource) ? Files.newInputStream(resource) : null;
    }

    public boolean hasCustomResource(String path) {
        return Files.isRegularFile(this.root.resolve(path));
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

    public JsonObject readVanillaModel(Key model) throws IOException {
        JsonObject json = readJson(this.vanillaRoot.resolve("assets/" + model.namespace() + "/models/" + model.value() + ".json"));
        if (json == null) {
            throw new IOException("Missing vanilla Java model " + model + " for " + this.version.version());
        }
        return json;
    }

    private record ResourceEntry(ZipEntry entry, int priority) {
    }
}
