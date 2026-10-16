package net.momirealms.craftengine.core.pack.bedrock;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackMappings;
import net.momirealms.craftengine.bedrock.BedrockPackMappingsCodec;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.pack.bedrock.item.JavaItemModelLeaf;
import net.momirealms.craftengine.core.pack.bedrock.item.JavaBowModel;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockBowWriter;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemSource;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemSources;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemRendering;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockBlockItems;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemIcons;
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
            BedrockItemSources sources = new BedrockItemSources(items);
            CollectedModels models = collectModels(pack, sources, warning);
            BedrockItemIcons icons = new BedrockItemIcons(iconSize);
            BedrockBlockItems blockItems = new BedrockBlockItems();
            List<BedrockPackMappings.Item> mappings = new ArrayList<>();
            int convertedModels = 0;
            for (Map.Entry<JavaItemModelLeaf, List<BedrockItemSource>> entry : models.staticModels().entrySet()) {
                JavaItemModelLeaf variant = entry.getKey();
                List<BedrockItemSource> users = entry.getValue();
                Key resource = users.getFirst().id();
                BedrockModelConverter.Result converted;
                BedrockPackMappings.DisplayBlock displayBlock;
                List<BedrockAnimatedTextures.Track> animations;
                try {
                    JavaModel model = resolver.resolve(variant.model());
                    converted = converter.convert(model, "geometry." + resource.namespace() + "." + resource.value(), textures, variant.tints());
                    animations = BedrockAnimatedTextures.write(converted, resource, root, false);
                    displayBlock = model.geometryKind() == JavaModel.GeometryKind.GENERATED && animations.isEmpty()
                            ? null : blockItems.write(converted, resource, root, animations);
                } catch (IOException | IllegalArgumentException exception) {
                    for (BedrockItemSource item : users) {
                        warning.accept("Skipping Bedrock item " + item.id() + " (" + item.itemModel() + "): " + exception.getMessage());
                    }
                    continue;
                }
                // Native icons and block geometry retain Bedrock's held rendering; attachables are reserved for stateful items.
                String icon = displayBlock == null ? icons.write(converted, resource, variant.tints(), root) : null;
                for (BedrockItemSource item : users) {
                    mappings.add(new BedrockPackMappings.Item(item.id().asString(), item.javaItem().asString(), item.itemModel().asString(),
                            item.id().asString(), icon, displayBlock, BedrockItemRendering.displayHandheld(item.itemModel(), sources)));
                }
                convertedModels++;
            }
            BedrockBowWriter bows = new BedrockBowWriter(resolver, textures, icons, blockItems, root);
            for (Map.Entry<JavaBowModel, List<BedrockItemSource>> entry : models.bows().entrySet()) {
                try {
                    BedrockBowWriter.Result bow = bows.write(entry.getKey(), entry.getValue());
                    for (BedrockItemSource item : entry.getValue()) {
                        mappings.add(new BedrockPackMappings.Item(item.id().asString(), item.javaItem().asString(), item.itemModel().asString(),
                                item.id().asString(), bow.icon(), bow.displayBlock(), BedrockItemRendering.displayHandheld(item.itemModel(), sources)));
                    }
                    convertedModels += bow.models();
                } catch (IOException | IllegalArgumentException exception) {
                    for (BedrockItemSource item : entry.getValue()) {
                        warning.accept("Skipping Bedrock bow " + item.id() + " (" + item.itemModel() + "): " + exception.getMessage());
                    }
                }
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

    private CollectedModels collectModels(JavaResourcePack pack, BedrockItemSources sources, Consumer<String> warning) {
        Map<JavaItemModelLeaf, List<BedrockItemSource>> models = new LinkedHashMap<>();
        Map<JavaBowModel, List<BedrockItemSource>> bows = new LinkedHashMap<>();
        for (BedrockItemSource item : sources.items()) {
            try {
                JsonObject definition = pack.readJson("assets/" + item.itemModel().namespace() + "/items/" + item.itemModel().value() + ".json");
                if (definition == null || !(definition.get("model") instanceof JsonObject model)) {
                    throw new IOException("Missing item definition in input ZIP");
                }
                BedrockItemSource definitionSource = sources.findItemByModel(item.itemModel());
                if (definitionSource != null && definitionSource.javaItem().equals(ItemKeys.BOW)) {
                    bows.computeIfAbsent(JavaBowModel.read(model), key -> new ArrayList<>()).add(item);
                } else {
                    models.computeIfAbsent(JavaItemModelLeaf.read(model), key -> new ArrayList<>()).add(item);
                }
            } catch (IOException | RuntimeException exception) {
                warning.accept("Skipping Bedrock item " + item.id() + " (" + item.itemModel() + "): " + exception.getMessage());
            }
        }
        return new CollectedModels(models, bows);
    }

    private record CollectedModels(Map<JavaItemModelLeaf, List<BedrockItemSource>> staticModels, Map<JavaBowModel, List<BedrockItemSource>> bows) {
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
