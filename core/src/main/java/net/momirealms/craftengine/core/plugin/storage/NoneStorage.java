package net.momirealms.craftengine.core.plugin.storage;

import java.util.Map;
import java.util.UUID;

public final class NoneStorage implements Storage {
    public static final NoneStorage INSTANCE = new NoneStorage();
    public static final StorageFactory<NoneStorage> FACTORY = section -> INSTANCE;

    private NoneStorage() {
    }

    @Override
    public Map<String, Boolean> loadPackPreferences(UUID player) {
        return Map.of();
    }

    @Override
    public void setPackPreference(UUID player, String pack, Boolean enabled) {
    }

    @Override
    public void setPackPreferences(UUID player, Map<String, Boolean> updates) {
    }

    @Override
    public void close() {
    }
}
