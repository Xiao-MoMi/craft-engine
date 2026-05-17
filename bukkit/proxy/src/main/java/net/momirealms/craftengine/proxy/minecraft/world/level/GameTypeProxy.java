package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.GameType")
public interface GameTypeProxy {
    GameTypeProxy INSTANCE = ASMProxyFactory.create(GameTypeProxy.class);
    Enum<?>[] VALUES = INSTANCE.values();
    Enum<?> SURVIVAL = VALUES[0];
    Enum<?> CREATIVE = VALUES[1];
    Enum<?> ADVENTURE = VALUES[2];
    Enum<?> SPECTATOR = VALUES[3];

    @MethodInvoker(name = "values", isStatic = true)
    Enum<?>[] values();
}
