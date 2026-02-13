package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.redstone.OrientationProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.shapes.CollisionContextProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.bukkit.World;

@ReflectionProxy(name = "net.minecraft.world.level.Level")
public interface LevelProxy extends LevelAccessorProxy {
    LevelProxy INSTANCE = ASMProxyFactory.create(LevelProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.Level");

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

    @MethodInvoker(name = "updateNeighborsAt", activeIf = "max_version=1.21.4") // 1.21.5+ 在 LevelAccessorProxy
    void updateNeighborsAt(Object target, @Type(clazz = BlockPosProxy.class) Object pos, @Type(clazz = BlockProxy.class) Object sourceBlock);

    @MethodInvoker(name = "updateNeighbourForOutputSignal")
    void updateNeighbourForOutputSignal(Object target, @Type(clazz = BlockPosProxy.class) Object pos, @Type(clazz = BlockProxy.class) Object block);

    @MethodInvoker(name = "setBlocksDirty")
    void setBlocksDirty(Object target, @Type(clazz = BlockPosProxy.class) Object pos, @Type(clazz = BlockStateProxy.class) Object oldState, @Type(clazz = BlockStateProxy.class) Object newState);

    @MethodInvoker(name = "moonrise$getEntityLookup", activeIf = "min_version=1.21")
    Object moonrise$getEntityLookup(Object target);

    @MethodInvoker(name = "removeBlock")
    boolean removeBlock(Object target, @Type(clazz = BlockPosProxy.class) Object pos, boolean movedByPiston);

    @MethodInvoker(name = "getWorld")
    World getWorld(Object target);
}
