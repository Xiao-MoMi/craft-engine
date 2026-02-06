package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.block.BaseFireBlock")
public interface BaseFireBlockProxy extends BlockProxy {
    BaseFireBlockProxy INSTANCE = ASMProxyFactory.create(BaseFireBlockProxy.class);

    @MethodInvoker(name = "canBePlacedAt", isStatic = true)
    boolean canBePlacedAt(@Type(clazz = LevelProxy.class) Object world,
                          @Type(clazz = BlockPosProxy.class) Object pos,
                          @Type(clazz = DirectionProxy.class) Object direction);
}
