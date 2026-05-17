package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.SupportType")
public interface SupportTypeProxy {
    SupportTypeProxy INSTANCE = ASMProxyFactory.create(SupportTypeProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.block.SupportType");
    Enum<?>[] VALUES = INSTANCE.values();
    Enum<?> FULL = VALUES[0];
    Enum<?> CENTER = VALUES[1];
    Enum<?> RIGID = VALUES[2];

    @MethodInvoker(name = "values", isStatic = true)
    Enum<?>[] values();
}
