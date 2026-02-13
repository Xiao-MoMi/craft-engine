package net.momirealms.craftengine.proxy.minecraft.world.level.material;

import net.momirealms.craftengine.proxy.minecraft.tags.TagKeyProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.material.FluidState")
public interface FluidStateProxy {
    FluidStateProxy INSTANCE = ASMProxyFactory.create(FluidStateProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.material.FluidState");

    @MethodInvoker(name = "getAmount")
    int getAmount(Object target);

    @MethodInvoker(name = "is")
    boolean is(Object target, @Type(clazz = TagKeyProxy.class) Object tag);

    @MethodInvoker(name = "createLegacyBlock")
    Object createLegacyBlock(Object target);

    @MethodInvoker(name = "getType")
    Object getType(Object target);
}
