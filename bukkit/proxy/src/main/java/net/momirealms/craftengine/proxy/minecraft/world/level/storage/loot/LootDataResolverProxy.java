package net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot;

import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.storage.loot.LootDataResolver", activeIf = "max_version=1.20.4")
public interface LootDataResolverProxy {
    LootDataResolverProxy INSTANCE = ASMProxyFactory.create(LootDataResolverProxy.class);

    @MethodInvoker(name = "getLootTable")
    Object getLootTable(Object target, @Type(clazz = IdentifierProxy.class) Object id);
}
