package net.momirealms.craftengine.core.pack.bedrock;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.BedrockPlayerPack;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

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
            BedrockPlayerPack playerPack = Config.bedrockPlayerPack();
            JsonObject playerPackMetadata = new JsonObject();
            playerPackMetadata.addProperty("name", playerPack.name());
            playerPackMetadata.addProperty("description", playerPack.description());
            GsonHelper.writeJsonFile(playerPackMetadata, data.resolve(BedrockPackLayout.PLAYER_PACK));
            JsonObject translations = resources.translations();
            if (!translations.isEmpty()) {
                GsonHelper.writeJsonFile(translations, data.resolve(BedrockPackLayout.TRANSLATIONS));
            }
            BedrockFontGenerator.write(resources, pack, data.resolve(BedrockPackLayout.FONTS));
            boolean pocketClassicMenus = Config.bedrockPocketClassicMenus();
            BedrockGuiGenerator.write(resources, pack, pocketClassicMenus);
            BedrockSidebarGenerator.write(resources, pack, data.resolve(BedrockPackLayout.SCOREBOARD), Config.bedrockScoreboard());
            if (Config.bedrockBossBarUseJavaLayout()) {
                BedrockBossBars.write(resources, pack, data.resolve(BedrockPackLayout.BOSS_BARS));
            }
            BedrockTitle.write(pack, Config.bedrockTitleLayer());
            JsonObject title = new JsonObject();
            JsonObject offsets = new JsonObject();
            Config.bedrockFontVerticalOffsets().forEach((font, offset) -> offsets.addProperty(font.asString(), offset));
            title.add(BedrockPackLayout.TEXT_VERTICAL_OFFSETS, offsets);
            title.addProperty(BedrockPackLayout.FORCE_JAVA_LAYOUT, Config.bedrockTitleForceJavaLayout());
            title.addProperty(BedrockPackLayout.POCKET_CLASSIC_MENUS, pocketClassicMenus);
            GsonHelper.writeJsonFile(title, data.resolve(BedrockPackLayout.TITLE));
            NativeModelAssets.write(plan.nativeModels(), pack, data.resolve(BedrockPackLayout.NATIVE_MODELS), Config.bedrockEntities()::dye);

            boolean protection = obfuscation && VersionHelper.PREMIUM;
            BedrockDefinitionIndex definitions = new BedrockDefinitionIndex();
            BedrockResourceSharing.share(pack, data.resolve(RegistrationPlanKind.BLOCK.file()), definitions);
            Map<String, JsonObject> bundles = BedrockDefinitionBundler.bundle(pack, definitions, protection);
            if (protection) {
                BedrockAssetObfuscator.obfuscate(pack, data, bundles, definitions);
            }
            try (InputStream icon = resources.openCustomResource("pack.png")) {
                if (icon != null) {
                    Files.copy(icon, pack.resolve("pack_icon.png"));
                }
            }
            BedrockPackArchive.write(pack, data, dataDestination, protection, Config.bedrockPackName(), Config.bedrockPackDescription());
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
}
