package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.CraftingInput", activeIf = "min_version=1.21")
public interface CraftingInputProxy {
    CraftingInputProxy INSTANCE = ASMProxyFactory.create(CraftingInputProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.crafting.CraftingInput");

    @MethodInvoker(name = "getItem")
    Object getItem(Object target, int index);

    @MethodInvoker(name = "size")
    int size(Object target);

    @MethodInvoker(name = "ingredientCount")
    int ingredientCount(Object target);

    @MethodInvoker(name = "width")
    int width(Object target);

    @MethodInvoker(name = "height")
    int height(Object target);

    @MethodInvoker(name = "items")
    List<Object> items(Object target);
}
