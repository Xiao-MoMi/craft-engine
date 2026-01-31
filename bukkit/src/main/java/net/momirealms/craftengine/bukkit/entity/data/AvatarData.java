package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.core.util.VersionHelper;

// todo: 未测试
public class AvatarData<T> extends LivingEntityData<T> {
    // 1.21.9~1.21.10
    public static final AvatarData<Byte> PLAYER_MAIN_HAND_V1_21_9 = of(AvatarData.class, EntityDataValue.Serializers$BYTE, (byte) 1, VersionHelper.isOrAbove1_21_9() && !VersionHelper.isOrAbove1_21_11());
    // 1.21.11
    public static final AvatarData<Object> PLAYER_MAIN_HAND_V1_21_11 = of(AvatarData.class, EntityDataValue.Serializers$HUMANOID_ARM, CoreReflections.instance$HumanoidArm$RIGHT, VersionHelper.isOrAbove1_21_11());

    // 1.21.9+
    public static final AvatarData<Byte> PLAYER_MODE_CUSTOMISATION_V1_21_9 = of(AvatarData.class, EntityDataValue.Serializers$BYTE, (byte) 0, VersionHelper.isOrAbove1_21_9());


    public static <T> AvatarData<T> of(final Class<?> clazz, final Object serializer, T defaultValue, boolean condition) {
        if (!condition) return null;
        return new AvatarData<>(clazz, serializer, defaultValue);
    }

    public AvatarData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
