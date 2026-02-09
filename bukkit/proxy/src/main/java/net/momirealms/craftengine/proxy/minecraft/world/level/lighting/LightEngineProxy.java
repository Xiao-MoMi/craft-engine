package net.momirealms.craftengine.proxy.minecraft.world.level.lighting;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.lighting.LightEngine")
public interface LightEngineProxy {
    LightEngineProxy INSTANCE = ASMProxyFactory.create(LightEngineProxy.class);

    @MethodInvoker(name = "getLightBlockInto", isStatic = true, activeIf = "min_version=1.21.2")
    int getLightBlockInto(
            @Type(clazz = BlockStateProxy.class) Object state1,
            @Type(clazz = BlockStateProxy.class) Object state2,
            @Type(clazz = DirectionProxy.class) Object direction,
            int defaultReturnValue
    );

    @MethodInvoker(name = "getLightBlockInto", isStatic = true, activeIf = "max_version=1.21.1")
    int getLightBlockInto(
            @Type(clazz = BlockGetterProxy.class) Object world,
            @Type(clazz = BlockStateProxy.class) Object state1,
            @Type(clazz = BlockPosProxy.class) Object pos1,
            @Type(clazz = BlockStateProxy.class) Object state2,
            @Type(clazz = BlockPosProxy.class) Object pos2,
            @Type(clazz = DirectionProxy.class) Object direction,
            int defaultReturnValue
    );
}
