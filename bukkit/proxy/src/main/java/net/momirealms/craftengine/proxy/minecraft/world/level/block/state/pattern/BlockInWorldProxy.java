package net.momirealms.craftengine.proxy.minecraft.world.level.block.state.pattern;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelReaderProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.world.level.block.state.pattern.BlockInWorld")
public interface BlockInWorldProxy {
    BlockInWorldProxy INSTANCE = ASMProxyFactory.create(BlockInWorldProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = LevelReaderProxy.class) Object level, @Type(clazz = BlockPosProxy.class) Object pos, boolean loadChunks);

    @FieldGetter(name = "state")
    Object getState(Object target);

    @FieldSetter(name = "state")
    void setState(Object target, Object value);
}
