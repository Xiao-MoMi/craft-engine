package net.momirealms.craftengine.core.plugin.storage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.momirealms.craftengine.core.plugin.CraftEngine;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FileStorage implements Storage {
    public static final StorageFactory<FileStorage> FACTORY = (section) -> new FileStorage(StorageConfig.Json.fromConfig(section, CraftEngine.instance().dataFolderPath()).directory());

    private final Path directory;
    private final Path packPreferencesDirectory;

    public FileStorage(Path directory) throws IOException {
        this.directory = directory;
        this.packPreferencesDirectory = directory.resolve("pack-preferences");
        Files.createDirectories(this.packPreferencesDirectory);
    }

    public Path directory() {
        return this.directory;
    }

    @Override
    public synchronized Map<String, Boolean> loadPackPreferences(UUID player) throws IOException {
        Path file = this.packPreferencesDirectory.resolve(player + ".json");
        if (!Files.exists(file)) return Map.of();
        JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        Map<String, Boolean> states = new HashMap<>();
        json.entrySet().forEach(entry -> states.put(entry.getKey(), entry.getValue().getAsBoolean()));
        return states;
    }

    @Override
    public synchronized void setPackPreferences(UUID player, Map<String, Boolean> updates) throws IOException {
        Map<String, Boolean> states = new HashMap<>(loadPackPreferences(player));
        updates.forEach((pack, enabled) -> {
            if (enabled == null) states.remove(pack);
            else states.put(pack, enabled);
        });
        JsonObject json = new JsonObject();
        states.forEach(json::addProperty);
        Path temporary = Files.createTempFile(this.packPreferencesDirectory, player + "-", ".tmp");
        try {
            Files.writeString(temporary, json.toString());
            try {
                Files.move(temporary, this.packPreferencesDirectory.resolve(player + ".json"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, this.packPreferencesDirectory.resolve(player + ".json"), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public void close() {
    }
}
