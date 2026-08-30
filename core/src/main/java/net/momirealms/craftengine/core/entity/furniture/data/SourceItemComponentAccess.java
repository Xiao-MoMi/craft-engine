package net.momirealms.craftengine.core.entity.furniture.data;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

final class SourceItemComponentAccess {
    private static final Map<Key, Object[]> LEGACY_PATHS = Map.of(
            DataComponentKeys.DYED_COLOR, new Object[] {"display", "color"},
            DataComponentKeys.FIREWORK_EXPLOSION, new Object[] {"Explosion"},
            DataComponentKeys.POTION_CONTENTS, new Object[] {"CustomPotionColor"},
            DataComponentKeys.MAP_COLOR, new Object[] {"display", "MapColor"}
    );

    private SourceItemComponentAccess() {
    }

    static @Nullable Tag read(@NotNull Item item, @NotNull Key component) {
        if (VersionHelper.COMPONENT_RELEASE) {
            return item.getComponentAsSparrowTag(component);
        }
        Object[] path = LEGACY_PATHS.get(component);
        return path == null ? null : item.getSparrowTag(path);
    }

    static void copy(@NotNull Item source, @NotNull Item target, @NotNull Key component) {
        if (VersionHelper.COMPONENT_RELEASE) {
            Tag value = source.getComponentAsSparrowTag(component);
            if (value != null) {
                target.setSparrowTagComponent(component, value);
            }
            return;
        }
        Object[] path = LEGACY_PATHS.get(component);
        if (path == null) {
            return;
        }
        Tag value = source.getSparrowTag(path);
        if (value != null) {
            target.setTag(value, path);
        }
    }
}
