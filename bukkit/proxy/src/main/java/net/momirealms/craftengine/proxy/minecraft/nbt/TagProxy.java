package net.momirealms.craftengine.proxy.minecraft.nbt;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.nbt.Tag")
public interface TagProxy {
    TagProxy INSTANCE = ASMProxyFactory.create(TagProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.nbt.Tag");
}
