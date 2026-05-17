package net.momirealms.craftengine.bukkit.entity.data.player;

import net.momirealms.craftengine.bukkit.entity.data.AvatarData;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.nbt.CompoundTagProxy;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.EntityDataSerializersProxy;

import java.util.OptionalInt;

public class PlayerData<T> extends AvatarData<T> {
    public static final PlayerData<Float> AbsorptionAmount = new PlayerData<>(PlayerData.class, EntityDataSerializersProxy.FLOAT, 0.0f);
    public static final PlayerData<Integer> Score = new PlayerData<>(PlayerData.class, EntityDataSerializersProxy.INT, 0);

    // 1.20~1.21.8
    public static final PlayerData<Byte> PlayerModeCustomisation = of(PlayerData.class, EntityDataSerializersProxy.BYTE, (byte) 0, !VersionHelper.isOrAbove1_21_9);
    public static final PlayerData<Byte> MainArm = of(PlayerData.class, EntityDataSerializersProxy.BYTE, (byte) 1, !VersionHelper.isOrAbove1_21_9);
    public static final PlayerData<Object> ShoulderEntityLeft = of(PlayerData.class, EntityDataSerializersProxy.COMPOUND_TAG, CompoundTagProxy.INSTANCE.newInstance(), !VersionHelper.isOrAbove1_21_9);
    public static final PlayerData<Object> ShoulderEntityRight = of(PlayerData.class, EntityDataSerializersProxy.COMPOUND_TAG, CompoundTagProxy.INSTANCE.newInstance(), !VersionHelper.isOrAbove1_21_9);

    // 1.21.9+
    public static final PlayerData<OptionalInt> ShoulderParrotLeft = of(PlayerData.class, EntityDataSerializersProxy.OPTIONAL_UNSIGNED_INT, OptionalInt.empty(), VersionHelper.isOrAbove1_21_9);
    public static final PlayerData<OptionalInt> ShoulderParrotRight = of(PlayerData.class, EntityDataSerializersProxy.OPTIONAL_UNSIGNED_INT, OptionalInt.empty(), VersionHelper.isOrAbove1_21_9);

    private static <T> PlayerData<T> of(final Class<?> clazz, final Object serializer, T defaultValue, boolean condition) {
        if (!condition) return null;
        return new PlayerData<>(clazz, serializer, defaultValue);
    }

    protected PlayerData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
