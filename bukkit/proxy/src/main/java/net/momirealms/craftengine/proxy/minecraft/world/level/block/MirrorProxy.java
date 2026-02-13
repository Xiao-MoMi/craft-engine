package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.Mirror")
public interface MirrorProxy {
    MirrorProxy INSTANCE = ASMProxyFactory.create(MirrorProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.block.Mirror");
}
