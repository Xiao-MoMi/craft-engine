package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.Optional;

public class AbstractMinecartData<T> extends VehicleEntityData<T> {
    // 1.20~1.21.2
    public static final AbstractMinecartData<Integer> DisplayBlock = of(AbstractMinecartData.class, EntityDataValue.Serializers$INT, BlockStateUtils.blockStateToId(MBlocks.AIR$defaultState), !VersionHelper.isOrAbove1_21_3());
    // 1.21.3+
    public static final AbstractMinecartData<Optional<Object>> CustomDisplayBlock = of(AbstractMinecartData.class, EntityDataValue.Serializers$OPTIONAL_BLOCK_STATE, Optional.empty(), VersionHelper.isOrAbove1_21_3());
    public static final AbstractMinecartData<Integer> DisplayOffset = of(AbstractMinecartData.class, EntityDataValue.Serializers$INT, 6, true);
    // 1.20~1.21.2
    public static final AbstractMinecartData<Boolean> CustomDisplay = of(AbstractMinecartData.class, EntityDataValue.Serializers$BOOLEAN, false, !VersionHelper.isOrAbove1_21_3());

    public static <T> AbstractMinecartData<T> of(final Class<?> clazz, final Object serializer, T defaultValue, boolean condition) {
        if (!condition) return null;
        return new AbstractMinecartData<>(clazz, serializer, defaultValue);
    }

    public AbstractMinecartData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
