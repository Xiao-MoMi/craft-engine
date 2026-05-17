package net.momirealms.craftengine.bukkit.entity.data.item;

import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.EntityDataSerializersProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;

public class PrimedTntData<T> extends BaseEntityData<T> {
    public static final PrimedTntData<Integer> Fuse = of(PrimedTntData.class, EntityDataSerializersProxy.INT, 80, true);
    // 1.20.3+
    public static final PrimedTntData<Object> BlockState = of(PrimedTntData.class, EntityDataSerializersProxy.BLOCK_STATE, BlocksProxy.TNT$defaultState, VersionHelper.isOrAbove1_20_3);

    private static <T> PrimedTntData<T> of(final Class<?> clazz, final Object serializer, T defaultValue, boolean condition) {
        if (!condition) return null;
        return new PrimedTntData<>(clazz, serializer, defaultValue);
    }

    protected PrimedTntData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
