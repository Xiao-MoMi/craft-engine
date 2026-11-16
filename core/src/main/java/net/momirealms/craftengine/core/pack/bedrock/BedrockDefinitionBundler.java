package net.momirealms.craftengine.core.pack.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class BedrockDefinitionBundler {
    private static final List<DefinitionType> TYPES = List.of(
            new DefinitionType("models/blocks", "minecraft:geometry", ".geo.json"),
            new DefinitionType("models/entity", "minecraft:geometry", ".geo.json"),
            new DefinitionType("animations", "animations", ".animation.json"),
            new DefinitionType("render_controllers", "render_controllers", ".render_controllers.json"),
            new DefinitionType("animation_controllers", "animation_controllers", ".animation_controllers.json")
    );

    private BedrockDefinitionBundler() {
    }

    public static void bundle(Path root, int maximumDefinitions) throws IOException {
        if (maximumDefinitions == 0) {
            return;
        }
        for (DefinitionType type : TYPES) {
            Path directory = root.resolve(type.directory());
            if (Files.exists(directory)) {
                bundleType(directory, type, maximumDefinitions);
            }
        }
    }

    private static void bundleType(Path directory, DefinitionType type, int maximumDefinitions) throws IOException {
        List<Path> files;
        try (Stream<Path> paths = Files.walk(directory)) {
            files = paths.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".json")).sorted().toList();
        }
        if (files.isEmpty()) {
            return;
        }
        Path temporary = Files.createTempDirectory(directory, "bundles-");
        Map<JsonObject, Group> groups = new LinkedHashMap<>();
        List<Path> sources = new ArrayList<>(files.size());
        List<Path> outputs = new ArrayList<>();
        for (Path file : files) {
            JsonObject header = GsonHelper.readJsonFromFile(file).getAsJsonObject();
            JsonElement definitions = header.remove(type.key());
            if (definitions == null) {
                continue;
            }
            // 文件头整体参与分组，避免混合 format_version 或其他加载条件不同的定义。
            Group group = groups.get(header);
            if (group == null) {
                group = new Group(type, header, groups.size(), maximumDefinitions, temporary, outputs);
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
        for (Group group : groups.values()) {
            group.flush();
        }
        // 先完成临时文件，再替换源文件；重新整理同一目录时也不会覆盖尚未读取的输入。
        for (Path source : sources) {
            Files.delete(source);
        }
        Path destination = directory.resolve("craftengine/bundled");
        if (!outputs.isEmpty()) {
            Files.createDirectories(destination);
            for (Path output : outputs) {
                Files.move(output, destination.resolve(output.getFileName().toString()));
            }
        }
        Files.delete(temporary);
    }

    private record DefinitionType(String directory, String key, String suffix) {
    }

    private static final class Group {
        private final DefinitionType type;
        private final JsonObject header;
        private final int index;
        private final int maximumDefinitions;
        private final Path temporary;
        private final List<Path> outputs;
        private final Set<String> identifiers = new HashSet<>();
        private JsonElement definitions;
        private int count;
        private int fileIndex;

        private Group(DefinitionType type, JsonObject header, int index, int maximumDefinitions, Path temporary, List<Path> outputs) {
            this.type = type;
            this.header = header;
            this.index = index;
            this.maximumDefinitions = maximumDefinitions;
            this.temporary = temporary;
            this.outputs = outputs;
        }

        private void add(String identifier, JsonElement definition) throws IOException {
            if (!this.identifiers.add(identifier)) {
                throw new IOException("Duplicate Bedrock " + this.type.key() + " identifier: " + identifier);
            }
            if (this.definitions == null) {
                this.definitions = this.type.key().equals("minecraft:geometry")
                        ? new JsonArray(Math.min(this.maximumDefinitions, 32)) : new JsonObject();
            }
            if (this.definitions instanceof JsonArray array) {
                array.add(definition);
            } else {
                this.definitions.getAsJsonObject().add(identifier, definition);
            }
            if (++this.count == this.maximumDefinitions) {
                this.flush();
            }
        }

        private void flush() throws IOException {
            if (this.count == 0) {
                return;
            }
            JsonObject document = new JsonObject();
            this.header.entrySet().forEach(entry -> document.add(entry.getKey(), entry.getValue()));
            document.add(this.type.key(), this.definitions);
            String name = String.format(Locale.ROOT, "%04d_%06d%s", this.index, this.fileIndex++, this.type.suffix());
            Path output = this.temporary.resolve(name);
            GsonHelper.writeJsonFile(document, output);
            this.outputs.add(output);
            // 每组只保留当前批次的定义，写出后即可释放网格或动画树。
            this.definitions = null;
            this.count = 0;
        }
    }
}
