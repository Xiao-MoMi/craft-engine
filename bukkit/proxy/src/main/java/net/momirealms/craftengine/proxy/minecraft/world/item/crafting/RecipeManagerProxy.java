package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.RecipeManager")
public interface RecipeManagerProxy {
    RecipeManagerProxy INSTANCE = ASMProxyFactory.create(RecipeManagerProxy.class);

    @MethodInvoker(name = "finalizeRecipeLoading", activeIf = "min_version=1.21.2")
    void finalizeRecipeLoading(Object target);

    @FieldGetter(name = "featureflagset", activeIf = "min_version=1.21.2")
    Object getFeatureFlagSet(Object target);

    @FieldSetter(name = "featureflagset", activeIf = "min_version=1.21.2")
    void setFeatureFlagSet(Object target, Object value);
}
