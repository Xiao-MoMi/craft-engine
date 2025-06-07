package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;

public class PlayerData<T> extends LivingEntityData<T> {
	public static final PlayerData<Object> Pose = new PlayerData<>(6, EntityDataValue.Serializers$POSE, CoreReflections.instance$Pose$STANDING);
	public static final PlayerData<Byte> Skin = new PlayerData<>(17, EntityDataValue.Serializers$BYTE, (byte) 0);
	public static final PlayerData<Byte> Hand = new PlayerData<>(18, EntityDataValue.Serializers$BYTE, (byte) 0);
	public static final PlayerData<Object> LShoulder = new PlayerData<>(19, EntityDataValue.Serializers$COMPOUND_TAG, CoreReflections.instance$CompoundTag$Empty);
	public static final PlayerData<Object> RShoulder = new PlayerData<>(20, EntityDataValue.Serializers$COMPOUND_TAG, CoreReflections.instance$CompoundTag$Empty);

	public PlayerData(int id, Object serializer, T defaultValue) {
		super(id, serializer, defaultValue);
	}
}
