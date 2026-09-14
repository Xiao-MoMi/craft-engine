package net.momirealms.craftengine.core.item.setting.value;

import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;

public record EquipmentFallback(Key assetId, double distance) {

    public static EquipmentFallback fromConfig(ConfigSection section) {
        Key assetId = section.getNonNullAssetPath(ConfigKeys.of("asset_id"));
        double distance = section.getValue("distance", it -> it.getAsDouble(0), 32d);
        return new EquipmentFallback(assetId, distance);
    }
}
