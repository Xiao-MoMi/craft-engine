package net.momirealms.craftengine.proxy.minecraft.server.packs.resources;

import net.momirealms.craftengine.proxy.minecraft.server.packs.PackTypeProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.List;

@ReflectionProxy(name = "net.minecraft.server.packs.resources.MultiPackResourceManager")
public interface MultiPackResourceManagerProxy {
    MultiPackResourceManagerProxy INSTANCE = ASMProxyFactory.create(MultiPackResourceManagerProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = PackTypeProxy.class) Object type,
                       List<Object> packs);
}
