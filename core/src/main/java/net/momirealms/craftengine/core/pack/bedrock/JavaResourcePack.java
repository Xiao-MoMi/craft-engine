package net.momirealms.craftengine.core.pack.bedrock;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.gson.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.pack.VanillaResourcePackDownloader;
import net.momirealms.craftengine.core.pack.mcmeta.Overlay;
import net.momirealms.craftengine.core.pack.mcmeta.PackMeta;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.*;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.FileSystem;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

// 单版本、去除overlay后的资源包视图
public final class JavaResourcePack {
    private static final int MAX_PATH = 260;
    private static final Configuration INPUT_FILE_SYSTEM = Configuration.forCurrentPlatform().toBuilder()
            .setBlockSize(4096)
            .build();
    private final MinecraftVersion version;
    private final Path root;
    private final Path vanillaRoot;
    private final VanillaResourcePackDownloader.IndexedAssets indexedAssets;
    private final BiPredicate<String, String> vanillaFilter;

    private JavaResourcePack(Path root, Path vanillaRoot, MinecraftVersion version, @Nullable BiPredicate<String, String> vanillaFilter, Path vanillaDirectory) {
        this.version = version;
        this.root = root;
        this.vanillaRoot = vanillaRoot;
        this.indexedAssets = new VanillaResourcePackDownloader.IndexedAssets(vanillaDirectory);
        this.vanillaFilter = vanillaFilter;
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
        Set<Path> directories = new HashSet<>();
        directories.add(assets);
        byte[] buffer = new byte[16 * 1024];
        for (Map.Entry<String, ResourceEntry> resource : resources.entrySet()) {
            Path target = root.resolve(resource.getKey()).normalize();
            if (!target.startsWith(assets)) {
                throw new IOException("Resource path outside assets: " + resource.getKey());
            }
            Path parent = target.getParent();
            if (directories.add(parent)) {
                Files.createDirectories(parent);
            }
            try (InputStream input = archive.getInputStream(resource.getValue().entry());
                 OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                FileUtils.copy(input, output, buffer);
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

    public static FileSystem newInputFileSystem() {
        return Jimfs.newFileSystem(INPUT_FILE_SYSTEM);
    }

    public static JavaResourcePack openZip(Path source, Path vanilla, MinecraftVersion version, FileSystem fileSystem) throws IOException {
        Path root = fileSystem.getPath("resource_pack").toAbsolutePath();
        Path vanillaRoot = fileSystem.getPath("vanilla").toAbsolutePath();
        BiPredicate<String, String> vanillaFilter;
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
            PackMeta packMeta = new PackMeta(metadata);
            vanillaFilter = packMeta.filter() == null ? null : packMeta.filter().compile();
            extractResources(archive, packMeta.overlays().overlays(), version, root);
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
        return new JavaResourcePack(root, vanillaRoot, version, vanillaFilter, vanilla.toAbsolutePath().getParent());
    }

    @Nullable
    public Path soundResource(String namespace, String name, boolean custom) throws IOException {
        if (namespace.isEmpty() || namespace.equals(".") || namespace.equals("..")
                || namespace.indexOf('/') >= 0 || namespace.indexOf('\\') >= 0 || namespace.indexOf(':') >= 0
                || name.indexOf('\\') >= 0 || hasParentDirectory(name)) {
            return null;
        }
        String path = "assets/" + namespace + '/' + name;
        if (!custom && this.filtersVanilla(path)) {
            return null;
        }
        Path root = custom ? this.root : this.vanillaRoot;
        Path resource = root.resolve(path);
        if (Files.isRegularFile(resource)) {
            return resource;
        }
        return custom ? null : this.indexedAssets.find(namespace + '/' + name);
    }

    private static boolean hasParentDirectory(String name) {
        return name.equals("..") || name.startsWith("../") || name.endsWith("/..") || name.contains("/../");
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
        JsonObject vanilla = this.filtersVanilla(path) ? null : readJson(this.vanillaRoot.resolve(path));
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
                        if (!custom && this.vanillaFilter != null && this.vanillaFilter.test(namespace.getFileName().toString(), "font/" + path)) {
                            continue;
                        }
                        Key id = Key.of(namespace.getFileName().toString(), path.substring(0, path.length() - 5));
                        try {
                            JsonObject definition = readJson(file);
                            if (definition == null || !(definition.get("providers") instanceof JsonArray)) {
                                TranslationManager.warn("resource_pack.bedrock.font.definition", id.asString(), file.toString());
                                continue;
                            }
                            fonts.computeIfAbsent(id, key -> new ObjectArrayList<>(2)).add(new FontResource(definition, custom));
                        } catch (IOException | JsonParseException exception) {
                            TranslationManager.warn("resource_pack.bedrock.font.read", id.asString(), file.toString(), exception.toString());
                        }
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
            List<Path> sorted = namespaces.filter(Files::isDirectory).sorted(Comparator.comparing(path -> path.getFileName().toString())).toList();
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

    public Map<Key, JsonObject> vanillaItemDefinitions() throws IOException {
        if (Files.isDirectory(this.vanillaRoot.resolve("assets/minecraft/items"))) {
            return this.itemDefinitions(this.vanillaRoot.resolve("assets"), true);
        }
        Map<Key, JsonObject> definitions = new TreeMap<>(Comparator.comparing(Key::asString));
        Path directory = this.vanillaRoot.resolve("assets/minecraft/models/item");
        if (!Files.isDirectory(directory)) {
            return definitions;
        }
        try (Stream<Path> files = Files.walk(directory)) {
            for (Path file : files.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".json")).toList()) {
                String name = CharacterUtils.replaceBackslashWithSlash(directory.relativize(file).toString());
                if (this.vanillaFilter != null && this.vanillaFilter.test("minecraft", "models/item/" + name)) {
                    continue;
                }
                Key id = Key.of("minecraft", name.substring(0, name.length() - 5));
                JsonObject model = new JsonObject();
                model.addProperty("type", "minecraft:model");
                model.addProperty("model", "minecraft:item/" + id.value());
                JsonObject definition = new JsonObject();
                definition.add("model", model);
                definitions.put(id, definition);
            }
        }
        return definitions;
    }

    public Map<Key, JsonObject> itemDefinitions() throws IOException {
        return this.itemDefinitions(this.root.resolve("assets"), false);
    }

    private Map<Key, JsonObject> itemDefinitions(Path assets, boolean vanilla) throws IOException {
        Map<Key, JsonObject> definitions = new TreeMap<>(Comparator.comparing(Key::asString));
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
                        if (vanilla && this.vanillaFilter != null && this.vanillaFilter.test(namespace.getFileName().toString(), "items/" + path)) {
                            continue;
                        }
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

    public Set<Key> customSoundFiles() throws IOException {
        Set<Key> files = new HashSet<>();
        for (String namespace : this.soundNamespaces()) {
            Path sounds = this.root.resolve("assets").resolve(namespace).resolve("sounds");
            if (Files.isDirectory(sounds)) {
                try (Stream<Path> paths = Files.walk(sounds)) {
                    for (Path path : paths.filter(Files::isRegularFile).filter(file -> file.toString().endsWith(".ogg")).toList()) {
                        String name = CharacterUtils.replaceBackslashWithSlash(sounds.relativize(path).toString());
                        files.add(Key.of(namespace, name.substring(0, name.length() - 4)));
                    }
                }
            }
        }
        return files;
    }

    public List<String> soundNamespaces() throws IOException {
        try (Stream<Path> paths = Files.list(this.root.resolve("assets"))) {
            return paths.filter(path -> Files.isRegularFile(path.resolve("sounds.json")) || Files.isDirectory(path.resolve("sounds")))
                    .map(path -> path.getFileName().toString()).sorted().toList();
        }
    }

    @Nullable
    public InputStream openResource(String path) throws IOException {
        Path resource = this.resolveResource(path);
        return Files.isRegularFile(resource) ? Files.newInputStream(resource) : null;
    }

    private Path resolveResource(String path) {
        Path resource = this.root.resolve(path);
        return Files.isRegularFile(resource) || this.filtersVanilla(path) ? resource : this.vanillaRoot.resolve(path);
    }

    public boolean filtersVanilla(String path) {
        if (this.vanillaFilter == null || !path.startsWith("assets/")) {
            return false;
        }
        int separator = path.indexOf('/', 7);
        return separator >= 0 && this.vanillaFilter.test(path.substring(7, separator), path.substring(separator + 1));
    }

    public JsonObject readModel(Key model) throws IOException {
        JsonObject json = this.readJson(ResourcePackUtils.modelPath(model));
        if (json == null) {
            throw new IOException("Missing Java model " + model + " for " + this.version.version());
        }
        return json;
    }

    public JsonObject readVanillaModel(Key model) throws IOException {
        JsonObject json = readJson(this.vanillaRoot.resolve(ResourcePackUtils.modelPath(model)));
        if (json == null) {
            throw new IOException("Missing vanilla Java model " + model + " for " + this.version.version());
        }
        return json;
    }

    private record ResourceEntry(ZipEntry entry, int priority) {
    }
}
