package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.shape.CollisionContextProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.Level")
public interface LevelProxy {
    LevelProxy INSTANCE = ASMProxyFactory.create(LevelProxy.class);

    @MethodInvoker(name = "checkEntityCollision")
    boolean checkEntityCollision(Object target,
                                 @Type(clazz = BlockStateProxy.class) Object state,
                                 @Type(clazz = EntityProxy.class) Object source,
                                 @Type(clazz = CollisionContextProxy.class) Object context,
                                 @Type(clazz = BlockPosProxy.class) Object pos,
                                 boolean checkCanSee);
}
