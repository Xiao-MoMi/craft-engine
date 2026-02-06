package net.momirealms.craftengine.proxy.minecraft.util;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.Optional;

@ReflectionProxy(name = "net.minecraft.util.HashOps", activeIf = "min_version=1.21.5")
public interface HashOpsProxy {
    HashOpsProxy INSTANCE = ASMProxyFactory.create(HashOpsProxy.class);
    Object CRC32C_INSTANCE = Optional.ofNullable(INSTANCE).map(HashOpsProxy::getCRC32C).orElse(null);

    @FieldGetter(name = "CRC32C_INSTANCE")
    Object getCRC32C();
}
