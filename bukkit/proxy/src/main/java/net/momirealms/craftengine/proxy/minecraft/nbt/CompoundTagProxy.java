package net.momirealms.craftengine.proxy.minecraft.nbt;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.nbt.CompoundTag")
public interface CompoundTagProxy {
    CompoundTagProxy INSTANCE = ASMProxyFactory.create(CompoundTagProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.nbt.CompoundTag");
}
