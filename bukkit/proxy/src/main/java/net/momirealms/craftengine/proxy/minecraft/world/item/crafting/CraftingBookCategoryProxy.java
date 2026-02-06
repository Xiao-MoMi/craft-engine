package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.CraftingBookCategory")
public interface CraftingBookCategoryProxy {
    CraftingBookCategoryProxy INSTANCE = ASMProxyFactory.create(CraftingBookCategoryProxy.class);
    Object[] VALUES = INSTANCE.values();
    Object BUILDING = VALUES[0];
    Object REDSTONE = VALUES[1];
    Object EQUIPMENT = VALUES[2];
    Object MISC = VALUES[3];

    @MethodInvoker(name = "values", isStatic = true)
    Object[] values();
}
