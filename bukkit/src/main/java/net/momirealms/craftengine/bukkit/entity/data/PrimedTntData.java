package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.core.util.VersionHelper;

public class PrimedTntData<T> extends BaseEntityData<T> {
    public static final PrimedTntData<Integer> Fuse = of(PrimedTntData.class, EntityDataValue.Serializers$INT, 80, true);
    // 1.20.3+
    public static final PrimedTntData<Object> BlockState = of(PrimedTntData.class, EntityDataValue.Serializers$BLOCK_STATE, MBlocks.TNT$defaultState, VersionHelper.isOrAbove1_20_3());

    public static <T> PrimedTntData<T> of(final Class<?> clazz, final Object serializer, T defaultValue, boolean condition) {
        if (!condition) return null;
        return new PrimedTntData<>(clazz, serializer, defaultValue);
    }

    public PrimedTntData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
