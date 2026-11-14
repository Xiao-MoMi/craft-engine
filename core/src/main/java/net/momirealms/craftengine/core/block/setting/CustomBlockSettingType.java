package net.momirealms.craftengine.core.block.setting;

import net.momirealms.craftengine.core.util.CustomSettingTypeRegistry;

public final class CustomBlockSettingType<T> {
    private static final CustomSettingTypeRegistry REGISTRY = new CustomSettingTypeRegistry("block");
    private final int id;

    public CustomBlockSettingType() {
        this.id = REGISTRY.register();
    }

    public int id() {
        return this.id;
    }

    public static <T> CustomBlockSettingType<T> simple() {
        return new CustomBlockSettingType<>();
    }

    public static void freeze() {
        REGISTRY.freeze();
    }

    static Object[] newDataArray() {
        return REGISTRY.newDataArray();
    }
}
