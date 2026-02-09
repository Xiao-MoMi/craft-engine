package net.momirealms.craftengine.proxy.minecraft.world.item;

import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.ClipContextProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.world.item.Item")
public interface ItemProxy {
    ItemProxy INSTANCE = ASMProxyFactory.create(ItemProxy.class);

    @FieldGetter(name = "builtInRegistryHolder")
    Object getBuiltInRegistryHolder(Object target);

    @FieldSetter(name = "builtInRegistryHolder")
    void setBuiltInRegistryHolder(Object target, Object value);

    @MethodInvoker(name = "getPlayerPOVHitResult", isStatic = true)
    Object getPlayerPOVHitResult(
            @Type(clazz = LevelProxy.class) Object level,
            @Type(clazz = PlayerProxy.class) Object player,
            @Type(clazz = ClipContextProxy.FluidProxy.class) Object fluidMode
    );
}
