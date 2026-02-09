package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.redstone.OrientationProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.shape.CollisionContextProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.Level")
public interface LevelProxy {
    LevelProxy INSTANCE = ASMProxyFactory.create(LevelProxy.class);

    @FieldGetter(name = "dimensionTypeRegistration")
    Object getDimensionTypeRegistration(Object target);

    @FieldGetter(name = "dimension")
    Object getDimension(Object target);

    @FieldGetter(name = "random")
    Object getRandom(Object target);

    @MethodInvoker(name = "checkEntityCollision")
    boolean checkEntityCollision(
            Object target,
            @Type(clazz = BlockStateProxy.class) Object state,
            @Type(clazz = EntityProxy.class) Object source,
            @Type(clazz = CollisionContextProxy.class) Object context,
            @Type(clazz = BlockPosProxy.class) Object pos,
            boolean checkCanSee
    );

    @MethodInvoker(name = "updateNeighborsAt", activeIf = "min_version=1.21.2")
    void updateNeighborsAt(Object target, @Type(clazz = BlockPosProxy.class) Object pos, @Type(clazz = BlockProxy.class) Object sourceBlock, @Type(clazz = OrientationProxy.class) Object orientation);

    @MethodInvoker(name = "updateNeighborsAt", activeIf = "max_version=1.21.1")
    void updateNeighborsAt(Object target, @Type(clazz = BlockPosProxy.class) Object pos, @Type(clazz = BlockProxy.class) Object sourceBlock);
}
