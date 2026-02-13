package net.momirealms.craftengine.proxy.minecraft.core.registries;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.registries.Registries")
public interface RegistriesProxy {
    RegistriesProxy INSTANCE = ASMProxyFactory.create(RegistriesProxy.class);
    Object BLOCK = INSTANCE.getBlock();
    Object ITEM = INSTANCE.getItem();
    Object ATTRIBUTE = INSTANCE.getAttribute();
    Object BIOME = INSTANCE.getBiome();
    Object MOB_EFFECT = INSTANCE.getMobEffect();
    Object SOUND_EVENT = INSTANCE.getSoundEvent();
    Object PARTICLE_TYPE = INSTANCE.getParticleType();
    Object ENTITY_TYPE = INSTANCE.getEntityType();
    Object FLUID = INSTANCE.getFluid();
    Object RECIPE_TYPE = INSTANCE.getRecipeType();
    Object DIMENSION_TYPE = INSTANCE.getDimensionType();
    Object CONFIGURED_FEATURE = INSTANCE.getConfiguredFeature();
    Object PLACED_FEATURE = INSTANCE.getPlacedFeature();
    Object TRIM_PATTERN = INSTANCE.getTrimPattern();
    Object TRIM_MATERIAL = INSTANCE.getTrimMaterial();
    Object JUKEBOX_SONG = INSTANCE.getJukeboxSong();
    Object RECIPE = INSTANCE.getRecipe();

    @FieldGetter(name = "BLOCK", isStatic = true)
    Object getBlock();

    @FieldGetter(name = "ITEM", isStatic = true)
    Object getItem();

    @FieldGetter(name = "ATTRIBUTE", isStatic = true)
    Object getAttribute();

    @FieldGetter(name = "BIOME", isStatic = true)
    Object getBiome();

    @FieldGetter(name = "MOB_EFFECT", isStatic = true)
    Object getMobEffect();

    @FieldGetter(name = "SOUND_EVENT", isStatic = true)
    Object getSoundEvent();

    @FieldGetter(name = "PARTICLE_TYPE", isStatic = true)
    Object getParticleType();

    @FieldGetter(name = "ENTITY_TYPE", isStatic = true)
    Object getEntityType();

    @FieldGetter(name = "FLUID", isStatic = true)
    Object getFluid();

    @FieldGetter(name = "RECIPE_TYPE", isStatic = true)
    Object getRecipeType();

    @FieldGetter(name = "DIMENSION_TYPE", isStatic = true)
    Object getDimensionType();

    @FieldGetter(name = "CONFIGURED_FEATURE", isStatic = true)
    Object getConfiguredFeature();

    @FieldGetter(name = "PLACED_FEATURE", isStatic = true)
    Object getPlacedFeature();

    @FieldGetter(name = "TRIM_PATTERN", isStatic = true)
    Object getTrimPattern();

    @FieldGetter(name = "TRIM_MATERIAL", isStatic = true)
    Object getTrimMaterial();

    @FieldGetter(name = "JUKEBOX_SONG", isStatic = true, activeIf = "min_version=1.21")
    default Object getJukeboxSong() {
        return null;
    }

    @FieldGetter(name = "RECIPE", isStatic = true, activeIf = "min_version=1.21")
    default Object getRecipe() {
        return null;
    }
}