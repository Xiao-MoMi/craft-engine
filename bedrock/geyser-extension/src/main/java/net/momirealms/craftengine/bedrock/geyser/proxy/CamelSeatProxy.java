package net.momirealms.craftengine.bedrock.geyser.proxy;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.geysermc.geyser.entity.vehicle.CamelVehicleComponent")
public interface CamelSeatProxy {
    CamelSeatProxy INSTANCE = ASMProxyFactory.create(CamelSeatProxy.class);

    @FieldGetter(name = "lastPoseTick")
    long lastPoseTick(Object component);
}
