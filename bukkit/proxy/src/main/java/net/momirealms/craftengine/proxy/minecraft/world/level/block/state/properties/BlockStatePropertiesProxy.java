package net.momirealms.craftengine.proxy.minecraft.world.level.block.state.properties;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.state.properties.BlockStateProperties")
public interface BlockStatePropertiesProxy {
    BlockStatePropertiesProxy INSTANCE = ASMProxyFactory.create(BlockStatePropertiesProxy.class);
    Object WATERLOGGED = INSTANCE.getWaterloggedProperty();
    Object FACING = INSTANCE.getFacingProperty();

    @FieldGetter(name = "WATERLOGGED", isStatic = true)
    Object getWaterloggedProperty();

    @FieldGetter(name = "FACING", isStatic = true)
    Object getFacingProperty();
}
