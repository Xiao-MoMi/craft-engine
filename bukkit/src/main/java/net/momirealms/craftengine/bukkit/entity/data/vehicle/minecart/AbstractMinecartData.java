package net.momirealms.craftengine.bukkit.entity.data.vehicle.minecart;

import net.momirealms.craftengine.bukkit.entity.data.vehicle.VehicleEntityData;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.EntityDataSerializersProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;

import java.util.Optional;

public class AbstractMinecartData<T> extends VehicleEntityData<T> {
    // 1.20~1.21.4
    public static final AbstractMinecartData<Integer> DisplayBlockState = of(AbstractMinecartData.class, EntityDataSerializersProxy.INT, BlockStateUtils.blockStateToId(BlocksProxy.AIR$defaultState), !VersionHelper.isOrAbove1_21_5);
    // 1.21.5+
    public static final AbstractMinecartData<Optional<Object>> CustomDisplayBlockState = of(AbstractMinecartData.class, EntityDataSerializersProxy.OPTIONAL_BLOCK_STATE, Optional.empty(), VersionHelper.isOrAbove1_21_5);
    public static final AbstractMinecartData<Integer> DisplayOffset = new AbstractMinecartData<>(AbstractMinecartData.class, EntityDataSerializersProxy.INT, 6);
    // 1.20~1.21.4
    public static final AbstractMinecartData<Boolean> hasCustomDisplay = of(AbstractMinecartData.class, EntityDataSerializersProxy.BOOLEAN, false, !VersionHelper.isOrAbove1_21_5);

    private static <T> AbstractMinecartData<T> of(final Class<?> clazz, final Object serializer, T defaultValue, boolean condition) {
        if (!condition) return null;
        return new AbstractMinecartData<>(clazz, serializer, defaultValue);
    }

    protected AbstractMinecartData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
