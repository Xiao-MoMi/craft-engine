package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import java.util.function.Predicate;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.Ingredient")
public interface IngredientProxy {
    IngredientProxy INSTANCE = ASMProxyFactory.create(IngredientProxy.class);

    @FieldSetter(name = "stackPredicate", activeIf = "min_version=26.3 && has_patch=paper")
    void setStackPredicate(Object target, Predicate<Object> predicate);

    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.crafting.Ingredient");
}
