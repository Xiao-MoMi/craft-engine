package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.proxy.minecraft.network.syncher.EntityDataSerializersProxy;

import java.util.List;
import java.util.Optional;

public class LivingEntityData<T> extends BaseEntityData<T> {
    public static final LivingEntityData<Byte> LivingEntityFlags = new LivingEntityData<>(LivingEntityData.class, EntityDataSerializersProxy.BYTE, (byte) 0);
    public static final LivingEntityData<Float> Health = new LivingEntityData<>(LivingEntityData.class, EntityDataSerializersProxy.FLOAT, 1.0f);
    public static final LivingEntityData<List<Object>> EffectParticles = new LivingEntityData<>(LivingEntityData.class, EntityDataSerializersProxy.PARTICLES, List.of());
    public static final LivingEntityData<Boolean> EffectAmbience = new LivingEntityData<>(LivingEntityData.class, EntityDataSerializersProxy.BOOLEAN, false);
    public static final LivingEntityData<Integer> ArrowCount = new LivingEntityData<>(LivingEntityData.class, EntityDataSerializersProxy.INT, 0);
    public static final LivingEntityData<Integer> StingerCount = new LivingEntityData<>(LivingEntityData.class, EntityDataSerializersProxy.INT, 0);
    public static final LivingEntityData<Optional<Object>> SleepingPos = new LivingEntityData<>(LivingEntityData.class, EntityDataSerializersProxy.OPTIONAL_BLOCK_POS, Optional.empty());

    protected LivingEntityData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}