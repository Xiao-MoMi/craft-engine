package net.momirealms.craftengine.proxy.minecraft.util.context;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = {"net.minecraft.util.context.ContextKeySet", "net.minecraft.world.level.storage.loot.parameters.LootContextParamSet"})
public interface ContextKeySetProxy {
    ContextKeySetProxy INSTANCE = ASMProxyFactory.create(ContextKeySetProxy.class);

    @ReflectionProxy(name = {"net.minecraft.util.context.ContextKeySet$Builder", "net.minecraft.world.level.storage.loot.parameters.LootContextParamSet$Builder"})
    interface BuilderProxy {
        BuilderProxy INSTANCE = ASMProxyFactory.create(BuilderProxy.class);

        @ConstructorInvoker
        Object newInstance();

        @MethodInvoker(name = "optional")
        Object optional(Object target, @Type(clazz = ContextKeyProxy.class) Object key);

        @MethodInvoker(name = "build")
        Object build(Object target);
    }
}
