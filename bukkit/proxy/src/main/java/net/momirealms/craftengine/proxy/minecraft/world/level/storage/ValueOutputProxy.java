package net.momirealms.craftengine.proxy.minecraft.world.level.storage;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.storage.ValueOutput", activeIf = "min_version=1.21.6")
public interface ValueOutputProxy {
    ValueOutputProxy INSTANCE = ASMProxyFactory.create(ValueOutputProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.storage.ValueOutput");

    @MethodInvoker(name = "putString", activeIf = "min_version=1.21.6")
    void putString(Object target, String key, String value);
}
