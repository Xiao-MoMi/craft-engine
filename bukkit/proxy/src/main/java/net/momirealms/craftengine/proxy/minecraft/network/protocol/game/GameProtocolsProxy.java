package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.GameProtocols", activeIf = "min_version=1.20.5")
public interface GameProtocolsProxy {
    GameProtocolsProxy INSTANCE = ASMProxyFactory.create(GameProtocolsProxy.class);

    @FieldGetter(name = "CLIENTBOUND", isStatic = true, activeIf = "max_version=1.20.6")
    Object clientbound$legacy();

    @FieldGetter(name = "CLIENTBOUND_TEMPLATE", isStatic = true, activeIf = "min_version=1.21")
    Object clientbound();

    @FieldGetter(name = "SERVERBOUND", isStatic = true, activeIf = "max_version=1.20.6")
    Object serverbound$legacy();

    @FieldGetter(name = "SERVERBOUND_TEMPLATE", isStatic = true, activeIf = "min_version=1.21")
    Object serverbound();
}
