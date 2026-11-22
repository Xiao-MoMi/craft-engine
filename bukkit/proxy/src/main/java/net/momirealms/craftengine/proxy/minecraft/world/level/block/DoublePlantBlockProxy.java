package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.block.DoublePlantBlock")
public interface DoublePlantBlockProxy {
    DoublePlantBlockProxy INSTANCE = ASMProxyFactory.create(DoublePlantBlockProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.block.DoublePlantBlock");

    @MethodInvoker(name = "placeAt", isStatic = true)
    void placeAt(@Type(clazz = LevelAccessorProxy.class) Object level, @Type(clazz = BlockStateProxy.class) Object state,
                 @Type(clazz = BlockPosProxy.class) Object pos, int flags);
}
