package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviorProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.Block")
public interface BlockProxy extends BlockBehaviorProxy {
    BlockProxy INSTANCE = ASMProxyFactory.create(BlockProxy.class);
    Object BLOCK_STATE_REGISTRY = INSTANCE.getBlockStateRegistry();

    @FieldGetter(name = "BLOCK_STATE_REGISTRY", isStatic = true)
    Object getBlockStateRegistry();

    @FieldGetter(name = "stateDefinition")
    Object getStateDefinition(Object target);

    @FieldSetter(name = "stateDefinition")
    void setStateDefinition(Object target, Object value);

    @FieldSetter(name = "setDescriptionId", activeIf = "max_version=1.21.1")
    void setDescriptionId(Object target, String descriptionId);

    @FieldGetter(name = "defaultBlockState")
    Object getDefaultBlockState(Object target);

    @FieldSetter(name = "defaultBlockState")
    void setDefaultBlockState(Object target, Object defaultBlockState);
}
