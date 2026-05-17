package net.momirealms.craftengine.core.item.customdata;

import net.momirealms.craftengine.core.entity.furniture.FurnitureDebugStickState;

public final class FurnitureDebugStickData {
    public FurnitureDebugStickState state;

    public FurnitureDebugStickData(FurnitureDebugStickState state) {
        this.state = state;
    }

    public FurnitureDebugStickData() {
        this.state = FurnitureDebugStickState.VARIANT;
    }
}
