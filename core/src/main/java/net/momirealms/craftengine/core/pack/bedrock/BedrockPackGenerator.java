package net.momirealms.craftengine.core.pack.bedrock;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackMappings;
import net.momirealms.craftengine.bedrock.BedrockPackMappingsCodec;
import net.momirealms.craftengine.core.pack.VanillaResourcePackDownloader;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockBlockItems;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemIcons;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemModelSupport;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemRendering;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemSource;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemSources;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockNativeItemWriter;
import net.momirealms.craftengine.core.pack.bedrock.lang.BedrockLanguageWriter;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTextureRegistry;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.bedrock.texture.vanilla.BedrockVanillaTextures;
import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.pack.model.definition.analysis.ItemModelProgram;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.HashUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.VersionHelper;
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
import java.util.stream.Stream;

public final class BedrockPackGenerator {

    public Result generate(Path input,
                           Path output,
                           MinecraftVersion version,
                           Collection<BedrockItemSource> items) throws IOException {
        // TODO 使用本地化语言与1.21.1以下版本分支
        if (!VersionHelper.isOrAbove1_21_2) {
            throw new IOException("Bedrock generation requires a server version of Java 1.21.2 or newer");
        }
        if (input.toAbsolutePath().normalize().equals(output.toAbsolutePath().normalize())) {
            throw new IOException("Bedrock output must not overwrite the input Java resource pack");
        }
        if (!Files.isRegularFile(input)) {
            throw new IOException("Java resource pack does not exist: " + input);
        }
        if (version.isBelow(MinecraftVersion.V1_21_4)) {
            throw new IOException("Bedrock generation requires a pack target of Java 1.21.4 or newer (items definitions)");
        }

        Path cacheDirectory = CraftEngine.instance().dataFolderPath().resolve("cache/vanilla");
        Path vanilla = new VanillaResourcePackDownloader(cacheDirectory).download(version);
        try (FileSystem fileSystem = Jimfs.newFileSystem(Configuration.forCurrentPlatform())) {
            JavaResourcePack pack = JavaResourcePack.openZip(input, vanilla, version, fileSystem);
            Path root = fileSystem.getPath("bedrock").toAbsolutePath();
            Files.createDirectories(root);
            // 将原版贴图转写成 bedrock 路径
            BedrockTextureRegistry textureRegistry = new BedrockTextureRegistry();
            BedrockVanillaTextures.write(pack, root, textureRegistry);

            // 专门读 java 模型
            JavaModelResolver resolver = new JavaModelResolver(pack);
            // 解析 sprite 到实际的 image
            JavaPackTextureSource textures = new JavaPackTextureSource(pack);
            BedrockItemSources sources = new BedrockItemSources(pack, items);

            Map<BaseItemModel, ModelUsers> models = this.collectModels(sources);

            BedrockItemIcons icons = new BedrockItemIcons(Config.bedrockItemIconSize(), textureRegistry);
            BedrockBlockItems blockItems = new BedrockBlockItems(textureRegistry);
            BedrockNativeItemWriter nativeItems = new BedrockNativeItemWriter(resolver, textures, icons, blockItems, root);
            List<BedrockPackMappings.Item> mappings = new ArrayList<>();

            int convertedModels = 0;
            for (Map.Entry<BaseItemModel, ModelUsers> entry : models.entrySet()) {
                BaseItemModel variant = entry.getKey();
                List<BedrockItemSource> users = entry.getValue().items();
                Key resource = entry.getValue().resource();
                BedrockNativeItemWriter.Appearance appearance;
                try {
                    appearance = nativeItems.write(variant, resource);
                } catch (IOException | IllegalArgumentException exception) {
                    CraftEngine.instance().logger().warn("Skipping Bedrock item definition " + entry.getValue().definition() + ": " + exception.getMessage());
                    continue;
                }
                for (BedrockItemSource item : users) {
                    mappings.add(new BedrockPackMappings.Item(item.id().asString(), item.javaItem().asString(), item.itemModel().asString(),
                            item.bedrockIdentifier(), appearance.icon(), blockItems.applyOptions(appearance.displayBlock(), item.bedrockOptions()), BedrockItemRendering.displayHandheld(item)));
                }
                convertedModels++;
            }
            textureRegistry.write(root);
            blockItems.writeBlocks(root);
            BedrockLanguageWriter.write(pack, root, mappings);
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
            return new Result(mappings.size(), convertedModels, sources.items().size() - mappings.size());
        }
    }

    private Map<BaseItemModel, ModelUsers> collectModels(BedrockItemSources sources) {
        Map<BaseItemModel, ModelUsers> models = new LinkedHashMap<>();
        for (Map.Entry<Key, ItemModelProgram> definition : sources.definitions().entrySet()) {
            Key id = definition.getKey();
            try {
                BaseItemModel model = BedrockItemModelSupport.requireStaticModel(definition.getValue());
                ModelUsers users = models.computeIfAbsent(model, key -> new ModelUsers(id,
                        Key.of("ce", "static_" + HashUtils.sha1(id.asString().getBytes(StandardCharsets.UTF_8))), new ArrayList<>()));
                users.items().addAll(sources.items(id));
            } catch (IOException exception) {
                CraftEngine.instance().logger().warn("Skipping Bedrock item definition " + id + ": " + exception.getMessage());
            }
        }
        return models;
    }

    private record ModelUsers(Key definition, Key resource, List<BedrockItemSource> items) {
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
