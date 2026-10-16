package net.momirealms.craftengine.core.pack.bedrock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.momirealms.craftengine.core.pack.AbstractPackManager;
import net.momirealms.craftengine.core.pack.ResourcePackView;
import net.momirealms.craftengine.core.pack.mcmeta.Overlay;
import net.momirealms.craftengine.core.pack.mcmeta.Overlays;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class JavaResourcePack implements AutoCloseable {
    private static final Pattern LOCALE = Pattern.compile("[a-z]{2,3}_[a-z0-9]{2,3}");
    private final ResourceReader resources;
    private final MinecraftVersion version;
    private final ZipFile archive;
    private final List<Path> directoryRoots;
    private final List<String> archiveRoots;

    public JavaResourcePack(Path root, Overlays overlays, MinecraftVersion version) {
        this.version = version;
        this.archive = null;
        this.archiveRoots = List.of();
        List<Path> roots = new ArrayList<>();
        roots.add(root);
        if (version.isAtOrAbove(MinecraftVersion.V1_20_2)) {
            for (Overlay overlay : overlays.overlays()) {
                if (overlay.test(version)) {
                    roots.add(root.resolve(overlay.directory()));
                }
            }
        }
        this.directoryRoots = List.copyOf(roots);
        ResourcePackView view = new ResourcePackView(roots.toArray(Path[]::new));
        this.resources = path -> {
            Path resource = view.getExistingReversed(path);
            return resource != null && Files.isRegularFile(resource) ? Files.newInputStream(resource) : null;
        };
    }

    private JavaResourcePack(ZipFile archive, Overlays overlays, MinecraftVersion version) {
        this.version = version;
        this.archive = archive;
        this.directoryRoots = List.of();
        List<String> roots = new ArrayList<>();
        roots.add("");
        for (Overlay overlay : overlays.overlays()) {
            if (version.isAtOrAbove(MinecraftVersion.V1_20_2) && overlay.test(version)) {
                roots.add(overlay.directory() + "/");
            }
        }
        this.archiveRoots = List.copyOf(roots);
        this.resources = path -> {
            for (int index = roots.size() - 1; index >= 0; index--) {
                // ZipFile resolves the central-directory alias, including protected entries ending in '/'.
                ZipEntry entry = archive.getEntry(roots.get(index) + path);
                if (entry != null) {
                    return archive.getInputStream(entry);
                }
            }
            return null;
        };
    }

    public static JavaResourcePack open(Path root, MinecraftVersion version) throws IOException {
        JsonObject metadata = GsonHelper.readJsonObjectFromFile(root.resolve("pack.mcmeta"));
        if (metadata == null) {
            throw new IOException("Missing pack.mcmeta in " + root);
        }
        return new JavaResourcePack(root, new Overlays(metadata), version);
    }

    public static JavaResourcePack openZip(Path source, MinecraftVersion version) throws IOException {
        ZipFile archive = new ZipFile(source.toFile());
        try {
            JavaResourcePack pack = new JavaResourcePack(archive, new Overlays(List.of()), version);
            JsonObject metadata = pack.readJson("pack.mcmeta");
            if (metadata == null || !(metadata.get("pack") instanceof JsonObject)) {
                throw new IOException("Missing pack.mcmeta in " + source);
            }
            return new JavaResourcePack(archive, new Overlays(metadata), version);
        } catch (IOException | RuntimeException | Error exception) {
            archive.close();
            throw exception;
        }
    }

    @Override
    public void close() throws IOException {
        if (this.archive != null) {
            this.archive.close();
        }
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
        if (this.archive != null) {
            Enumeration<? extends ZipEntry> entries = this.archive.entries();
            while (entries.hasMoreElements()) {
                String entry = entries.nextElement().getName();
                for (String prefix : this.archiveRoots) {
                    if (entry.startsWith(prefix)) {
                        String path = entry.substring(prefix.length());
                        // Protected ZIP entries may have a trailing slash while still containing JSON data.
                        if (path.endsWith(".json/")) {
                            path = path.substring(0, path.length() - 1);
                        }
                        if (isLanguageResource(path)) {
                            paths.add(path);
                        }
                    }
                }
            }
        } else {
            for (Path root : this.directoryRoots) {
                Path assets = root.resolve("assets");
                if (!Files.isDirectory(assets)) {
                    continue;
                }
                try (Stream<Path> files = Files.walk(assets, 3)) {
                    files.filter(Files::isRegularFile).forEach(file -> {
                        String path = root.relativize(file).toString().replace('\\', '/');
                        if (isLanguageResource(path)) {
                            paths.add(path);
                        }
                    });
                }
            }
        }
        return paths;
    }

    private static boolean isLanguageResource(String path) {
        if (!path.startsWith("assets/") || !path.endsWith(".json")) {
            return false;
        }
        int namespaceEnd = path.indexOf('/', 7);
        if (namespaceEnd < 0 || !path.startsWith("/lang/", namespaceEnd)) {
            return false;
        }
        return LOCALE.matcher(path.substring(namespaceEnd + 6, path.length() - 5)).matches();
    }

    @Nullable
    public InputStream openResource(String path) throws IOException {
        return this.resources.open(path);
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

    @FunctionalInterface
    private interface ResourceReader {
        @Nullable
        InputStream open(String path) throws IOException;
    }
}
