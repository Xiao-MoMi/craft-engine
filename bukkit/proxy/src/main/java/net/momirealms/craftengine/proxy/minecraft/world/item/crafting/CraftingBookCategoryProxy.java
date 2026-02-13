package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.CraftingBookCategory")
public interface CraftingBookCategoryProxy {
    CraftingBookCategoryProxy INSTANCE = ASMProxyFactory.create(CraftingBookCategoryProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.crafting.CraftingBookCategory");
    Enum<?>[] VALUES = INSTANCE.values();
    Enum<?> BUILDING = VALUES[0];
    Enum<?> REDSTONE = VALUES[1];
    Enum<?> EQUIPMENT = VALUES[2];
    Enum<?> MISC = VALUES[3];

    @MethodInvoker(name = "values", isStatic = true)
    Enum<?>[] values();
}
