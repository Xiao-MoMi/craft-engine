package net.momirealms.craftengine.core.entity.furniture.element.tint;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DefaultFurnitureTintSource implements FurnitureTintSource {
    private static final Map<Key, Object[]> TO_LEGACY = Map.of(
            DataComponentKeys.DYED_COLOR, new Object[] {"display", "color"},
            DataComponentKeys.FIREWORK_EXPLOSION, new Object[] {"Explosion"},
            DataComponentKeys.POTION_CONTENTS, new Object[] {"CustomPotionColor"},
            DataComponentKeys.MAP_COLOR, new Object[] {"display", "MapColor"}
    );

    private final Furniture furniture;
    private final List<Key> components;
    private final List<Object[]> legacyNBTPaths;

    public DefaultFurnitureTintSource(Furniture furniture, List<Key> components) {
        this.furniture = furniture;
        this.components = components;
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
        Item sourceItem = this.furniture.sourceItem();
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
