package net.momirealms.craftengine.proxy.minecraft.world.level.chunk;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.function.Consumer;

@ReflectionProxy(name = "net.minecraft.world.level.chunk.PalettedContainerRO")
public interface PalettedContainerROProxy {
    PalettedContainerROProxy INSTANCE = ASMProxyFactory.create(PalettedContainerROProxy.class);

    @MethodInvoker(name = "getAll")
    void getAll(Object target, Consumer<Object> consumer);
}
