package net.momirealms.craftengine.proxy.minecraft.world.inventory;

import net.momirealms.craftengine.proxy.minecraft.world.ContainerProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.inventory.CraftingContainer")
public interface CraftingContainerProxy extends ContainerProxy {
    CraftingContainerProxy INSTANCE = ASMProxyFactory.create(CraftingContainerProxy.class);
}
