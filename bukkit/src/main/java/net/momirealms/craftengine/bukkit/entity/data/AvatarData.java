package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.EntityDataSerializersProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.HumanoidArmProxy;

public class AvatarData<T> extends LivingEntityData<T> {
    // 1.21.9~1.21.10
    public static final AvatarData<Byte> MainArm_V1_21_9 = of(AvatarData.class, EntityDataSerializersProxy.BYTE, (byte) 1, VersionHelper.isOrAbove1_21_9 && !VersionHelper.isOrAbove1_21_11);
    // 1.21.11+
    public static final AvatarData<Object> MainArm_V1_21_11 = of(AvatarData.class, EntityDataSerializersProxy.HUMANOID_ARM, HumanoidArmProxy.RIGHT, VersionHelper.isOrAbove1_21_11);
    // 1.21.9+
    public static final AvatarData<Byte> PlayerModeCustomisation_V1_21_9 = of(AvatarData.class, EntityDataSerializersProxy.BYTE, (byte) 0, VersionHelper.isOrAbove1_21_9);

    private static <T> AvatarData<T> of(final Class<?> clazz, final Object serializer, T defaultValue, boolean condition) {
        if (!condition) return null;
        return new AvatarData<>(clazz, serializer, defaultValue);
    }

    protected AvatarData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
