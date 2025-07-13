package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static java.util.Objects.requireNonNull;

public final class MRegistries {
    public static final Object BLOCK;
    public static final Object ITEM;
    public static final Object ATTRIBUTE;
    public static final Object BIOME;
    public static final Object MOB_EFFECT;
    public static final Object SOUND_EVENT;
    public static final Object PARTICLE_TYPE;
    public static final Object ENTITY_TYPE;
    public static final Object FLUID;
    public static final Object RECIPE_TYPE;
    public static final Object DIMENSION_TYPE;
    public static final Object CONFIGURED_FEATURE;
    public static final Object PLACED_FEATURE;
    public static final Object TRIM_PATTERN;
    public static final Object TRIM_MATERIAL;
    @Nullable // 1.21+
    public static final Object JUKEBOX_SONG;
    @Nullable // 1.21+
    public static final Object RECIPE;

    static {
        Field[] fields = CoreReflections.clazz$Registries.getDeclaredFields();
        try {
            Object registries$Block = null;
            Object registries$Attribute  = null;
            Object registries$Biome  = null;
            Object registries$MobEffect  = null;
            Object registries$SoundEvent  = null;
            Object registries$DimensionType  = null;
            Object registries$ParticleType  = null;
            Object registries$EntityType  = null;
            Object registries$Item  = null;
            Object registries$Fluid  = null;
            Object registries$RecipeType  = null;
            Object registries$ConfiguredFeature  = null;
            Object registries$PlacedFeature  = null;
            Object registries$JukeboxSong  = null;
            Object registries$Recipe  = null;
            Object registries$TrimPattern  = null;
            Object registries$TrimMaterial  = null;
            for (Field field : fields) {
                Type fieldType = field.getGenericType();
                if (fieldType instanceof ParameterizedType paramType) {
                    if (paramType.getRawType() == CoreReflections.clazz$ResourceKey) {
                        Type[] actualTypeArguments = paramType.getActualTypeArguments();
                        if (actualTypeArguments.length == 1 && actualTypeArguments[0] instanceof ParameterizedType registryType) {
                            Type type = registryType.getActualTypeArguments()[0];
                            if (type instanceof ParameterizedType parameterizedType) {
                                Type rawType = parameterizedType.getRawType();
                                if (rawType == CoreReflections.clazz$ParticleType) {
                                    registries$ParticleType = field.get(null);
                                } else if (rawType == CoreReflections.clazz$EntityType) {
                                    registries$EntityType = field.get(null);
                                } else if (rawType == CoreReflections.clazz$RecipeType) {
                                    registries$RecipeType = field.get(null);
                                } else if (rawType == CoreReflections.clazz$ConfiguredFeature) {
                                    registries$ConfiguredFeature = field.get(null);
                                } else if (rawType == CoreReflections.clazz$Recipe) {
                                    registries$Recipe = field.get(null);
                                }
                            } else {
                                if (type == CoreReflections.clazz$Block) {
                                    registries$Block = field.get(null);
                                } else if (type == CoreReflections.clazz$Attribute) {
                                    registries$Attribute = field.get(null);
                                } else if (type == CoreReflections.clazz$Biome) {
                                    registries$Biome = field.get(null);
                                } else if (type == CoreReflections.clazz$MobEffect) {
                                    registries$MobEffect = field.get(null);
                                } else if (type == CoreReflections.clazz$SoundEvent) {
                                    registries$SoundEvent = field.get(null);
                                } else if (type == CoreReflections.clazz$DimensionType) {
                                    registries$DimensionType = field.get(null);
                                } else if (type == CoreReflections.clazz$Item) {
                                    registries$Item = field.get(null);
                                } else if (type == CoreReflections.clazz$Fluid) {
                                    registries$Fluid = field.get(null);
                                } else if (VersionHelper.isOrAbove1_21() && type == CoreReflections.clazz$JukeboxSong) {
                                    registries$JukeboxSong = field.get(null);
                                } else if (type == CoreReflections.clazz$PlacedFeature) {
                                    registries$PlacedFeature = field.get(null);
                                } else if (type == CoreReflections.clazz$TrimPattern) {
                                    registries$TrimPattern = field.get(null);
                                } else if (type == CoreReflections.clazz$TrimMaterial) {
                                    registries$TrimMaterial = field.get(null);
                                }
                            }
                        }
                    }
                }
            }
            BLOCK = requireNonNull(registries$Block);
            ITEM = requireNonNull(registries$Item);
            ATTRIBUTE = requireNonNull(registries$Attribute);
            BIOME = requireNonNull(registries$Biome);
            MOB_EFFECT = requireNonNull(registries$MobEffect);
            SOUND_EVENT = requireNonNull(registries$SoundEvent);
            DIMENSION_TYPE = requireNonNull(registries$DimensionType);
            PARTICLE_TYPE = requireNonNull(registries$ParticleType);
            ENTITY_TYPE = requireNonNull(registries$EntityType);
            FLUID = requireNonNull(registries$Fluid);
            RECIPE_TYPE = requireNonNull(registries$RecipeType);
            CONFIGURED_FEATURE = requireNonNull(registries$ConfiguredFeature);
            PLACED_FEATURE = requireNonNull(registries$PlacedFeature);
            TRIM_PATTERN = requireNonNull(registries$TrimPattern);
            TRIM_MATERIAL = requireNonNull(registries$TrimMaterial);
            JUKEBOX_SONG = registries$JukeboxSong;
            RECIPE = registries$Recipe;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
