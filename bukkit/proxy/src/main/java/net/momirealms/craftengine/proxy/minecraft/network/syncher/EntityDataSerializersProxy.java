package net.momirealms.craftengine.proxy.minecraft.network.syncher;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.syncher.EntityDataSerializers")
public interface EntityDataSerializersProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.syncher.EntityDataSerializers");
}
