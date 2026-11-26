package net.momirealms.craftengine.proxy.minecraft.server.commands;

import net.momirealms.craftengine.proxy.minecraft.world.ContainerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.server.commands.LootCommand")
public interface LootCommandProxy {
    LootCommandProxy INSTANCE = ASMProxyFactory.create(LootCommandProxy.class);

    @MethodInvoker(name = "distributeToContainer", isStatic = true)
    boolean distributeToContainer(@Type(clazz = ContainerProxy.class) Object container, @Type(clazz = ItemStackProxy.class) Object stack);
}
