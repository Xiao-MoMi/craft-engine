package net.momirealms.craftengine.proxy.minecraft.resources;

import com.mojang.serialization.DynamicOps;
import net.momirealms.craftengine.proxy.minecraft.core.HolderLookupProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.resources.RegistryOps")
public interface RegistryOpsProxy {
    RegistryOpsProxy INSTANCE = ASMProxyFactory.create(RegistryOpsProxy.class);

    @MethodInvoker(name = "create", isStatic = true)
    <T> DynamicOps<T> create(DynamicOps<T> delegate, @Type(clazz = HolderLookupProxy.ProviderProxy.class) Object wrapperLookup);
}
