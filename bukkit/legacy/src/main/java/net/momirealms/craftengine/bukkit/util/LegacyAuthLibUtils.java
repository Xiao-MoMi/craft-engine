package net.momirealms.craftengine.bukkit.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;

import java.util.UUID;

public class LegacyAuthLibUtils {

    private LegacyAuthLibUtils() {}

    public static GameProfile constructor$GameProfile(UUID id, String name, PropertyMap properties) {
        GameProfile profile = new GameProfile(id, name);
        profile.getProperties().putAll(properties);
        return profile;
    }

    public static PropertyMap constructor$PropertyMap() {
        return new PropertyMap();
    }
}
