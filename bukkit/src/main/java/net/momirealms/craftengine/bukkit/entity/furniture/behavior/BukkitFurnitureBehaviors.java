package net.momirealms.craftengine.bukkit.entity.furniture.behavior;

import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureBehaviorType;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureBehaviors;
import net.momirealms.craftengine.core.util.Key;

public final class BukkitFurnitureBehaviors extends FurnitureBehaviors {
    private BukkitFurnitureBehaviors() {}

    public static final FurnitureBehaviorType<SimpleStorageFurnitureBehaviorTemplate> SIMPLE_STORAGE_FURNITURE = register(Key.ce("simple_storage_furniture"), SimpleStorageFurnitureBehaviorTemplate.FACTORY);
    public static final FurnitureBehaviorType<DisplayItemFurnitureBehaviorTemplate> DISPLAY_ITEM_FURNITURE = register(Key.ce("display_item_furniture"), DisplayItemFurnitureBehaviorTemplate.FACTORY);
    public static final FurnitureBehaviorType<GlowingFurnitureBehaviorTemplate> GLOWING_FURNITURE = register(Key.ce("glowing_furniture"), GlowingFurnitureBehaviorTemplate.FACTORY);

    public static void init() {
    }
}
