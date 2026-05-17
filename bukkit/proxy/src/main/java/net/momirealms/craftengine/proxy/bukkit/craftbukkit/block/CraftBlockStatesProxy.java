package net.momirealms.craftengine.proxy.bukkit.craftbukkit.block;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "org.bukkit.craftbukkit.block.CraftBlockStates")
public interface CraftBlockStatesProxy {
    CraftBlockStatesProxy INSTANCE = ASMProxyFactory.create(CraftBlockStatesProxy.class);

    @MethodInvoker(name = "getBlockState", isStatic = true)
    Object getBlockState(@Type(clazz = LevelAccessorProxy.class) Object level,
                         @Type(clazz = BlockPosProxy.class) Object pos);
}
