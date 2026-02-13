package net.momirealms.craftengine.proxy.minecraft.core.registries;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.registries.BuiltInRegistries")
public interface BuiltInRegistriesProxy {
    BuiltInRegistriesProxy INSTANCE = ASMProxyFactory.create(BuiltInRegistriesProxy.class);
    Object BLOCK = INSTANCE.getBlock();
    Object ITEM = INSTANCE.getItem();
    Object ATTRIBUTE = INSTANCE.getAttribute();
    Object MOB_EFFECT = INSTANCE.getMobEffect();
    Object SOUND_EVENT = INSTANCE.getSoundEvent();
    Object ENTITY_TYPE = INSTANCE.getEntityType();
    Object BLOCK_ENTITY_TYPE = INSTANCE.getBlockEntityType();
    Object FLUID = INSTANCE.getFluid();
    Object RECIPE_TYPE = INSTANCE.getRecipeType();
    Object PARTICLE_TYPE = INSTANCE.getParticleType();
    Object DATA_COMPONENT_TYPE = INSTANCE.getDataComponentType();
    Object DATA_COMPONENT_PREDICATE_TYPE = INSTANCE.getDataComponentPredicateType();
    Object LOOT_POOL_ENTRY_TYPE = INSTANCE.getLootPoolEntryType();
    Object GAME_EVENT = INSTANCE.getGameEvent();
    Object BLOCKSTATE_PROVIDER_TYPE = INSTANCE.getBlockstateProviderType();
    Object FEATURE = INSTANCE.getFeature();

    @FieldGetter(name = "BLOCK", isStatic = true)
    Object getBlock();

    @FieldGetter(name = "ITEM", isStatic = true)
    Object getItem();

    @FieldGetter(name = "ATTRIBUTE", isStatic = true)
    Object getAttribute();

    @FieldGetter(name = "MOB_EFFECT", isStatic = true)
    Object getMobEffect();

    @FieldGetter(name = "SOUND_EVENT", isStatic = true)
    Object getSoundEvent();

    @FieldGetter(name = "ENTITY_TYPE", isStatic = true)
    Object getEntityType();

    @FieldGetter(name = "BLOCK_ENTITY_TYPE", isStatic = true)
    Object getBlockEntityType();

    @FieldGetter(name = "FLUID", isStatic = true)
    Object getFluid();

    @FieldGetter(name = "RECIPE_TYPE", isStatic = true)
    Object getRecipeType();

    @FieldGetter(name = "PARTICLE_TYPE", isStatic = true)
    Object getParticleType();

    @FieldGetter(name = "DATA_COMPONENT_TYPE", isStatic = true, activeIf = "min_version=1.20.5")
    default Object getDataComponentType() {
        return null;
    }

    @FieldGetter(name = "DATA_COMPONENT_PREDICATE_TYPE", isStatic = true, activeIf = "min_version=1.20.5")
    default Object getDataComponentPredicateType() {
        return null;
    }

    @FieldGetter(name = "LOOT_POOL_ENTRY_TYPE", isStatic = true)
    Object getLootPoolEntryType();

    @FieldGetter(name = "GAME_EVENT", isStatic = true)
    Object getGameEvent();

    @FieldGetter(name = "BLOCKSTATE_PROVIDER_TYPE", isStatic = true)
    Object getBlockstateProviderType();

    @FieldGetter(name = "FEATURE", isStatic = true)
    Object getFeature();
}
