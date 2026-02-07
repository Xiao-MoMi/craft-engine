package net.momirealms.craftengine.proxy.minecraft.world.item;

import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.ClipContextProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.item.Item")
public interface ItemProxy {
    ItemProxy INSTANCE = ASMProxyFactory.create(ItemProxy.class);

    @MethodInvoker(name = "getPlayerPOVHitResult", isStatic = true)
    Object getPlayerPOVHitResult(@Type(clazz = LevelProxy.class) Object level,
                                 @Type(clazz = PlayerProxy.class) Object player,
                                 @Type(clazz = ClipContextProxy.FluidProxy.class) Object fluidMode);
}
