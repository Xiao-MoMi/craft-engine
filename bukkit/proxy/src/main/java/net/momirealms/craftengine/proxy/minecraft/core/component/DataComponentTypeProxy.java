package net.momirealms.craftengine.proxy.minecraft.core.component;

import com.mojang.serialization.Codec;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.component.DataComponentType", activeIf = "min_version=1.20.5")
public interface DataComponentTypeProxy {
    DataComponentTypeProxy INSTANCE = ASMProxyFactory.create(DataComponentTypeProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.core.component.DataComponentType");

    @MethodInvoker(name = "codec")
    <T> Codec<T> codec(Object target);
}
