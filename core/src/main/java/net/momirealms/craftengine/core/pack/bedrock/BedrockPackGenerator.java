package net.momirealms.craftengine.core.pack.bedrock;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackFiles;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.core.pack.bedrock.block.BedrockBlockDefinitions;
import net.momirealms.craftengine.core.pack.bedrock.block.BedrockWorldBlocks;
import net.momirealms.craftengine.core.pack.bedrock.block.BlockRegistrationPlanWriter;
import net.momirealms.craftengine.core.pack.bedrock.font.BedrockFontGenerator;
import net.momirealms.craftengine.core.pack.bedrock.gui.BedrockBossBars;
import net.momirealms.craftengine.core.pack.bedrock.gui.BedrockGuiGenerator;
import net.momirealms.craftengine.core.pack.bedrock.gui.BedrockTitle;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemResourceGenerator;
import net.momirealms.craftengine.core.pack.bedrock.item.display.BedrockItemDisplayGenerator;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRegistrationPlanWriter;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.model.NativeModelAssets;
import net.momirealms.craftengine.core.pack.bedrock.scoreboard.BedrockSidebarGenerator;
import net.momirealms.craftengine.core.pack.bedrock.sound.BedrockSounds;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTerrainAtlas;
import net.momirealms.craftengine.core.pack.bedrock.texture.vanilla.BedrockVanillaTextures;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.util.ZipUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.FORMAT_VERSION;

public final class BedrockPackGenerator {

    private BedrockPackGenerator() {
    }

    public static void generate(JavaModelResources models,
                                BedrockPackPlan plan,
                                Path output,
                                boolean obfuscation) throws IOException {
        Path dataDestination = output.toAbsolutePath().normalize();
        JavaResourcePack resources = models.pack();

        try (FileSystem fileSystem = Jimfs.newFileSystem(Configuration.forCurrentPlatform())) {
            Path pack = fileSystem.getPath("bedrock").toAbsolutePath();
            Path data = fileSystem.getPath("extension-data").toAbsolutePath();
            writeItemsAndBlocks(models, plan, pack, data);
            JsonObject translations = resources.translations();
            if (!translations.isEmpty()) {
                GsonHelper.writeJsonFile(translations, data.resolve(BedrockPackLayout.TRANSLATIONS));
            }
            BedrockFontGenerator.write(resources, pack, data.resolve(BedrockPackLayout.GLYPHS));
            BedrockGuiGenerator.write(resources, pack);
            BedrockSidebarGenerator.write(resources, pack, data.resolve(BedrockPackLayout.SCOREBOARD), Config.bedrockScoreboard());
            BedrockBossBars.write(resources, pack, data.resolve(BedrockPackLayout.BOSS_BARS));
            BedrockTitle.write(pack, Config.bedrockTitleLayer());
            NativeModelAssets.write(plan.nativeModels(), pack, data.resolve(BedrockPackLayout.NATIVE_MODELS), Config.bedrockEntities()::dye);

            boolean protection = obfuscation && VersionHelper.PREMIUM;
            Map<Path, JsonObject> geometryDocuments = new HashMap<>();
            BedrockResourceSharing.share(pack, data.resolve(RegistrationPlanKind.BLOCK.file()), geometryDocuments);
            Map<String, JsonObject> bundles = BedrockDefinitionBundler.bundle(pack, geometryDocuments, protection);
            if (protection) {
                BedrockAssetObfuscator.obfuscate(pack, data, bundles);
            }
            try (InputStream icon = resources.openCustomResource("pack.png")) {
                if (icon != null) {
                    Files.copy(icon, pack.resolve("pack_icon.png"));
                }
            }
            writeArchives(pack, data, dataDestination);
        }
    }

    private static void writeItemsAndBlocks(JavaModelResources models,
                                            BedrockPackPlan plan,
                                            Path pack,
                                            Path data) throws IOException {
        Path itemPlanPath = data.resolve(RegistrationPlanKind.ITEM.file());
        Files.createDirectories(itemPlanPath.getParent());
        JavaResourcePack resources = models.pack();
        BedrockTerrainAtlas atlas = new BedrockTerrainAtlas();
        BedrockBlockDefinitions definitions = new BedrockBlockDefinitions();

        BedrockItemResourceGenerator.Result items = BedrockItemResourceGenerator.write(models, plan.items(), pack, atlas, definitions);
        JsonObject modelDisplays = BedrockItemDisplayGenerator.write(models, items, plan.items(), pack);
        ItemRegistrationPlanWriter.write(plan.items(), itemPlanPath, items);
        if (!modelDisplays.isEmpty()) {
            GsonHelper.writeJsonFile(modelDisplays, data.resolve(BedrockPackLayout.MODEL_DISPLAYS));
        }
        Map<Key, Integer> elytraVariants = items.elytraVariants();
        if (!elytraVariants.isEmpty()) {
            JsonObject elytra = new JsonObject();
            elytraVariants.forEach((id, variant) -> elytra.addProperty(id.asString(), variant));
            GsonHelper.writeJsonFile(GsonHelper.sortObjectKeys(elytra), data.resolve(BedrockPackLayout.ELYTRA_VARIANTS));
        }

        BedrockSounds sounds = new BedrockSounds(resources, plan.soundReplacements());
        BedrockWorldBlocks.Result blocks = BedrockWorldBlocks.write(models, plan.blocks(), pack, atlas, definitions, sounds);
        JsonObject playback = new JsonObject();
        sounds.write(pack, playback);
        if (!playback.isEmpty()) {
            GsonHelper.writeJsonFile(playback, data.resolve(BedrockPackLayout.SOUND_MAPPINGS));
        }
        BlockRegistrationPlanWriter.write(items, blocks, plan.blocks().customBlocks(), data.resolve(RegistrationPlanKind.BLOCK.file()));

        BedrockVanillaTextures.write(resources, pack, atlas);
        atlas.finish(pack);
        definitions.write(pack);
    }

    private static void writeArchives(Path pack, Path data, Path dataDestination) throws IOException {
        GsonHelper.writeJsonFile(manifest(), pack.resolve("manifest.json"));

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
        manifest.addProperty(FORMAT_VERSION, BedrockFormatVersions.MANIFEST);
        manifest.add("header", header);
        manifest.add("modules", modules);
        return manifest;
    }
}
