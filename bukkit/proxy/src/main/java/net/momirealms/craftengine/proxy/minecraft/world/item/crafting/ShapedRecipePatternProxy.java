package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.craftengine.proxy.minecraft.world.inventory.CraftingContainerProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Map;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.ShapedRecipePattern", activeIf = "min_version=1.20.3")
public interface ShapedRecipePatternProxy {
    ShapedRecipePatternProxy INSTANCE = ASMProxyFactory.create(ShapedRecipePatternProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.crafting.ShapedRecipePattern");

    @MethodInvoker(name = "of", isStatic = true)
    Object of(Map<Character, Object> ingredients, String[] pattern);

    @MethodInvoker(name = "matches", activeIf = "min_version=1.20.3 && max_version=1.20.6")
    boolean matches$legacy(Object target, @Type(clazz = CraftingContainerProxy.class) Object input);

    @MethodInvoker(name = "matches", activeIf = "min_version=1.21")
    boolean matches(Object target, @Type(clazz = CraftingInputProxy.class) Object input);
}
