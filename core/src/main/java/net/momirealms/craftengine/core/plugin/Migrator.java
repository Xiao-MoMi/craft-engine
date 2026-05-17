package net.momirealms.craftengine.core.plugin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public final class Migrator {
    private Migrator() {}

    static void migrateCache(CraftEngine plugin) throws IOException {
        Path cacheFolder = plugin.dataFolderPath().resolve("cache");
        if (Files.exists(cacheFolder)) {
            Path customBlockState = cacheFolder.resolve("custom-block-states.json");
            if (Files.exists(customBlockState)) {
                Files.move(customBlockState, cacheFolder.resolve("custom_block_states.json"), StandardCopyOption.REPLACE_EXISTING);
            }
            Path visualBlockState = cacheFolder.resolve("visual-block-states.json");
            if (Files.exists(visualBlockState)) {
                Files.move(visualBlockState, cacheFolder.resolve("visual_block_states.json"), StandardCopyOption.REPLACE_EXISTING);
            }
            Path customModelDataFolder = cacheFolder.resolve("custom-model-data");
            if (Files.exists(customModelDataFolder)) {
                Files.move(customModelDataFolder, cacheFolder.resolve("custom_model_data"), StandardCopyOption.REPLACE_EXISTING);
            }
            Path jukeboxSongs = cacheFolder.resolve("jukebox-songs.json");
            if (Files.exists(jukeboxSongs)) {
                Files.move(jukeboxSongs, cacheFolder.resolve("jukebox_songs.json"), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    static void migrateWorldData(CraftEngine plugin) throws IOException {
        if (!VersionHelper.isOrAbove26_1) {
            return;
        }
        Path tempData = plugin.dataFolderPath().resolve("worlds_to_migrate.json");
        if (!Files.exists(tempData)) {
            return;
        }
        Path pluginsFolder = plugin.dataFolderPath().toAbsolutePath().getParent();
        if (pluginsFolder == null) {
            return;
        }
        Path rootFolder = pluginsFolder.getParent();
        if (rootFolder == null) {
            return;
        }
        Path dimensionsFolder = rootFolder.resolve("world").resolve("dimensions");
        if (!Files.exists(dimensionsFolder)) {
            return;
        }
        JsonObject worldsToMigrate = GsonHelper.readJsonObjectFromFile(tempData);
        if (worldsToMigrate == null || worldsToMigrate.isEmpty()) {
            return;
        }

        for (Map.Entry<String, JsonElement> entry : worldsToMigrate.entrySet()) {
            Path backupFile = plugin.dataFolderPath().resolve("world_upgrade_backup").resolve(entry.getKey() + ".zip");
            if (!Files.exists(backupFile)) {
                continue;
            }

            Key dimension = Key.of(entry.getValue().getAsString());
            try {
                Path finalWorldFolder = dimensionsFolder.resolve(dimension.namespace).resolve(dimension.value).resolve("craftengine");
                Files.createDirectories(finalWorldFolder);

                plugin.logger().warn("Restoring world '" + dimension.asString() + "' from backup " + backupFile.toAbsolutePath());
                Timestamp timestamp = new Timestamp();
                ZipUtils.decompress(backupFile, finalWorldFolder);
                plugin.logger().warn("Successfully restored world '" + dimension.asString() + "' in " + String.format("%.2f", timestamp.deltaMillis() / 1000d) + "s");
            } catch (Exception e) {
                plugin.logger().error("Failed to restore world " + dimension.asString(), e);
            }
        }

        Files.delete(tempData);
    }
}
