package net.momirealms.craftengine.proxy.minecraft.server;

import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.server.ReloadableServerRegistries", activeIf = "min_version=1.20.5")
public interface ReloadableServerRegistriesProxy {
    ReloadableServerRegistriesProxy INSTANCE = ASMProxyFactory.create(ReloadableServerRegistriesProxy.class);

    @ReflectionProxy(name = "net.minecraft.server.ReloadableServerRegistries$Holder", activeIf = "min_version=1.20.5")
    interface HolderProxy {
        HolderProxy INSTANCE = ASMProxyFactory.create(HolderProxy.class);

        @MethodInvoker(name = "getLootTable")
        Object getLootTable(Object target, @Type(clazz = ResourceKeyProxy.class) Object lootTableKey);
    }
}
