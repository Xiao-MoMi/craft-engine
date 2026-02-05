package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.util.Color;
import net.momirealms.sparrow.nbt.Tag;

public record FurnitureColorSource(Color dyedColor, int[] fireworkColors, Tag potionContents) {
}
