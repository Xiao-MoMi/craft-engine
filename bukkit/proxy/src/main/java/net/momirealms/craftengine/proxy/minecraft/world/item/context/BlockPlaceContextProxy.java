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
import org.jspecify.annotations.Nullable;

@ReflectionProxy(name = "net.minecraft.world.item.context.BlockPlaceContext")
public interface BlockPlaceContextProxy {
    BlockPlaceContextProxy INSTANCE = ASMProxyFactory.create(BlockPlaceContextProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.context.BlockPlaceContext");

    @ConstructorInvoker
    Object newInstance(
            @Type(clazz = LevelProxy.class) Object level,
            @Type(clazz = PlayerProxy.class) @Nullable Object player,
            @Type(clazz = InteractionHandProxy.class) Object hand,
            @Type(clazz = ItemStackProxy.class) Object itemStack,
            @Type(clazz = BlockHitResultProxy.class) Object hitResult
    );

    @MethodInvoker(name = "getNearestLookingDirection")
    Enum<?> getNearestLookingDirection(Object target);

    @MethodInvoker(name = "getNearestLookingVerticalDirection")
    Enum<?> getNearestLookingVerticalDirection(Object target);

    @MethodInvoker(name = "getNearestLookingDirections")
    Enum<?>[] getNearestLookingDirections(Object target);
}
