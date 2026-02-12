package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.RecipeMap", activeIf = "min_version=1.21.2")
public interface RecipeMapProxy {
    RecipeMapProxy INSTANCE = ASMProxyFactory.create(RecipeMapProxy.class);

    @MethodInvoker(name = "removeRecipe")
    boolean removeRecipe(Object target, @Type(clazz = ResourceKeyProxy.class) Object id);
}
