package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.ContainerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Optional;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.RecipeManager$CachedCheck")
public interface CachedRecipeCheckProxy {
    CachedRecipeCheckProxy INSTANCE = ASMProxyFactory.create(CachedRecipeCheckProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.crafting.RecipeManager$CachedCheck");

    @MethodInvoker(name = "getRecipeFor", activeIf = "max_version=1.20.6")
    Optional<?> getRecipe$legacy(Object target, @Type(clazz = ContainerProxy.class) Object input, @Type(clazz = LevelProxy.class) Object level);

    @MethodInvoker(name = "getRecipeFor", activeIf = "min_version=1.21 && max_version=1.21.1")
    Optional<?> getRecipe$1_21(Object target, @Type(clazz = RecipeInputProxy.class) Object input, @Type(clazz = LevelProxy.class) Object level);

    @MethodInvoker(name = "getRecipeFor", activeIf = "min_version=1.21.2")
    Optional<?> getRecipe(Object target, @Type(clazz = RecipeInputProxy.class) Object input, @Type(clazz = ServerLevelProxy.class) Object level);
}
