package net.momirealms.craftengine.core.pack.bedrock;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackMappings;
import net.momirealms.craftengine.bedrock.BedrockPackMappingsCodec;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.pack.VanillaResourcePackDownloader;
import net.momirealms.craftengine.core.pack.bedrock.animation.BedrockPlayerAnimations;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockBlockItems;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockBowWriter;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockCrossbowWriter;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemIcons;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemRendering;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemSource;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemSources;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockNativeItemWriter;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockSpearWriter;
import net.momirealms.craftengine.core.pack.bedrock.item.JavaBowModel;
import net.momirealms.craftengine.core.pack.bedrock.item.JavaCrossbowModel;
import net.momirealms.craftengine.core.pack.bedrock.item.JavaDisplayContextModel;
import net.momirealms.craftengine.core.pack.bedrock.item.JavaFishingRodModel;
import net.momirealms.craftengine.core.pack.bedrock.lang.BedrockLanguageWriter;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTextureRegistry;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.bedrock.texture.vanilla.BedrockVanillaTextures;
import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ItemModels;
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
            // 基岩版自定义物品
            BedrockItemSources sources = new BedrockItemSources(items);

            CollectedModels models = collectModels(pack, sources);

            BedrockItemIcons icons = new BedrockItemIcons(Config.bedrockItemIconSize(), textureRegistry);
            BedrockBlockItems blockItems = new BedrockBlockItems(textureRegistry);
            BedrockNativeItemWriter nativeItems = new BedrockNativeItemWriter(resolver, textures, icons, blockItems, root);
            List<BedrockPackMappings.Item> mappings = new ArrayList<>();

            int convertedModels = 0;
            for (Map.Entry<BaseItemModel, List<BedrockItemSource>> entry : models.staticModels().entrySet()) {
                BaseItemModel variant = entry.getKey();
                List<BedrockItemSource> users = entry.getValue();
                Key resource = users.getFirst().id();
                BedrockNativeItemWriter.Appearance appearance;
                try {
                    appearance = nativeItems.write(variant, resource);
                } catch (IOException | IllegalArgumentException exception) {
                    for (BedrockItemSource item : users) {
                        CraftEngine.instance().logger().warn("Skipping Bedrock item " + item.id() + " (" + item.itemModel() + "): " + exception.getMessage());
                    }
                    continue;
                }
                for (BedrockItemSource item : users) {
                    mappings.add(new BedrockPackMappings.Item(item.id().asString(), item.javaItem().asString(), item.itemModel().asString(),
                            item.bedrockIdentifier(), appearance.icon(), blockItems.applyOptions(appearance.displayBlock(), item.bedrockOptions()), BedrockItemRendering.displayHandheld(item.itemModel(), sources)));
                }
                convertedModels++;
            }
            for (Map.Entry<JavaFishingRodModel, List<BedrockItemSource>> entry : models.fishingRods().entrySet()) {
                JavaFishingRodModel rod = entry.getKey();
                List<BedrockItemSource> users = entry.getValue();
                Key resource = users.getFirst().id();
                try {
                    // A native PNG tool loses handheld_rod's opposite Y rotation; both states need their display transforms.
                    BedrockNativeItemWriter.Appearance idle = nativeItems.writeBlock(rod.idle(), resource);
                    boolean distinct = !rod.idle().equals(rod.cast());
                    BedrockNativeItemWriter.Appearance cast = distinct ? nativeItems.writeBlock(rod.cast(), fishingRodCastId(resource)) : null;
                    for (BedrockItemSource item : users) {
                        BedrockPackMappings.ItemVariant variant = cast == null ? null : new BedrockPackMappings.ItemVariant(
                                BedrockItemSource.bedrockIdentifier(fishingRodCastId(item.id())), cast.icon(), blockItems.applyOptions(cast.displayBlock(), item.bedrockOptions()));
                        mappings.add(new BedrockPackMappings.Item(item.id().asString(), item.javaItem().asString(), item.itemModel().asString(),
                                item.bedrockIdentifier(), idle.icon(), blockItems.applyOptions(idle.displayBlock(), item.bedrockOptions()), BedrockItemRendering.displayHandheld(item.itemModel(), sources), variant));
                    }
                    convertedModels += distinct ? 2 : 1;
                } catch (IOException | IllegalArgumentException exception) {
                    for (BedrockItemSource item : users) {
                        CraftEngine.instance().logger().warn("Skipping Bedrock fishing rod " + item.id() + " (" + item.itemModel() + "): " + exception.getMessage());
                    }
                }
            }
            BedrockBowWriter bows = new BedrockBowWriter(resolver, textures, icons, blockItems, root);
            for (Map.Entry<JavaBowModel, List<BedrockItemSource>> entry : models.bows().entrySet()) {
                try {
                    BedrockBowWriter.Result bow = bows.write(entry.getKey(), entry.getValue());
                    for (BedrockItemSource item : entry.getValue()) {
                        mappings.add(new BedrockPackMappings.Item(item.id().asString(), item.javaItem().asString(), item.itemModel().asString(),
                                item.bedrockIdentifier(), bow.icon(), blockItems.applyOptions(bow.displayBlock(), item.bedrockOptions()), BedrockItemRendering.displayHandheld(item.itemModel(), sources)));
                    }
                    convertedModels += bow.models();
                } catch (IOException | IllegalArgumentException exception) {
                    for (BedrockItemSource item : entry.getValue()) {
                        CraftEngine.instance().logger().warn("Skipping Bedrock bow " + item.id() + " (" + item.itemModel() + "): " + exception.getMessage());
                    }
                }
            }
            BedrockCrossbowWriter crossbows = new BedrockCrossbowWriter(resolver, textures, icons, blockItems, root);
            for (Map.Entry<JavaCrossbowModel, List<BedrockItemSource>> entry : models.crossbows().entrySet()) {
                try {
                    BedrockCrossbowWriter.Result result = crossbows.write(entry.getKey(), entry.getValue());
                    for (BedrockItemSource item : entry.getValue()) {
                        BedrockPackMappings.CrossbowCharge charge = new BedrockPackMappings.CrossbowCharge(
                                new BedrockPackMappings.ItemVariant(BedrockItemSource.bedrockIdentifier(BedrockCrossbowWriter.chargedId(item.id(), false)), result.arrow().icon(), blockItems.applyOptions(result.arrow().displayBlock(), item.bedrockOptions())),
                                new BedrockPackMappings.ItemVariant(BedrockItemSource.bedrockIdentifier(BedrockCrossbowWriter.chargedId(item.id(), true)), result.rocket().icon(), blockItems.applyOptions(result.rocket().displayBlock(), item.bedrockOptions())));
                        mappings.add(new BedrockPackMappings.Item(item.id().asString(), item.javaItem().asString(), item.itemModel().asString(),
                                item.bedrockIdentifier(), result.icon(), blockItems.applyOptions(result.displayBlock(), item.bedrockOptions()), false, null, charge));
                    }
                    convertedModels += result.models();
                } catch (IOException | IllegalArgumentException exception) {
                    for (BedrockItemSource item : entry.getValue()) {
                        CraftEngine.instance().logger().warn("Skipping Bedrock crossbow " + item.id() + " (" + item.itemModel() + "): " + exception.getMessage());
                    }
                }
            }
            BedrockSpearWriter spears = new BedrockSpearWriter(resolver, textures, icons, blockItems, root);
            for (Map.Entry<JavaDisplayContextModel, List<BedrockItemSource>> entry : models.spears().entrySet()) {
                try {
                    BedrockSpearWriter.Result result = spears.write(entry.getKey(), entry.getValue());
                    for (BedrockItemSource item : entry.getValue()) {
                        mappings.add(new BedrockPackMappings.Item(item.id().asString(), item.javaItem().asString(), item.itemModel().asString(),
                                item.bedrockIdentifier(), result.icon(), blockItems.applyOptions(result.displayBlock(), item.bedrockOptions()), false));
                    }
                    convertedModels += result.models();
                } catch (IOException | IllegalArgumentException exception) {
                    for (BedrockItemSource item : entry.getValue()) {
                        CraftEngine.instance().logger().warn("Skipping Bedrock spear " + item.id() + " (" + item.itemModel() + "): " + exception.getMessage());
                    }
                }
            }
            textureRegistry.write(root);
            blockItems.writeBlocks(root);
            BedrockPlayerAnimations.write(root, mappings);
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
            return new Result(mappings.size(), convertedModels, items.size() - mappings.size());
        }
    }

    private CollectedModels collectModels(JavaResourcePack pack, BedrockItemSources sources) {
        Map<BaseItemModel, List<BedrockItemSource>> models = new LinkedHashMap<>();
        Map<JavaBowModel, List<BedrockItemSource>> bows = new LinkedHashMap<>();
        Map<JavaFishingRodModel, List<BedrockItemSource>> fishingRods = new LinkedHashMap<>();
        Map<JavaCrossbowModel, List<BedrockItemSource>> crossbows = new LinkedHashMap<>();
        Map<JavaDisplayContextModel, List<BedrockItemSource>> spears = new LinkedHashMap<>();
        for (BedrockItemSource item : sources.items()) {
            try {
                JsonObject definition = pack.readJson("assets/" + item.itemModel().namespace() + "/items/" + item.itemModel().value() + ".json");
                // 可能是第三方包，如果没有 item model 则跳过
                if (definition == null || !(definition.get("model") instanceof JsonObject model)) {
                    continue;
                }
                BedrockItemSource definitionSource = sources.findItemByModel(item.itemModel());
                ItemModel itemModel = ItemModels.fromJson(model);
                if (definitionSource != null && definitionSource.javaItem().equals(ItemKeys.BOW)) {
                    bows.computeIfAbsent(JavaBowModel.read(itemModel), key -> new ArrayList<>()).add(item);
                } else if (definitionSource != null && definitionSource.javaItem().equals(ItemKeys.FISHING_ROD)) {
                    fishingRods.computeIfAbsent(JavaFishingRodModel.read(itemModel), key -> new ArrayList<>()).add(item);
                } else if (definitionSource != null && definitionSource.javaItem().equals(ItemKeys.CROSSBOW)) {
                    crossbows.computeIfAbsent(JavaCrossbowModel.read(itemModel), key -> new ArrayList<>()).add(item);
                } else if (definitionSource != null && BedrockItemRendering.isSpear(definitionSource.javaItem())) {
                    spears.computeIfAbsent(JavaDisplayContextModel.read(itemModel), key -> new ArrayList<>()).add(item);
                } else {
                    models.computeIfAbsent(BedrockItemRendering.baseModel(itemModel), key -> new ArrayList<>()).add(item);
                }
            } catch (IOException | RuntimeException exception) {
                CraftEngine.instance().logger().warn("Skipping Bedrock item " + item.id() + " (" + item.itemModel() + "): " + exception.getMessage());
            }
        }
        return new CollectedModels(models, bows, fishingRods, crossbows, spears);
    }

    private static Key fishingRodCastId(Key item) {
        return Key.of("ce", "rod_cast_" + HashUtils.sha1(item.asString().getBytes(StandardCharsets.UTF_8)));
    }

    private record CollectedModels(Map<BaseItemModel, List<BedrockItemSource>> staticModels,
                                   Map<JavaBowModel, List<BedrockItemSource>> bows,
                                   Map<JavaFishingRodModel, List<BedrockItemSource>> fishingRods,
                                   Map<JavaCrossbowModel, List<BedrockItemSource>> crossbows,
                                   Map<JavaDisplayContextModel, List<BedrockItemSource>> spears) {
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
