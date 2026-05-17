package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.EntityDataSerializersProxy;

public class AgeableMobData<T> extends PathfinderMobData<T> {
    public static final AgeableMobData<Boolean> IsBaby = new AgeableMobData<>(AgeableMobData.class, EntityDataSerializersProxy.BOOLEAN, false);
    public static final AgeableMobData<Boolean> AgeLocked = of(AgeableMobData.class, EntityDataSerializersProxy.BOOLEAN, false, VersionHelper.isOrAbove26_1);

    private static <T> AgeableMobData<T> of(final Class<?> clazz, final Object serializer, T defaultValue, boolean condition) {
        if (!condition) return null;
        return new AgeableMobData<>(clazz, serializer, defaultValue);
    }

    protected AgeableMobData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
