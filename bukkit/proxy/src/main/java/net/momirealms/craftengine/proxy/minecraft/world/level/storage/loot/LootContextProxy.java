package net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot;

import net.momirealms.craftengine.proxy.minecraft.util.context.ContextKeyProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.storage.loot.LootContext")
public interface LootContextProxy {
    LootContextProxy INSTANCE = ASMProxyFactory.create(LootContextProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.storage.loot.LootContext");

    @MethodInvoker(name = "getParamOrNull", activeIf = "max_version=1.21.1")
    Object getParamOrNull(Object target, @Type(clazz = ContextKeyProxy.class) Object parameter);

    @MethodInvoker(name = "getOptionalParameter", activeIf = "min_version=1.21.2 && max_version=26.2")
    Object getOptionalParameter(Object target, @Type(clazz = ContextKeyProxy.class) Object parameter);

    @MethodInvoker(name = "getOptional", activeIf = "min_version=26.3")
    Object getOptional(Object target, @Type(clazz = ContextKeyProxy.class) Object parameter);
}
