package net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot;

import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.util.context.ContextKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.util.context.ContextKeySetProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.world.level.storage.loot.LootParams")
public interface LootParamsProxy {
    LootParamsProxy INSTANCE = ASMProxyFactory.create(LootParamsProxy.class);

    @ReflectionProxy(name = "net.minecraft.world.level.storage.loot.LootParams$Builder")
    interface BuilderProxy {
        BuilderProxy INSTANCE = ASMProxyFactory.create(BuilderProxy.class);
        Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.storage.loot.LootParams$Builder");

        @ConstructorInvoker
        Object newInstance(@Type(clazz = ServerLevelProxy.class) Object level);

        @FieldGetter(name = "level")
        Object getLevel(Object target);

        @MethodInvoker(name = "withParameter")
        Object withParameter(Object target, @Type(clazz = ContextKeyProxy.class) Object parameter, Object value);

        @MethodInvoker(name = "withOptionalParameter")
        Object withOptionalParameter(Object target, @Type(clazz = ContextKeyProxy.class) Object parameter, Object value);

        @MethodInvoker(name = "getOptionalParameter")
        <T> T getOptionalParameter(Object target, @Type(clazz = ContextKeyProxy.class) Object parameter);

        @MethodInvoker(name = "withLuck")
        Object withLuck(Object target, float luck);

        @MethodInvoker(name = "create")
        Object create(Object target, @Type(clazz = ContextKeySetProxy.class) Object contextKeySet);
    }
}
