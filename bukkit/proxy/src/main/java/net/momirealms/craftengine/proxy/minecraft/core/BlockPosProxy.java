package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.core.BlockPos")
public interface BlockPosProxy extends Vec3iProxy {
    BlockPosProxy INSTANCE = ASMProxyFactory.create(BlockPosProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.core.BlockPos");
    Object ZERO = INSTANCE.newInstance(0, 0, 0);

    @ConstructorInvoker
    Object newInstance(int x, int y, int z);

    @MethodInvoker(name = "mutable")
    Object mutable(Object target);

    @MethodInvoker(name = "offset")
    Object offset(Object target, int x, int y, int z);

    @MethodInvoker(name = "relative")
    Object relative(Object target, @Type(clazz = DirectionProxy.class) Object direction);

    @ReflectionProxy(name = "net.minecraft.core.BlockPos$MutableBlockPos")
    interface MutableBlockPosProxy extends BlockPosProxy {
        MutableBlockPosProxy INSTANCE = ASMProxyFactory.create(MutableBlockPosProxy.class);

        @ConstructorInvoker
        Object newInstance();

        @ConstructorInvoker
        Object newInstance(int x, int y, int z);

        @MethodInvoker(name = "setWithOffset")
        Object setWithOffset(Object target, @Type(clazz = Vec3iProxy.class) Object pos, @Type(clazz = DirectionProxy.class) Object direction);
    }
}
