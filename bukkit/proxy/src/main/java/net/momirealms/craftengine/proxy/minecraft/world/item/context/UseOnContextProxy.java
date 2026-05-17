package net.momirealms.craftengine.proxy.minecraft.world.item.context;

import net.momirealms.craftengine.proxy.minecraft.world.InteractionHandProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.BlockHitResultProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.item.context.UseOnContext")
public interface UseOnContextProxy {
    UseOnContextProxy INSTANCE = ASMProxyFactory.create(UseOnContextProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.context.UseOnContext");

    @MethodInvoker(name = "getHitResult")
    Object getHitResult(Object target);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = PlayerProxy.class) Object player,
                       @Type(clazz = InteractionHandProxy.class) Object hand,
                       @Type(clazz = BlockHitResultProxy.class) Object hitResult);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = LevelProxy.class) Object level,
                       @Type(clazz = PlayerProxy.class) Object player,
                       @Type(clazz = InteractionHandProxy.class) Object hand,
                       @Type(clazz = ItemStackProxy.class) Object itemStack,
                       @Type(clazz = BlockHitResultProxy.class) Object hitResult);
}
