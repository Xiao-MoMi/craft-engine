package net.momirealms.craftengine.proxy.minecraft.stats;

import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.stats.ServerRecipeBook")
public interface ServerRecipeBookProxy {
    ServerRecipeBookProxy INSTANCE = ASMProxyFactory.create(ServerRecipeBookProxy.class);

    @MethodInvoker(name = "contains", activeIf = "min_version=1.21.2")
    boolean contains(Object target, @Type(clazz = ResourceKeyProxy.class) Object recipe);
}
