package net.momirealms.craftengine.core.pack.bedrock;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackMappings;
import net.momirealms.craftengine.bedrock.BedrockPackMappingsCodec;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemSource;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockBlockItems;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemIcons;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemWriter;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockAnimatedTextures;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelConverter;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.HashUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.ZipUtils;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class BedrockPackGenerator {

    public Result generate(Path input, Path output, MinecraftVersion version, Collection<BedrockItemSource> items,
                           JsonObject blockAtlas, JsonObject itemAtlas, int iconSize, Consumer<String> warning) throws IOException {
        if (input.toAbsolutePath().normalize().equals(output.toAbsolutePath().normalize())) {
            throw new IOException("Bedrock output must not overwrite the input Java resource pack");
        }
        if (!Files.isRegularFile(input)) {
            throw new IOException("Java resource pack does not exist: " + input);
        }
        if (version.isBelow(MinecraftVersion.V1_21_4)) {
            throw new IOException("Bedrock generation requires a pack target of Java 1.21.4 or newer (items definitions)");
        }
        try (JavaResourcePack pack = JavaResourcePack.openZip(input, version);
             FileSystem generated = Jimfs.newFileSystem(Configuration.unix())) {
            Path root = generated.getPath("/bedrock");
            Files.createDirectories(root);
            JavaModelResolver resolver = new JavaModelResolver(pack);
            JavaPackTextureSource textures = new JavaPackTextureSource(pack, blockAtlas, itemAtlas);
            BedrockModelConverter converter = new BedrockModelConverter();
            List<BedrockItemSource> orderedItems = new ArrayList<>(items);
            orderedItems.sort(Comparator.comparing(item -> item.id().asString()));
            Map<ModelVariant, List<BedrockItemSource>> models = collectModels(pack, orderedItems, warning);
            BedrockItemIcons icons = new BedrockItemIcons(iconSize);
            BedrockBlockItems blockItems = new BedrockBlockItems();
            List<BedrockPackMappings.Item> mappings = new ArrayList<>();
            int convertedModels = 0;
            for (Map.Entry<ModelVariant, List<BedrockItemSource>> entry : models.entrySet()) {
                ModelVariant variant = entry.getKey();
                List<BedrockItemSource> users = entry.getValue();
                Key resource = users.getFirst().id();
                BedrockModelConverter.Result converted;
                BedrockPackMappings.DisplayBlock displayBlock;
                List<BedrockAnimatedTextures.Track> animations;
                try {
                    JavaModel model = resolver.resolve(variant.model());
                    converted = converter.convert(model, "geometry." + resource.namespace() + "." + resource.value(), textures, variant.tints());
                    animations = BedrockAnimatedTextures.write(converted, resource, root);
                    displayBlock = model.geometryKind() == JavaModel.GeometryKind.GENERATED && animations.isEmpty()
                            ? null : blockItems.write(converted, resource, root, animations);
                } catch (IOException | IllegalArgumentException exception) {
                    for (BedrockItemSource item : users) {
                        warning.accept("Skipping Bedrock item " + item.id() + " (" + item.itemModel() + "): " + exception.getMessage());
                    }
                    continue;
                }
                // Convert and write shared geometry/texture/animations once, then release the image before the next model.
                BedrockItemWriter.write(converted, resource, root, animations);
                String icon = displayBlock == null ? icons.write(converted, resource, variant.tints(), root) : null;
                for (BedrockItemSource item : users) {
                    if (!item.id().equals(resource)) {
                        BedrockItemWriter.writeAttachable(item.id(), resource, converted.geometry().identifier(), root, animations);
                    }
                    mappings.add(new BedrockPackMappings.Item(item.id().asString(), item.javaItem().asString(), item.itemModel().asString(), item.id().asString(), icon, displayBlock));
                }
                convertedModels++;
            }
            icons.writeAtlas(root);
            blockItems.writeAtlas(root);
            BedrockLanguageWriter.write(pack, root, mappings, warning);
            BedrockPackMappings metadata = new BedrockPackMappings(BedrockPackMappings.FORMAT_VERSION, version.version(), HashUtils.sha1(input), mappings);
            Path metadataPath = root.resolve(BedrockPackMappings.PATH);
            Files.createDirectories(metadataPath.getParent());
            try (Writer writer = Files.newBufferedWriter(metadataPath, StandardCharsets.UTF_8)) {
                BedrockPackMappingsCodec.write(metadata, writer);
            }
            GsonHelper.writeJsonFile(manifest(root, !blockItems.isEmpty()), root.resolve("manifest.json"));
            Path destination = output.toAbsolutePath().normalize();
            Files.createDirectories(destination.getParent());
            Path temporary = Files.createTempFile(destination.getParent(), "bedrock-pack-", ".zip");
            try {
                ZipUtils.compress(root, temporary);
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
            return new Result(mappings.size(), convertedModels, items.size() - mappings.size());
        }
    }

    private Map<ModelVariant, List<BedrockItemSource>> collectModels(JavaResourcePack pack, List<BedrockItemSource> items, Consumer<String> warning) {
        Map<ModelVariant, List<BedrockItemSource>> models = new LinkedHashMap<>();
        for (BedrockItemSource item : items) {
            try {
                JsonObject definition = pack.readJson("assets/" + item.itemModel().namespace() + "/items/" + item.itemModel().value() + ".json");
                if (definition == null || !(definition.get("model") instanceof JsonObject model)) {
                    throw new IOException("Missing item definition in input ZIP");
                }
                String type = model.has("type") ? model.get("type").getAsString() : "";
                if (!type.equals("minecraft:model") && !type.equals("model")) {
                    throw new IOException("Unsupported item definition type: " + type);
                }
                List<Integer> tints = this.constantTints(model);
                JsonElement reference = model.get("model");
                if (reference == null || !reference.isJsonPrimitive()) {
                    throw new IOException("Missing static model reference");
                }
                models.computeIfAbsent(new ModelVariant(Key.of(reference.getAsString()), tints), key -> new ArrayList<>()).add(item);
            } catch (IOException | RuntimeException exception) {
                warning.accept("Skipping Bedrock item " + item.id() + " (" + item.itemModel() + "): " + exception.getMessage());
            }
        }
        return models;
    }

    private List<Integer> constantTints(JsonObject model) throws IOException {
        if (!model.has("tints") || model.getAsJsonArray("tints").isEmpty()) {
            return List.of();
        }
        JsonArray definitions = model.getAsJsonArray("tints");
        List<Integer> tints = new ArrayList<>(definitions.size());
        for (JsonElement definition : definitions) {
            JsonObject tint = definition.getAsJsonObject();
            String type = tint.get("type").getAsString();
            if (!type.equals("constant") && !type.equals("minecraft:constant")) {
                throw new IOException("Unsupported dynamic item tint: " + type);
            }
            tints.add(tint.get("value").getAsInt() & 0xFFFFFF);
        }
        return List.copyOf(tints);
    }

    private record ModelVariant(Key model, List<Integer> tints) {
    }

    private JsonObject manifest(Path root, boolean blockItems) throws IOException {
        StringBuilder identity = new StringBuilder();
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                identity.append(root.relativize(path)).append('\n').append(HashUtils.sha1(path)).append('\n');
            }
        }
        JsonObject header = new JsonObject();
        header.addProperty("name", "CraftEngine Bedrock resources");
        header.addProperty("description", "Generated from the final Java resource pack");
        header.addProperty("uuid", UUID.nameUUIDFromBytes(("craftengine:bedrock:1:" + identity).getBytes(StandardCharsets.UTF_8)).toString());
        JsonArray version = new JsonArray();
        version.add(1);
        version.add(0);
        version.add(0);
        header.add("version", version);
        JsonArray engineVersion = new JsonArray();
        engineVersion.add(1);
        engineVersion.add(21);
        engineVersion.add(blockItems ? 30 : 0);
        header.add("min_engine_version", engineVersion);
        JsonObject module = new JsonObject();
        module.addProperty("type", "resources");
        module.addProperty("uuid", UUID.nameUUIDFromBytes(("craftengine:bedrock:resources:1:" + identity).getBytes(StandardCharsets.UTF_8)).toString());
        module.add("version", version);
        JsonArray modules = new JsonArray();
        modules.add(module);
        JsonObject manifest = new JsonObject();
        manifest.addProperty("format_version", 2);
        manifest.add("header", header);
        manifest.add("modules", modules);
        return manifest;
    }

    public record Result(int items, int models, int skipped) {
    }
}
