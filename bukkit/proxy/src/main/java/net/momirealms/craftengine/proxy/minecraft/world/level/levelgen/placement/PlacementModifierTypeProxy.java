package net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.placement;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = {"net.minecraft.world.level.levelgen.placement.PlacementModifierType", "net.minecraft.world.level.levelgen.placement.BiomeFilter"})
public interface PlacementModifierTypeProxy {
    PlacementModifierTypeProxy INSTANCE = ASMProxyFactory.create(PlacementModifierTypeProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.levelgen.placement.PlacementModifierType", "net.minecraft.world.level.levelgen.placement.BiomeFilter");

    @FieldGetter(name = {"BIOME_FILTER", "CODEC"}, isStatic = true)
    Object getBiomeFilter();
}
