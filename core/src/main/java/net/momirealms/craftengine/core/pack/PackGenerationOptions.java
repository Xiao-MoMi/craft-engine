package net.momirealms.craftengine.core.pack;

import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public record PackGenerationOptions(String description, boolean mapCompatibility) {
    private static final String[] MAP_COMPATIBILITY = ConfigKeys.of("map_compatibility");

    public static PackGenerationOptions defaults() {
        return new PackGenerationOptions(Config.packDescription(), false);
    }

    public PackGenerationOptions override(ConfigSection section) {
        if (section == null) return this;
        return new PackGenerationOptions(section.getString("description", this.description), section.getBoolean(MAP_COMPATIBILITY, this.mapCompatibility));
    }
}
