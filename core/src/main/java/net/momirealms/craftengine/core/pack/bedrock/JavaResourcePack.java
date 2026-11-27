package net.momirealms.craftengine.core.pack.bedrock;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
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
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

// 单版本、去除 overlay 后的资源视图。普通读取直接打开 ZIP 条目，只有需要 Path 的声音调用才物化文件。
public final class JavaResourcePack implements AutoCloseable {
    private static final int MAX_PATH = 260;
    private static final Configuration INPUT_FILE_SYSTEM = Configuration.forCurrentPlatform().toBuilder().setBlockSize(4096).build();
    private final MinecraftVersion version;
    private final Layer custom;
    private final Layer vanilla;
    private final VanillaResourcePackDownloader.IndexedAssets indexedAssets;
    private final BiPredicate<String, String> vanillaFilter;
    private final FileSystem ownedFileSystem;

    private JavaResourcePack(Layer custom, Layer vanilla, MinecraftVersion version, @Nullable BiPredicate<String, String> filter,
                             Path vanillaDirectory, FileSystem ownedFileSystem) {
        this.custom = custom;
        this.vanilla = vanilla;
        this.version = version;
        this.vanillaFilter = filter;
        this.indexedAssets = new VanillaResourcePackDownloader.IndexedAssets(vanillaDirectory);
        this.ownedFileSystem = ownedFileSystem;
    }

    public static JavaResourcePack openZip(Path source, Path vanilla, MinecraftVersion version) throws IOException {
        FileSystem fileSystem = Jimfs.newFileSystem(INPUT_FILE_SYSTEM);
        try {
            return open(source, vanilla, version, fileSystem);
        } catch (Throwable exception) {
            fileSystem.close();
            throw exception;
        }
    }

    private static JavaResourcePack open(Path source, Path vanilla, MinecraftVersion version, FileSystem fileSystem) throws IOException {
        IndexedZipFile customArchive = new IndexedZipFile(source);
        try {
            ZipEntry metadata = customArchive.getEntry("pack.mcmeta");
            if (metadata == null) {
                throw new IOException("Missing pack.mcmeta in " + source);
            }
            JsonObject object;
            try (Reader reader = new InputStreamReader(customArchive.getInputStream(metadata), StandardCharsets.UTF_8)) {
                object = JsonParser.parseReader(reader).getAsJsonObject();
            }
            if (object == null || !(object.get("pack") instanceof JsonObject)) {
                throw new IOException("Missing pack.mcmeta in " + source);
            }
            PackMeta meta = new PackMeta(object);
            Layer custom = new Layer(fileSystem.getPath("resource_pack").toAbsolutePath(), customArchive, meta.overlays().overlays(), version);
            ZipEntry icon = customArchive.getEntry("pack.png");
            if (icon != null) {
                custom.files.put(custom.root.resolve("pack.png"), icon);
            }

            customArchive.releaseIndex();
            IndexedZipFile vanillaArchive = new IndexedZipFile(vanilla);
            try {
                Layer original = new Layer(fileSystem.getPath("vanilla").toAbsolutePath(), vanillaArchive, List.of(), version);
                vanillaArchive.releaseIndex();
                Path directory = vanilla.toAbsolutePath().getParent();
                Path fonts = directory.resolve("assets/minecraft/font");
                if (Files.isDirectory(fonts)) {
                    try (Stream<Path> paths = Files.walk(fonts)) {
                        Iterator<Path> iterator = paths.filter(Files::isRegularFile).iterator();
                        while (iterator.hasNext()) {
                            Path font = iterator.next();
                            Path key = original.root.resolve("assets/minecraft/font").resolve(CharacterUtils.replaceBackslashWithSlash(fonts.relativize(font).toString()));
                            original.external.put(key, font);
                            original.files.putIfAbsent(key, null);
                        }
                    }
                }
                custom.groupFiles();
                original.groupFiles();
                return new JavaResourcePack(custom, original, version, meta.filter() == null ? null : meta.filter().compile(),
                        directory, fileSystem);
            } catch (Throwable exception) {
                vanillaArchive.close();
                throw exception;
            }
        } catch (Throwable exception) {
            customArchive.close();
            throw exception;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            this.custom.close();
        } finally {
            try {
                this.vanilla.close();
            } finally {
                this.ownedFileSystem.close();
            }
        }
    }

    private static final class Layer implements AutoCloseable {
        private final Path root;
        private final Map<Path, ZipEntry> files = new HashMap<>();
        private final Map<Path, Path> external = new HashMap<>();
        private final Map<String, List<Path>> groups = new HashMap<>();
        private final Set<String> soundNamespaces = new TreeSet<>();
        private final IndexedZipFile archive;

        private Layer(Path root, IndexedZipFile archive, List<Overlay> overlays, MinecraftVersion version) throws IOException {
            this.root = root;
            this.archive = archive;
            Map<String, Integer> priorities = new HashMap<>();
            int priority = 0;
            for (Overlay overlay : overlays) {
                if (version.isAtOrAbove(MinecraftVersion.V1_20_2) && overlay.test(version)) {
                    priorities.put(overlay.directory(), ++priority);
                }
            }
            Map<String, ResourceEntry> selected = new HashMap<>();
            for (ZipEntry entry : archive.entries()) {
                if ((entry.isDirectory() && entry.getSize() == 0) || entry.getName().length() >= MAX_PATH) {
                    continue;
                }
                String name = entry.getName();
                if (name.startsWith("assets/")) {
                    index(name, entry, 0, selected);
                }
                int separator = name.indexOf('/');
                if (separator >= 0 && name.startsWith("assets/", separator + 1)) {
                    Integer overlayPriority = priorities.get(name.substring(0, separator));
                    if (overlayPriority != null) {
                        index(name.substring(separator + 1), entry, overlayPriority, selected);
                    }
                }
            }
            Path assets = root.resolve("assets");
            Set<Path> directories = new HashSet<>();
            directories.add(assets);
            for (Map.Entry<String, ResourceEntry> resource : selected.entrySet()) {
                Path path = root.resolve(resource.getKey()).normalize();
                if (!path.startsWith(assets)) {
                    throw new IOException("Resource path outside assets: " + resource.getKey());
                }
                if (this.files.put(path, resource.getValue().entry()) != null) {
                    throw new FileAlreadyExistsException(path.toString());
                }
                // 收集真实目录，受保护 ZIP 可能同时包含同路径的空文件占位。
                Path parent = path.getParent();
                while (parent != null && parent.startsWith(assets) && directories.add(parent)) {
                    parent = parent.getParent();
                }
            }
            for (Path directory : directories) {
                ZipEntry placeholder = this.files.remove(directory);
                if (placeholder != null && placeholder.getSize() != 0) {
                    throw new FileAlreadyExistsException(directory.toString());
                }
            }
        }

        private static void index(String name, ZipEntry entry, int priority, Map<String, ResourceEntry> selected) {
            String path = entry.isDirectory() ? name.substring(0, name.length() - 1) : name;
            ResourceEntry previous = selected.get(path);
            if (previous == null || priority > previous.priority() || (priority == previous.priority() && previous.entry().isDirectory() && !entry.isDirectory())) {
                selected.put(path, new ResourceEntry(entry, priority));
            }
        }

        private void groupFiles() {
            for (Path path : this.files.keySet()) {
                Path relative = this.root.relativize(path);
                int count = relative.getNameCount();
                if (count < 3) {
                    continue;
                }
                String type = relative.getName(2).toString();
                if ((count == 3 && type.equals("sounds.json")) || (count > 3 && type.equals("sounds"))) {
                    this.soundNamespaces.add(relative.getName(1).toString());
                }
                if (count < 4) {
                    continue;
                }
                if (type.equals("models") && count >= 5 && relative.getName(3).toString().equals("item")) {
                    type = "models/item";
                }
                if (type.equals("font") || type.equals("items") || type.equals("models/item") || type.equals("sounds") || type.equals("lang")) {
                    this.groups.computeIfAbsent(type, ignored -> new ArrayList<>()).add(path);
                }
            }
        }

        private List<Path> group(String name) {
            return this.groups.getOrDefault(name, List.of());
        }

        private boolean has(String path) {
            return this.files.containsKey(this.root.resolve(path));
        }

        @Nullable
        private InputStream open(String name) throws IOException {
            return this.open(this.root.resolve(name));
        }

        @Nullable
        private InputStream open(Path key) throws IOException {
            Path externalFile = this.external.get(key);
            if (externalFile != null) {
                return Files.newInputStream(externalFile);
            }
            ZipEntry entry = this.files.get(key);
            return entry == null ? null : this.archive.getInputStream(entry);
        }

        @Nullable
        private JsonObject json(String path) throws IOException {
            return this.json(this.root.resolve(path));
        }

        @Nullable
        private JsonObject json(Path key) throws IOException {
            try (InputStream input = this.open(key)) {
                if (input == null) {
                    return null;
                }
                try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8.newDecoder())) {
                    JsonElement element = JsonParser.parseReader(reader);
                    return element.isJsonObject() ? element.getAsJsonObject() : null;
                }
            }
        }

        @Nullable
        private synchronized Path materialize(Path key) throws IOException {
            if (!this.files.containsKey(key)) {
                return null;
            }
            if (!Files.isRegularFile(key)) {
                Files.createDirectories(key.getParent());
                try (InputStream input = this.open(key)) {
                    Files.copy(input, key);
                } catch (IOException exception) {
                    Files.deleteIfExists(key);
                    throw exception;
                }
            }
            return key;
        }

        @Override
        public void close() throws IOException {
            this.archive.close();
        }
    }

    private record ResourceEntry(ZipEntry entry, int priority) {
    }

    @Nullable
    public Path soundResource(String namespace, String name, boolean custom) throws IOException {
        if (namespace.isEmpty() || namespace.equals(".") || namespace.equals("..")
                || namespace.indexOf('/') >= 0 || namespace.indexOf('\\') >= 0 || namespace.indexOf(':') >= 0
                || name.indexOf('\\') >= 0 || name.equals("..") || name.startsWith("../") || name.endsWith("/..") || name.contains("/../")) {
            return null;
        }
        String path = "assets/" + namespace + '/' + name;
        if (!custom && this.filtersVanilla(path)) {
            return null;
        }
        Layer layer = custom ? this.custom : this.vanilla;
        Path resource = layer.materialize(layer.root.resolve(path));
        return resource != null || custom ? resource : this.indexedAssets.find(namespace + '/' + name);
    }

    @Nullable
    public JsonObject readJson(String path) throws IOException {
        return this.resource(path).json(path);
    }

    @Nullable
    public JsonObject readVanillaJson(String path) throws IOException {
        return this.vanilla.json(path);
    }

    @Nullable
    public JsonObject readParticleJson(Key key) throws IOException {
        return this.readJson(ResourcePackUtils.particleDefinitionPath(key));
    }

    public List<JsonObject> readAtlas(Key atlas) throws IOException {
        String path = "assets/" + atlas.namespace() + "/atlases/" + atlas.value() + ".json";
        List<JsonObject> sources = new ObjectArrayList<>(2);
        JsonObject original = this.filtersVanilla(path) ? null : this.vanilla.json(path);
        if (original != null) {
            sources.add(original);
        }
        JsonObject custom = this.custom.json(path);
        if (custom != null) {
            sources.add(custom);
        }
        return sources;
    }

    @Nullable
    public JsonObject readResourceMetadata(String path) throws IOException {
        return this.hasCustomResource(path) ? this.custom.json(path + ".mcmeta") : this.readJson(path + ".mcmeta");
    }

    public MinecraftVersion version() {
        return this.version;
    }

    public Map<Key, List<FontResource>> fonts() throws IOException {
        Map<Key, List<FontResource>> fonts = new TreeMap<>(Comparator.comparing(Key::asString));
        this.collectFonts(this.custom, true, fonts);
        this.collectFonts(this.vanilla, false, fonts);
        return fonts;
    }

    private void collectFonts(Layer layer, boolean custom, Map<Key, List<FontResource>> fonts) {
        for (Path path : layer.group("font")) {
            Path relative = layer.root.relativize(path);
            if (!path.toString().endsWith(".json")) {
                continue;
            }
            String namespace = relative.getName(1).toString();
            String name = CharacterUtils.replaceBackslashWithSlash(relative.subpath(3, relative.getNameCount()).toString());
            if (!custom && this.vanillaFilter != null && this.vanillaFilter.test(namespace, "font/" + name)) {
                continue;
            }
            Key id = Key.of(namespace, name.substring(0, name.length() - 5));
            try {
                JsonObject definition = layer.json(path);
                if (definition == null || !(definition.get("providers") instanceof JsonArray)) {
                    TranslationManager.warn("resource_pack.bedrock.font.definition", id.asString(), path.toString());
                    continue;
                }
                fonts.computeIfAbsent(id, ignored -> new ObjectArrayList<>(2)).add(new FontResource(definition, custom));
            } catch (IOException | JsonParseException exception) {
                TranslationManager.warn("resource_pack.bedrock.font.read", id.asString(), path.toString(), exception.toString());
            }
        }
    }

    public record FontResource(JsonObject definition, boolean custom) {
    }

    public JsonObject translations() throws IOException {
        JsonObject translations = new JsonObject();
        List<Path> files = new ArrayList<>();
        for (Path path : this.custom.group("lang")) {
            Path relative = this.custom.root.relativize(path);
            String name = path.getFileName().toString();
            if (relative.getNameCount() == 4 && name.endsWith(".json") && !name.equals("deprecated.json")) {
                files.add(path);
            }
        }
        files.sort(Comparator.naturalOrder());
        for (Path file : files) {
            try {
                JsonObject source = this.custom.json(file);
                if (source == null) {
                    throw new IOException("Missing or non-object language file " + file);
                }
                if (!source.isEmpty()) {
                    String name = file.getFileName().toString();
                    String locale = name.substring(0, name.length() - 5);
                    JsonObject language = translations.getAsJsonObject(locale);
                    if (language == null) {
                        language = new JsonObject();
                        translations.add(locale, language);
                    }
                    for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
                        language.addProperty(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            } catch (RuntimeException exception) {
                throw new IOException("Failed to read language file " + file, exception);
            }
        }
        return GsonHelper.sortObjectKeys(translations).getAsJsonObject();
    }

    public Map<Key, JsonObject> vanillaItemDefinitions() throws IOException {
        return this.itemDefinitions(this.vanilla, true);
    }

    public void validateItemDefinitions() throws IOException {
        // 未被注册选中的定义也必须报告无效 JSON，此处只验证语法，实际使用时才构造对象树。
        for (Path path : this.custom.group("items")) {
            if (!path.toString().endsWith(".json")) {
                continue;
            }
            Path relative = this.custom.root.relativize(path);
            String name = CharacterUtils.replaceBackslashWithSlash(relative.subpath(3, relative.getNameCount()).toString());
            Key id = Key.of(relative.getName(1).toString(), name.substring(0, name.length() - 5));
            String source = ResourcePackUtils.itemDefinitionPath(id);
            try (InputStream stream = this.custom.open(path);
                 JsonReader reader = new JsonReader(new InputStreamReader(stream, StandardCharsets.UTF_8.newDecoder()))) {
                reader.setStrictness(Strictness.LENIENT);
                if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                    throw new IOException("Missing or non-object item definition " + id + " at " + source);
                }
                reader.skipValue();
                reader.setStrictness(Strictness.LEGACY_STRICT);
                if (reader.peek() != JsonToken.END_DOCUMENT) {
                    throw new IOException("Trailing data in " + source);
                }
            } catch (IOException | JsonParseException exception) {
                throw new IOException("Failed to read item definition " + id + " at " + source, exception);
            }
        }
    }

    public Map<Key, JsonObject> itemDefinitions() throws IOException {
        return this.itemDefinitions(this.custom, false);
    }

    private Map<Key, JsonObject> itemDefinitions(Layer layer, boolean vanilla) throws IOException {
        Map<Key, JsonObject> definitions = new TreeMap<>(Comparator.comparing(Key::asString));
        Path vanillaItems = layer.root.resolve("assets/minecraft/items");
        Path vanillaModels = layer.root.resolve("assets/minecraft/models/item");
        boolean modern = !vanilla || layer.group("items").stream().anyMatch(path -> path.startsWith(vanillaItems));
        for (Path path : layer.group(modern ? "items" : "models/item")) {
            Path relative = layer.root.relativize(path);
            if (!path.getFileName().toString().endsWith(".json") || (!modern && !path.startsWith(vanillaModels))) {
                continue;
            }
            int start = modern ? 3 : 4;
            String namespace = relative.getName(1).toString();
            String name = CharacterUtils.replaceBackslashWithSlash(relative.subpath(start, relative.getNameCount()).toString());
            if (vanilla && this.vanillaFilter != null && this.vanillaFilter.test(namespace, (modern ? "items/" : "models/item/") + name)) {
                continue;
            }
            Key id = Key.of(namespace, name.substring(0, name.length() - 5));
            if (modern) {
                JsonObject definition;
                try {
                    definition = layer.json(path);
                } catch (IOException | JsonParseException exception) {
                    throw new IOException("Failed to read item definition " + id + " at " + ResourcePackUtils.itemDefinitionPath(id), exception);
                }
                if (definition == null) {
                    throw new IOException("Missing or non-object item definition " + id + " at " + ResourcePackUtils.itemDefinitionPath(id));
                }
                definitions.put(id, definition);
            } else {
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

    @Nullable
    public InputStream openCustomResource(String path) throws IOException {
        return this.custom.open(path);
    }

    public boolean hasCustomResource(String path) {
        return this.custom.has(path);
    }

    public Set<Key> customSoundFiles() throws IOException {
        Set<Key> files = new HashSet<>();
        for (Path path : this.custom.group("sounds")) {
            Path relative = this.custom.root.relativize(path);
            if (path.toString().endsWith(".ogg")) {
                String name = CharacterUtils.replaceBackslashWithSlash(relative.subpath(3, relative.getNameCount()).toString());
                files.add(Key.of(relative.getName(1).toString(), name.substring(0, name.length() - 4)));
            }
        }
        return files;
    }

    public List<String> soundNamespaces() throws IOException {
        return List.copyOf(this.custom.soundNamespaces);
    }

    @Nullable
    public InputStream openResource(String path) throws IOException {
        return this.resource(path).open(path);
    }

    private Layer resource(String path) {
        return this.custom.has(path) || this.filtersVanilla(path) ? this.custom : this.vanilla;
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
        JsonObject json = this.readVanillaJson(ResourcePackUtils.modelPath(model));
        if (json == null) {
            throw new IOException("Missing vanilla Java model " + model + " for " + this.version.version());
        }
        return json;
    }
}
