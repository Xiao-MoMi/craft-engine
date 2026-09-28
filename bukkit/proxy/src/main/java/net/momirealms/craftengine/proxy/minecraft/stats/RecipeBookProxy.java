package net.momirealms.craftengine.proxy.minecraft.stats;

import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.stats.RecipeBook")
public interface RecipeBookProxy {
    RecipeBookProxy INSTANCE = ASMProxyFactory.create(RecipeBookProxy.class);

    @MethodInvoker(name = "contains", activeIf = "max_version=1.21.1")
    boolean contains(Object target, @Type(clazz = IdentifierProxy.class) Object recipe);
}
