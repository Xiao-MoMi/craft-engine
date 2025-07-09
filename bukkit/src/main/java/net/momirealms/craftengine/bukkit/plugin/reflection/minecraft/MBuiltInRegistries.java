package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.plugin.reflection.ReflectionInitException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static java.util.Objects.requireNonNull;

public final class MBuiltInRegistries {
    private MBuiltInRegistries() {}

    public static final Object BLOCK;
    public static final Object ITEM;
    public static final Object ATTRIBUTE;
    public static final Object MOB_EFFECT;
    public static final Object SOUND_EVENT;
    public static final Object ENTITY_TYPE;
    public static final Object FLUID;
    public static final Object RECIPE_TYPE;
    public static final Object PARTICLE_TYPE;
    public static final Object DATA_COMPONENT_TYPE;

    static {
        Field[] fields = CoreReflections.clazz$BuiltInRegistries.getDeclaredFields();
        try {
            Object registries$Block = null;
            Object registries$Attribute  = null;
            Object registries$MobEffect  = null;
            Object registries$SoundEvent  = null;
            Object registries$ParticleType  = null;
            Object registries$EntityType  = null;
            Object registries$Item  = null;
            Object registries$Fluid  = null;
            Object registries$RecipeType  = null;
            Object registries$DataComponentType  = null;
            for (Field field : fields) {
                Type fieldType = field.getGenericType();
                if (fieldType instanceof ParameterizedType paramType) {
                    Type type = paramType.getActualTypeArguments()[0];
                    if (type instanceof ParameterizedType parameterizedType) {
                        Type rawType = parameterizedType.getRawType();
                        if (rawType == CoreReflections.clazz$ParticleType) {
                            registries$ParticleType = field.get(null);
                        } else if (rawType == CoreReflections.clazz$EntityType) {
                            registries$EntityType = field.get(null);
                        } else if (rawType == CoreReflections.clazz$RecipeType) {
                            registries$RecipeType = field.get(null);
                        } else if (rawType == CoreReflections.clazz$DataComponentType && registries$DataComponentType == null) {
                            registries$DataComponentType = field.get(null);
                        }
                    } else {
                        if (type == CoreReflections.clazz$Block) {
                            registries$Block = field.get(null);
                        } else if (type == CoreReflections.clazz$Attribute) {
                            registries$Attribute = field.get(null);
                        } else if (type == CoreReflections.clazz$MobEffect) {
                            registries$MobEffect = field.get(null);
                        } else if (type == CoreReflections.clazz$SoundEvent) {
                            registries$SoundEvent = field.get(null);
                        } else if (type == CoreReflections.clazz$Item) {
                            registries$Item = field.get(null);
                        } else if (type == CoreReflections.clazz$Fluid) {
                            registries$Fluid = field.get(null);
                        }
                    }
                }
            }
            BLOCK = requireNonNull(registries$Block);
            ITEM = requireNonNull(registries$Item);
            ATTRIBUTE = requireNonNull(registries$Attribute);
            MOB_EFFECT = requireNonNull(registries$MobEffect);
            SOUND_EVENT = requireNonNull(registries$SoundEvent);
            PARTICLE_TYPE = requireNonNull(registries$ParticleType);
            ENTITY_TYPE = requireNonNull(registries$EntityType);
            FLUID = requireNonNull(registries$Fluid);
            RECIPE_TYPE = requireNonNull(registries$RecipeType);
            DATA_COMPONENT_TYPE = registries$DataComponentType;
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to init BuiltInRegistries", e);
        }
    }
}
