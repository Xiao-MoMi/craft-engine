package net.momirealms.craftengine.proxy.minecraft.world.entity.ai.attributes;

import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.function.Consumer;

@ReflectionProxy(name = "net.minecraft.world.entity.ai.attributes.AttributeInstance")
public interface AttributeInstanceProxy {
    AttributeInstanceProxy INSTANCE = ASMProxyFactory.create(AttributeInstanceProxy.class);

    @ConstructorInvoker(activeIf = "min_version=1.20.5")
    Object newInstance$0(@Type(clazz = HolderProxy.class) Object type, Consumer<Object> updateCallback);

    @ConstructorInvoker(activeIf = "max_version=1.20.4")
    Object newInstance$1(@Type(clazz = AttributeProxy.class) Object type, Consumer<Object> updateCallback);

    @MethodInvoker(name = "setBaseValue")
    void setBaseValue(Object target, double baseValue);

    @MethodInvoker(name = "getValue")
    double getValue(Object target);
}
