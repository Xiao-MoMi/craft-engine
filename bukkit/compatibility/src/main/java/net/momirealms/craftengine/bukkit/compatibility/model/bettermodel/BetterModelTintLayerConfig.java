package net.momirealms.craftengine.bukkit.compatibility.model.bettermodel;

import kr.toxicity.model.api.util.function.BonePredicate;
import net.momirealms.craftengine.core.entity.furniture.data.FurnitureDataSourceConfig;
import net.momirealms.craftengine.core.util.Color;
import org.jetbrains.annotations.NotNull;

public record BetterModelTintLayerConfig(
        @NotNull FurnitureDataSourceConfig<Color> source,
        @NotNull BonePredicate bones
) {
}
