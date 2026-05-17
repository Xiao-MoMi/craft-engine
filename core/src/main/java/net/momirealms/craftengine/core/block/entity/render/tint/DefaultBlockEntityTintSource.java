package net.momirealms.craftengine.core.block.entity.render.tint;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.TintSource;
import net.momirealms.sparrow.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DefaultBlockEntityTintSource implements BlockEntityTintSource {
    private static final Map<Key, Object[]> TO_LEGACY = Map.of(
            DataComponentKeys.DYED_COLOR, new Object[] {"display", "color"},
            DataComponentKeys.FIREWORK_EXPLOSION, new Object[] {"Explosion"},
            DataComponentKeys.POTION_CONTENTS, new Object[] {"CustomPotionColor"},
            DataComponentKeys.MAP_COLOR, new Object[] {"display", "MapColor"}
    );

    private final TintSource tintSource;
    private final List<Key> components;
    private final List<Object[]> legacyNBTPaths;

    public DefaultBlockEntityTintSource(TintSource tintSource, List<Key> components) {
        this.components = components;
        this.tintSource = tintSource;
        if (VersionHelper.COMPONENT_RELEASE) {
            this.legacyNBTPaths = null;
        } else {
            this.legacyNBTPaths = new ArrayList<>(components.size());
            for (final Key key : components) {
                Optional.ofNullable(TO_LEGACY.get(key)).ifPresent(this.legacyNBTPaths::add);
            }
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void applyTint(Item item) {
        Item sourceItem = this.tintSource.tintSource();
        if (sourceItem != null) {
            if (VersionHelper.COMPONENT_RELEASE) {
                for (Key component : this.components) {
                    Tag componentData = sourceItem.getComponentAsSparrowTag(component);
                    if (componentData != null) {
                        item.setSparrowTagComponent(component, componentData);
                    }
                }
            } else {
                for (Object[] legacyNBTPath : this.legacyNBTPaths) {
                    Tag tag = sourceItem.getSparrowTag(legacyNBTPath);
                    if (tag != null) {
                        item.setTag(tag, legacyNBTPath);
                    }
                }
            }
        }
    }
}
