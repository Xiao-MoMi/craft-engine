package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.context.BlockPlaceContextProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.world.level.block.Block")
public interface BlockProxy extends BlockBehaviourProxy {
    BlockProxy INSTANCE = ASMProxyFactory.create(BlockProxy.class);
    Object BLOCK_STATE_REGISTRY = INSTANCE.getBlockStateRegistry();

    @FieldGetter(name = "BLOCK_STATE_REGISTRY", isStatic = true)
    Object getBlockStateRegistry();

    @FieldGetter(name = "stateDefinition")
    Object getStateDefinition(Object target);

    @FieldSetter(name = "stateDefinition")
    void setStateDefinition(Object target, Object value);

    @FieldSetter(name = "descriptionId", activeIf = "max_version=1.21.1")
    void setDescriptionId(Object target, String descriptionId);

    @FieldGetter(name = "defaultBlockState")
    Object getDefaultBlockState(Object target);

    @FieldSetter(name = "defaultBlockState")
    void setDefaultBlockState(Object target, Object defaultBlockState);

    @MethodInvoker(name = "getStateForPlacement")
    Object getStateForPlacement(Object target, @Type(clazz = BlockPlaceContextProxy.class) Object ctx);

    @MethodInvoker(name = "asItem")
    Object asItem(Object target);

    @MethodInvoker(name = "dropResources", isStatic = true)
    void dropResources(@Type(clazz = BlockStateProxy.class) Object state, @Type(clazz = LevelProxy.class) Object level, @Type(clazz = BlockPosProxy.class) Object pos);
}
