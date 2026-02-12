package net.momirealms.craftengine.proxy.bukkit.craftbukkit.block.data;

import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.bukkit.block.data.BlockData;

@ReflectionProxy(name = "org.bukkit.craftbukkit.block.data.CraftBlockData")
public interface CraftBlockDataProxy {
    CraftBlockDataProxy INSTANCE = ASMProxyFactory.create(CraftBlockDataProxy.class);

    @MethodInvoker(name = "getState")
    Object getState(BlockData target);

    @MethodInvoker(name = "fromData", isStatic = true)
    BlockData fromData(@Type(clazz = BlockStateProxy.class) Object data);
}
