package net.momirealms.craftengine.proxy.minecraft.network;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.Optional;

@ReflectionProxy(name = "net.minecraft.network.HashedStack", activeIf = "min_version=1.21.5")
public interface HashedStackProxy {
    HashedStackProxy INSTANCE = ASMProxyFactory.create(HashedStackProxy.class);
    Object STREAM_CODEC = Optional.ofNullable(INSTANCE).map(HashedStackProxy::getStreamCodec).orElse(null);

    @FieldGetter(name = "STREAM_CODEC", isStatic = true)
    Object getStreamCodec();
}
