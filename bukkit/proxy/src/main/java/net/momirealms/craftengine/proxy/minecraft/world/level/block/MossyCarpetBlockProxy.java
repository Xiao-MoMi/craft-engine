package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.util.RandomSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.block.MossyCarpetBlock", activeIf = "min_version=1.21.2")
public interface MossyCarpetBlockProxy {
    MossyCarpetBlockProxy INSTANCE = ASMProxyFactory.create(MossyCarpetBlockProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.block.MossyCarpetBlock");

    @MethodInvoker(name = "placeAt", isStatic = true)
    void placeAt(@Type(clazz = LevelAccessorProxy.class) Object level, @Type(clazz = BlockPosProxy.class) Object pos,
                 @Type(clazz = RandomSourceProxy.class) Object random, int flags);
}
