package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import com.google.common.collect.Multimap;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.world.flag.FeatureFlagSetProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

import java.util.Map;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.RecipeManager")
public interface RecipeManagerProxy {
    RecipeManagerProxy INSTANCE = ASMProxyFactory.create(RecipeManagerProxy.class);

    @MethodInvoker(name = "finalizeRecipeLoading", activeIf = "min_version=1.21.2")
    void finalizeRecipeLoading(Object target, @Type(clazz = FeatureFlagSetProxy.class) Object flagSet);

    @FieldGetter(name = {"enabledFlags", "featureflagset"}, activeIf = "min_version=1.21.2")
    Object getEnabledFlags(Object target);

    @FieldSetter(name = {"enabledFlags", "featureflagset"}, activeIf = "min_version=1.21.2")
    void setEnabledFlags(Object target, Object value);

    @FieldGetter(name = "byType", activeIf = "min_version=1.20.5 && max_version=1.21.1")
    Multimap<Object, Object> getByType(Object target);

    @FieldSetter(name = "byType", activeIf = "min_version=1.20.5 && max_version=1.21.1")
    void setByType(Object target, Multimap<Object, Object> value);

    @FieldGetter(name = "recipes", activeIf = "max_version=1.20.4")
    Map<Object, Object> getByType$legacy(Object target);

    @FieldSetter(name = "recipes", activeIf = "max_version=1.20.4")
    void setByType$legacy(Object target, Map<Object, Object> value);

    @FieldGetter(name = "byName", activeIf = "max_version=1.21.1")
    Map<Object, Object> getByName(Object target);

    @FieldSetter(name = "byName", activeIf = "max_version=1.21.1")
    void setByName(Object target, Map<Object, Object> value);

    @FieldGetter(name = "recipes", activeIf = "min_version=1.21.2")
    Object getRecipes(Object target);

    @FieldSetter(name = "recipes", activeIf = "min_version=1.21.2")
    void setRecipes(Object target, Object value);

    @MethodInvoker(name = "removeRecipe", activeIf = "min_version=1.21.2")
    boolean removeRecipe$0(Object target, @Type(clazz = ResourceKeyProxy.class) Object id);

    @MethodInvoker(name = "removeRecipe", activeIf = "max_version=1.21.1")
    boolean removeRecipe$1(Object target, @Type(clazz = IdentifierProxy.class) Object id);

    @MethodInvoker(name = "addRecipe", activeIf = "min_version=1.20.2")
    void addRecipe$0(Object target, @Type(clazz = RecipeHolderProxy.class) Object holder);

    @MethodInvoker(name = "addRecipe", activeIf = "max_version=1.20.1")
    void addRecipe$1(Object target, @Type(clazz = RecipeProxy.class) Object recipe);
}
