package net.momirealms.craftengine.bukkit.reflection.minecraft.core;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.BlockPos")
public interface BlockPosProxy extends Vec3iProxy {
    BlockPosProxy INSTANCE = ReflectionHelper.getProxy(BlockPosProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(BlockPosProxy.class);

    @ConstructorInvoker
    Object newInstance(int x, int y, int z);
}
