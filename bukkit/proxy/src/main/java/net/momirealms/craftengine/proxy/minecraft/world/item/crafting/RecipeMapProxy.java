package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import com.google.common.collect.Multimap;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

import java.util.Map;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.RecipeMap", activeIf = "min_version=1.21.2")
public interface RecipeMapProxy {
    RecipeMapProxy INSTANCE = ASMProxyFactory.create(RecipeMapProxy.class);

    @ConstructorInvoker
    Object newInstance(Multimap<Object, Object> byType, Map<Object, Object> byKey);

    @MethodInvoker(name = "removeRecipe")
    boolean removeRecipe(Object target, @Type(clazz = ResourceKeyProxy.class) Object id);

    @MethodInvoker(name = "addRecipe")
    void addRecipe(Object target, @Type(clazz = RecipeHolderProxy.class) Object holder);

    @FieldGetter(name = "byType")
    Multimap<Object, Object> getByType(Object target);

    @FieldGetter(name = "byKey")
    Map<Object, Object> getByKey(Object target);

    @MethodInvoker(name = "byKey")
    Object byKey(Object target, @Type(clazz = ResourceKeyProxy.class) Object recipeId);
}
