package net.momirealms.craftengine.proxy.minecraft.world.level.material;

import net.momirealms.craftengine.proxy.minecraft.core.TypedInstanceProxy;
import net.momirealms.craftengine.proxy.minecraft.tags.TagKeyProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.material.FluidState")
public interface FluidStateProxy extends TypedInstanceProxy {
    FluidStateProxy INSTANCE = ASMProxyFactory.create(FluidStateProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.material.FluidState");

    @MethodInvoker(name = "getAmount")
    int getAmount(Object target);

    @MethodInvoker(name = "is", activeIf = "max_version=1.21.11")
    boolean is$0(Object target, @Type(clazz = TagKeyProxy.class) Object tag);

    @MethodInvoker(name = "createLegacyBlock")
    Object createLegacyBlock(Object target);

    @MethodInvoker(name = "getType")
    Object getType(Object target);
}
