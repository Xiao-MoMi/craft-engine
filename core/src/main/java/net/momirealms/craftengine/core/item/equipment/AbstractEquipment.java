package net.momirealms.craftengine.core.item.equipment;

import net.momirealms.craftengine.core.util.Key;

public abstract class AbstractEquipment implements Equipment {
    protected final Key assetId;

    protected AbstractEquipment(Key assetId) {
        this.assetId = assetId;
    }

    @Override
    public Key assetId() {
        return assetId;
    }
}
