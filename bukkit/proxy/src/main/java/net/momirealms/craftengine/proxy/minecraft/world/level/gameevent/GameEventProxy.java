package net.momirealms.craftengine.proxy.minecraft.world.level.gameevent;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.gameevent.GameEvent")
public interface GameEventProxy {
    GameEventProxy INSTANCE = ASMProxyFactory.create(GameEventProxy.class);
    Object BLOCK_ACTIVATE = INSTANCE.getBlockActivate();
    Object BLOCK_DEACTIVATE = INSTANCE.getBlockDeactivate();

    @FieldGetter(name = "BLOCK_ACTIVATE", isStatic = true)
    Object getBlockActivate();

    @FieldGetter(name = "BLOCK_DEACTIVATE", isStatic = true)
    Object getBlockDeactivate();
}
