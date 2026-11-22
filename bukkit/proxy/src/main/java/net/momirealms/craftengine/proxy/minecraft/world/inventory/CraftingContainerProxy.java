package net.momirealms.craftengine.proxy.minecraft.world.inventory;

import net.momirealms.craftengine.proxy.minecraft.world.ContainerProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;

@ReflectionProxy(name = "net.minecraft.world.inventory.CraftingContainer")
public interface CraftingContainerProxy extends ContainerProxy {
    CraftingContainerProxy INSTANCE = ASMProxyFactory.create(CraftingContainerProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.inventory.CraftingContainer");

    @MethodInvoker(name = "getCurrentRecipe", activeIf = "min_version=1.21")
    Object getCurrentRecipe(Object target);

    @MethodInvoker(name = "getWidth", activeIf = "max_version=1.20.6")
    int getWidth(Object target);

    @MethodInvoker(name = "getHeight", activeIf = "max_version=1.20.6")
    int getHeight(Object target);

    @MethodInvoker(name = "getItems", activeIf = "max_version=1.20.6")
    List<Object> getItems(Object target);
}
