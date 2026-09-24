package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.VecDelta", activeIf = "min_version=26.3")
public interface VecDeltaProxy {
    VecDeltaProxy INSTANCE = ASMProxyFactory.create(VecDeltaProxy.class);
    @ReflectionProxy(name = "net.minecraft.network.protocol.game.VecDelta$Linear", activeIf = "min_version=26.3")
    interface LinearProxy {
        LinearProxy INSTANCE = ASMProxyFactory.create(LinearProxy.class);
        @ConstructorInvoker
        Object newInstance(short x, short y, short z);
    }
}
