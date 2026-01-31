package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.OptionalInt;

// todo: 未测试
public class PlayerData<T> extends AvatarData<T> {
    public static final PlayerData<Float> PLAYER_ABSORPTION = new PlayerData<>(PlayerData.class, EntityDataValue.Serializers$FLOAT, 0.0f);
    public static final PlayerData<Integer> SCORE = new PlayerData<>(PlayerData.class, EntityDataValue.Serializers$INT, 0);

    // 1.20~1.21.8
    public static final PlayerData<Byte> PLAYER_MODE_CUSTOMISATION = of(PlayerData.class, EntityDataValue.Serializers$BYTE, (byte) 0, !VersionHelper.isOrAbove1_21_9());
    public static final PlayerData<Byte> PLAYER_MAIN_HAND = of(PlayerData.class, EntityDataValue.Serializers$BYTE, (byte) 1, !VersionHelper.isOrAbove1_21_9());
    public static final PlayerData<Object> SHOULDER_LEFT = of(PlayerData.class, EntityDataValue.Serializers$COMPOUND_TAG, FastNMS.INSTANCE.constructor$CompoundTag(), !VersionHelper.isOrAbove1_21_9());
    public static final PlayerData<Object> SHOULDER_RIGHT = of(PlayerData.class, EntityDataValue.Serializers$COMPOUND_TAG, FastNMS.INSTANCE.constructor$CompoundTag(), !VersionHelper.isOrAbove1_21_9());

    // 1.21.9+
    public static final PlayerData<OptionalInt> SHOULDER_PARROT_LEFT = of(PlayerData.class, EntityDataValue.Serializers$OPTIONAL_UNSIGNED_INT, OptionalInt.empty(), VersionHelper.isOrAbove1_21_9());
    public static final PlayerData<OptionalInt> SHOULDER_PARROT_RIGHT = of(PlayerData.class, EntityDataValue.Serializers$OPTIONAL_UNSIGNED_INT, OptionalInt.empty(), VersionHelper.isOrAbove1_21_9());

    public static <T> PlayerData<T> of(final Class<?> clazz, final Object serializer, T defaultValue, boolean condition) {
        if (!condition) return null;
        return new PlayerData<>(clazz, serializer, defaultValue);
    }

    public PlayerData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
