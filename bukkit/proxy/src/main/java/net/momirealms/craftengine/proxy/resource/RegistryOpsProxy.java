package net.momirealms.craftengine.proxy.resource;

import com.mojang.serialization.DynamicOps;
import net.momirealms.craftengine.proxy.core.HolderLookupProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.resources.RegistryOps")
public interface RegistryOpsProxy {
    RegistryOpsProxy INSTANCE = ASMProxyFactory.create(RegistryOpsProxy.class);

    @MethodInvoker(name = "create", isStatic = true)
    Object create(DynamicOps<?> ops, @Type(clazz = HolderLookupProxy.ProviderProxy.class) Object registries);
}
