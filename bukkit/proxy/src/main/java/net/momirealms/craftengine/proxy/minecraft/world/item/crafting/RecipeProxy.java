package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.craftengine.proxy.minecraft.core.HolderLookupProxy;
import net.momirealms.craftengine.proxy.minecraft.world.ContainerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.Recipe")
public interface RecipeProxy {
    RecipeProxy INSTANCE = ASMProxyFactory.create(RecipeProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.crafting.Recipe");

    @MethodInvoker(name = "getId", activeIf = "max_version=1.20.1")
    Object getId(Object target);

    @MethodInvoker(name = "getType")
    Object getType(Object target);

    @MethodInvoker(name = "matches", activeIf = "max_version=1.20.6")
    boolean matches$legacy(Object target, @Type(clazz = ContainerProxy.class) Object input, @Type(clazz = LevelProxy.class) Object level);

    @MethodInvoker(name = "matches", activeIf = "min_version=1.21")
    boolean matches(Object target, @Type(clazz = RecipeInputProxy.class) Object input, @Type(clazz = LevelProxy.class) Object level);

    @MethodInvoker(name = "getResultItem", activeIf = "min_version=1.20.5 && max_version=1.21.1")
    Object getResultItem(Object target, @Type(clazz = HolderLookupProxy.ProviderProxy.class) Object registries);
}
