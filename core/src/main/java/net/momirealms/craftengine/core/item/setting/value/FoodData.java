package net.momirealms.craftengine.core.item.setting.value;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public record FoodData(int nutrition, float saturation) {

    public static FoodData fromConfig(ConfigSection section) {
        return new FoodData(
                section.getInt("nutrition"),
                section.getFloat("saturation")
        );
    }
}
