package net.momirealms.craftengine.proxy.minecraft.nbt;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.Map;

@ReflectionProxy(name = "net.minecraft.nbt.CompoundTag")
public interface CompoundTagProxy {
    CompoundTagProxy INSTANCE = ASMProxyFactory.create(CompoundTagProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.nbt.CompoundTag");

    @FieldGetter(name = "tags")
    Map<String, Object> getTags(Object target);

    @MethodInvoker(name = "copy")
    Object copy(Object target);

    @MethodInvoker(name = "merge")
    Object merge(Object target, Object other);
}
