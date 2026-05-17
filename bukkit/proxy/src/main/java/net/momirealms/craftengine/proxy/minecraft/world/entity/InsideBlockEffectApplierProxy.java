package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.InsideBlockEffectApplier")
public interface InsideBlockEffectApplierProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.entity.InsideBlockEffectApplier");
}
