package net.momirealms.craftengine.bukkit.reflection.craftbukkit.block;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.craftengine.bukkit.reflection.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.bukkit.reflection.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.craftengine.bukkit.reflection.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "org.bukkit.craftbukkit.block.CraftBlock")
public interface CraftBlockProxy {
    CraftBlockProxy INSTANCE = ReflectionHelper.getProxy(CraftBlockProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftBlockProxy.class);

    @MethodInvoker(names = {"setBlockState", "setTypeAndData"}, isStatic = true)
    boolean setBlockState(
            @Type(clazz = LevelAccessorProxy.class) Object world,
            @Type(clazz = BlockPosProxy.class) Object position,
            @Type(clazz = BlockStateProxy.class) Object old,
            @Type(clazz = BlockStateProxy.class) Object blockData,
            boolean applyPhysics
    );
}
