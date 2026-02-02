package net.momirealms.craftengine.bukkit.reflection.craftbukkit.block.data;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.craftengine.bukkit.reflection.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.*;
import org.bukkit.block.data.BlockData;

@ReflectionProxy(name = "org.bukkit.craftbukkit.block.data.CraftBlockData")
public interface CraftBlockDataProxy {
    CraftBlockDataProxy INSTANCE = ReflectionHelper.getProxy(CraftBlockDataProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftBlockDataProxy.class);

    @FieldGetter(name = "state")
    Object state(BlockData instance);

    @FieldSetter(name = "state")
    void state(BlockData instance, Object state);

    @MethodInvoker(name = "createData", isStatic = true)
    BlockData createData(@Type(clazz = BlockStateProxy.class) Object data);

    @MethodInvoker(name = "fromData", isStatic = true)
    BlockData fromData(@Type(clazz = BlockStateProxy.class) Object data);
}
