package net.momirealms.craftengine.core.pack.bedrock;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import net.momirealms.craftengine.core.pack.bedrock.sound.BedrockSounds;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackFiles;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.core.pack.bedrock.block.BedrockBlockDefinitions;
import net.momirealms.craftengine.core.pack.bedrock.block.BedrockWorldBlocks;
import net.momirealms.craftengine.core.pack.bedrock.block.BlockRegistrationPlanWriter;
import net.momirealms.craftengine.core.pack.bedrock.block.WorldBlockSource;
import net.momirealms.craftengine.core.pack.bedrock.font.BedrockFontGenerator;
import net.momirealms.craftengine.core.pack.bedrock.gui.BedrockGuiGenerator;
import net.momirealms.craftengine.core.pack.bedrock.gui.BedrockBossBars;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemResourceGenerator;
import net.momirealms.craftengine.core.pack.bedrock.item.display.BedrockItemDisplayGenerator;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRegistrationPlan;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRegistrationPlanWriter;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.model.NativeModelAssets;
import net.momirealms.craftengine.core.pack.bedrock.model.NativeModelSource;
import net.momirealms.craftengine.core.pack.bedrock.scoreboard.BedrockSidebarGenerator;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTerrainAtlas;
import net.momirealms.craftengine.core.pack.bedrock.texture.vanilla.BedrockVanillaTextures;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ZipUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BedrockPackGenerator {

    private BedrockPackGenerator() {
    }

    public static void generate(JavaResourcePack resources,
                                ItemRegistrationPlan plan,
                                List<WorldBlockSource> blocks,
                                List<NativeModelSource> nativeModels,
                                Map<Key, Key> soundReplacements,
                                Path output) throws IOException {
        Path dataDestination = output.toAbsolutePath().normalize();

        try (FileSystem fileSystem = Jimfs.newFileSystem(Configuration.forCurrentPlatform())) {
            Path pack = fileSystem.getPath("bedrock").toAbsolutePath();
            Path data = fileSystem.getPath("extension-data").toAbsolutePath();
            writeItemsAndBlocks(resources, plan, blocks, pack, data, soundReplacements);
            GsonHelper.writeJsonFile(resources.translations(), data.resolve(BedrockPackLayout.TRANSLATIONS));
            BedrockFontGenerator.write(resources, pack, data.resolve(BedrockPackLayout.GLYPHS));
            BedrockGuiGenerator.write(resources, pack);
            BedrockSidebarGenerator.write(resources, pack, data.resolve(BedrockPackLayout.SCOREBOARD), Config.bedrockScoreboard());
            BedrockBossBars.write(resources, pack, data.resolve(BedrockPackLayout.BOSS_BARS));
            NativeModelAssets.write(nativeModels, pack, data.resolve(BedrockPackLayout.NATIVE_MODELS), Config.bedrockDisplayDyeMaxLayers());

            BedrockResourceSharing.share(pack, data.resolve(RegistrationPlanKind.BLOCK.file()));
            BedrockDefinitionBundler.bundle(pack, Config.bedrockMaxDefinitionsPerFile());
            try (InputStream icon = resources.openCustomResource("pack.png")) {
                if (icon != null) {
                    Files.copy(icon, pack.resolve("pack_icon.png"));
                }
            }
            writeArchives(pack, data, dataDestination);
        }
    }

    private static void writeItemsAndBlocks(JavaResourcePack resources,
                                            ItemRegistrationPlan plan,
                                            List<WorldBlockSource> blocks,
                                            Path pack,
                                            Path data,
                                            Map<Key, Key> soundReplacements) throws IOException {
        Path itemPlanPath = data.resolve(RegistrationPlanKind.ITEM.file());
        Files.createDirectories(itemPlanPath.getParent());
        JavaModelResources models = new JavaModelResources(resources);
        BedrockTerrainAtlas atlas = new BedrockTerrainAtlas();
        BedrockBlockDefinitions definitions = new BedrockBlockDefinitions();

        BedrockItemResourceGenerator.Result items = BedrockItemResourceGenerator.write(models, plan, pack, atlas, definitions);
        JsonObject displays = BedrockItemDisplayGenerator.write(models, plan, items, pack);
        ItemRegistrationPlanWriter.write(plan, itemPlanPath, items, displays);

        BedrockSounds sounds = new BedrockSounds(resources, soundReplacements);
        JsonArray worldBlocks = BedrockWorldBlocks.write(models, blocks, pack, atlas, definitions, sounds);
        JsonObject playback = new JsonObject();
        sounds.write(pack, playback);
        if (!playback.isEmpty()) {
            GsonHelper.writeJsonFile(playback, data.resolve(BedrockPackLayout.SOUND_MAPPINGS));
        }
        BlockRegistrationPlanWriter.write(items, worldBlocks, blocks, data.resolve(RegistrationPlanKind.BLOCK.file()));

        BedrockVanillaTextures.write(resources, pack, atlas);
        atlas.finish(pack);
        definitions.write(pack);
    }

    private static void writeArchives(Path pack, Path data, Path dataDestination) throws IOException {
        JsonObject manifest = manifest();
        GsonHelper.writeJsonFile(manifest, pack.resolve("manifest.json"));
        GsonHelper.writeJsonFile(manifest, data.resolve("manifest.json"));

        Path dataDirectory = Files.createDirectories(dataDestination.getParent());
        Path temporaryPack = Files.createTempFile(dataDirectory, "bedrock-pack-", ".tmp");
        try {
            ZipUtils.compress(pack, temporaryPack, false, false);
            Path temporaryData = Files.createTempFile(dataDirectory, "bedrock-data-", ".tmp");
            try {
                ZipUtils.compressWithStoredFile(data, temporaryData, temporaryPack, BedrockPackFiles.CLIENT_PACK);
                Files.move(temporaryData, dataDestination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporaryData);
            }
        } finally {
            Files.deleteIfExists(temporaryPack);
        }
    }

    private static JsonObject manifest() {
        JsonArray version = new JsonArray();
        version.add(1);
        version.add(0);
        version.add(0);

        JsonArray engineVersion = new JsonArray();
        engineVersion.add(1);
        engineVersion.add(21);
        engineVersion.add(0);

        JsonObject header = new JsonObject();
        header.addProperty("name", Config.bedrockPackName());
        header.addProperty("description", Config.bedrockPackDescription());
        header.addProperty("uuid", UUID.randomUUID().toString());
        header.add("version", version);
        header.add("min_engine_version", engineVersion);

        JsonObject module = new JsonObject();
        module.addProperty("type", "resources");
        module.addProperty("uuid", UUID.randomUUID().toString());
        module.add("version", version);

        JsonArray modules = new JsonArray();
        modules.add(module);

        JsonObject manifest = new JsonObject();
        manifest.addProperty("format_version", 2);
        manifest.add("header", header);
        manifest.add("modules", modules);
        return manifest;
    }
}
