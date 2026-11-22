package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

import java.util.List;
import java.util.stream.Stream;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.Ingredient")
public interface IngredientProxy {
    IngredientProxy INSTANCE = ASMProxyFactory.create(IngredientProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.crafting.Ingredient");

    @FieldGetter(name = "EMPTY", isStatic = true, activeIf = "max_version=1.21.1")
    Object empty();

    @MethodInvoker(name = "of", isStatic = true)
    Object of(Stream<?> values);

    @MethodInvoker(name = "ofStacks", isStatic = true, activeIf = "min_version=1.21.2", optional = true)
    default Object ofStacks(List<Object> stacks) {
        return null;
    }

    @FieldSetter(name = "exact", activeIf = "max_version=1.21.1", optional = true)
    default void setExact(Object target, boolean exact) {
    }

    @FieldSetter(name = "itemStacks", activeIf = "max_version=1.21.1")
    void setItemStacks(Object target, Object stacks);

    @MethodInvoker(name = "test")
    boolean test(Object target, @Type(clazz = ItemStackProxy.class) Object stack);
}
