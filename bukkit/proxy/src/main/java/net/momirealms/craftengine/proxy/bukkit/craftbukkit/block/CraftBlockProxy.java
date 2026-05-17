package net.momirealms.craftengine.proxy.bukkit.craftbukkit.block;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.bukkit.block.Block;

@ReflectionProxy(name = "org.bukkit.craftbukkit.block.CraftBlock")
public interface CraftBlockProxy {
    CraftBlockProxy INSTANCE = ASMProxyFactory.create(CraftBlockProxy.class);

    @MethodInvoker(name = "at", isStatic = true)
    Block at(@Type(clazz = LevelAccessorProxy.class) Object world, @Type(clazz = BlockPosProxy.class) Object position);
}
