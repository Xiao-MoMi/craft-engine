package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.GameType")
public interface GameTypeProxy {
    GameTypeProxy INSTANCE = ASMProxyFactory.create(GameTypeProxy.class);
    Object[] VALUES = INSTANCE.values();
    Object SURVIVAL = VALUES[0];
    Object CREATIVE = VALUES[1];
    Object ADVENTURE = VALUES[2];
    Object SPECTATOR = VALUES[3];

    @MethodInvoker(name = "values", isStatic = true)
    Object[] values();
}
