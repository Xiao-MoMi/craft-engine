package net.momirealms.craftengine.core.pack.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.NAMESPACE;

public final class BedrockDefinitionBundler {
    static final String BLOCK_MODELS = "models/blocks";
    static final String ENTITY_MODELS = "models/entity";
    private static final List<DefinitionType> TYPES = List.of(
            new DefinitionType(BLOCK_MODELS, "minecraft:geometry", ".geo.json"),
            new DefinitionType(ENTITY_MODELS, "minecraft:geometry", ".geo.json"),
            new DefinitionType("animations", "animations", ".animation.json"),
            new DefinitionType("render_controllers", "render_controllers", ".render_controllers.json"),
            new DefinitionType("animation_controllers", "animation_controllers", ".animation_controllers.json")
    );

    private BedrockDefinitionBundler() {
    }

    public static void bundle(Path root) throws IOException {
        bundle(root, new HashMap<>(), false);
    }

    static Map<String, JsonObject> bundle(Path root, Map<Path, JsonObject> models, boolean protection) throws IOException {
        // 保护模式下等引用改写完成再写出合并后的 JSON
        Map<String, JsonObject> deferred = protection ? new LinkedHashMap<>() : null;
        for (DefinitionType type : TYPES) {
            Path directory = root.resolve(type.directory());
            if (Files.exists(directory)) {
                bundleType(directory, type, models, deferred);
            }
        }
        return deferred == null ? Map.of() : deferred;
    }

    private static void bundleType(Path directory, DefinitionType type, Map<Path, JsonObject> models,
                                   @Nullable Map<String, JsonObject> deferred) throws IOException {
        List<Path> files;
        try (Stream<Path> paths = Files.walk(directory)) {
            files = paths.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".json")).sorted().toList();
        }
        if (files.isEmpty()) {
            return;
        }
        Map<JsonObject, Group> groups = new LinkedHashMap<>();
        List<Path> sources = new ArrayList<>(files.size());
        for (Path file : files) {
            JsonObject header = models.remove(file);
            if (header == null) {
                header = GsonHelper.readJsonFromFile(file).getAsJsonObject();
            }
            JsonElement definitions = header.remove(type.key());
            if (definitions == null) {
                continue;
            }
            // 文件头整体参与分组，避免混合加载条件不同的定义
            Group group = groups.get(header);
            if (group == null) {
                group = new Group(type);
                groups.put(header, group);
            }
            if (definitions instanceof JsonArray array) {
                for (JsonElement definition : array) {
                    String identifier = definition.getAsJsonObject().getAsJsonObject("description").get("identifier").getAsString();
                    group.add(identifier, definition);
                }
            } else {
                for (Map.Entry<String, JsonElement> definition : definitions.getAsJsonObject().entrySet()) {
                    group.add(definition.getKey(), definition.getValue());
                }
            }
            sources.add(file);
        }
        Path temporary = deferred == null ? Files.createTempDirectory(directory, "bundles-") : null;
        List<Path> outputs = new ArrayList<>(groups.size());
        int index = 0;
        for (Map.Entry<JsonObject, Group> entry : groups.entrySet()) {
            Group group = entry.getValue();
            if (group.identifiers.isEmpty()) {
                continue;
            }
            JsonObject document = new JsonObject();
            entry.getKey().entrySet().forEach(header -> document.add(header.getKey(), header.getValue()));
            document.add(type.key(), group.definitions);
            String name = index++ + type.suffix();
            if (deferred != null) {
                deferred.put(type.directory() + '/' + NAMESPACE + '/' + name, document);
            } else {
                Path output = temporary.resolve(name);
                GsonHelper.writeJsonFile(document, output);
                outputs.add(output);
            }
        }
        // 先完成临时文件，再替换源文件；重新整理同一目录时也不会覆盖尚未读取的输入
        for (Path source : sources) {
            Files.delete(source);
        }
        Path destination = directory.resolve(NAMESPACE);
        if (!outputs.isEmpty()) {
            Files.createDirectories(destination);
            for (Path output : outputs) {
                Files.move(output, destination.resolve(output.getFileName().toString()));
            }
        }
        if (temporary != null) {
            Files.delete(temporary);
        }
    }

    private record DefinitionType(String directory, String key, String suffix) {
    }

    private static final class Group {
        private final DefinitionType type;
        private final Set<String> identifiers = new HashSet<>();
        private final JsonElement definitions;

        private Group(DefinitionType type) {
            this.type = type;
            this.definitions = type.key().equals("minecraft:geometry") ? new JsonArray() : new JsonObject();
        }

        private void add(String identifier, JsonElement definition) throws IOException {
            if (!this.identifiers.add(identifier)) {
                throw new IOException("Duplicate Bedrock " + this.type.key() + " identifier: " + identifier);
            }
            if (this.definitions instanceof JsonArray array) {
                array.add(definition);
            } else {
                this.definitions.getAsJsonObject().add(identifier, definition);
            }
        }
    }
}
