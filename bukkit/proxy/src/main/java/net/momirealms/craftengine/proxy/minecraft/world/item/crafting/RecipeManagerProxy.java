package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.RecipeManager")
public interface RecipeManagerProxy {
    RecipeManagerProxy INSTANCE = ASMProxyFactory.create(RecipeManagerProxy.class);

    @MethodInvoker(name = "finalizeRecipeLoading", activeIf = "min_version=1.21.2")
    void finalizeRecipeLoading(Object target);

    @FieldGetter(name = "featureflagset", activeIf = "min_version=1.21.2")
    Object getFeatureFlagSet(Object target);

    @FieldSetter(name = "featureflagset", activeIf = "min_version=1.21.2")
    void setFeatureFlagSet(Object target, Object value);

    @FieldGetter(name = "recipes", activeIf = "min_version=1.21.2")
    Object getRecipes(Object target);

    @MethodInvoker(name = "removeRecipe", activeIf = "min_version=1.21.2")
    boolean removeRecipe$0(Object target, @Type(clazz = ResourceKeyProxy.class) Object id);

    @MethodInvoker(name = "removeRecipe", activeIf = "max_version=1.21.1")
    boolean removeRecipe$1(Object target, @Type(clazz = IdentifierProxy.class) Object id);

    @MethodInvoker(name = "addRecipe", activeIf = "min_version=1.20.2")
    void addRecipe$0(Object target, @Type(clazz = RecipeHolderProxy.class) Object holder);

    @MethodInvoker(name = "addRecipe", activeIf = "max_version=1.20.1")
    void addRecipe$1(Object target, @Type(clazz = RecipeProxy.class) Object recipe);
}
