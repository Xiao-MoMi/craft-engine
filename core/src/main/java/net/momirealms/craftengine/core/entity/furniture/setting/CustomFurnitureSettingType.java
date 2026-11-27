package net.momirealms.craftengine.core.entity.furniture.setting;

import net.momirealms.craftengine.core.util.CustomSettingTypeRegistry;

public final class CustomFurnitureSettingType<T> {
    private static final CustomSettingTypeRegistry REGISTRY = new CustomSettingTypeRegistry("furniture");
    private final int id;

    public CustomFurnitureSettingType() {
        this.id = REGISTRY.register();
    }

    public int id() {
        return this.id;
    }

    public static <T> CustomFurnitureSettingType<T> simple() {
        return new CustomFurnitureSettingType<>();
    }

    public static void freeze() {
        REGISTRY.freeze();
    }

    static Object[] newDataArray() {
        return REGISTRY.newDataArray();
    }
}
