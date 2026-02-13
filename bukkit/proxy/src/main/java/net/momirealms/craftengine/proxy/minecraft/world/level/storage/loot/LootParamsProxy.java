package net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot;

import net.momirealms.craftengine.proxy.minecraft.util.context.ContextKeyProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.storage.loot.LootParams")
public interface LootParamsProxy {
    LootParamsProxy INSTANCE = ASMProxyFactory.create(LootParamsProxy.class);

    @ReflectionProxy(name = "net.minecraft.world.level.storage.loot.LootParams$Builder")
    interface BuilderProxy {
        BuilderProxy INSTANCE = ASMProxyFactory.create(BuilderProxy.class);
        Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.storage.loot.LootParams$Builder");

        @FieldGetter(name = "level")
        Object getLevel(Object target);

        @MethodInvoker(name = "getOptionalParameter")
        <T> T getOptionalParameter(Object target, @Type(clazz = ContextKeyProxy.class) Object parameter);
    }
}
