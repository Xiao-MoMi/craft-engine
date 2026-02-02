package net.momirealms.craftengine.bukkit.reflection.craftbukkit.block;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.craftengine.bukkit.reflection.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.bukkit.reflection.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.*;
import org.bukkit.Material;
import org.bukkit.block.BlockState;

import java.util.Map;

@ReflectionProxy(name = "org.bukkit.craftbukkit.block.CraftBlockStates")
public interface CraftBlockStatesProxy {
    CraftBlockStatesProxy INSTANCE = ReflectionHelper.getProxy(CraftBlockStatesProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftBlockStatesProxy.class);

    @MethodInvoker(name = "getBlockState", isStatic = true)
    BlockState getBlockState(@Type(clazz = LevelAccessorProxy.class) Object world, @Type(clazz = BlockPosProxy.class) Object pos);

    @FieldGetter(name = "FACTORIES", isStatic = true)
    Map<Material, ?> FACTORIES();

    @FieldSetter(name = "FACTORIES", isStatic = true, strategy = Strategy.MH)
    void FACTORIES(Map<Material, ?> FACTORIES);

    @ReflectionProxy(name = "org.bukkit.craftbukkit.block.CraftBlockStates$BlockEntityStateFactory")
    interface BlockEntityStateFactoryProxy {
        BlockEntityStateFactoryProxy INSTANCE = ReflectionHelper.getProxy(BlockEntityStateFactoryProxy.class);
        Class<?> CLAZZ = ReflectionHelper.getClass(BlockEntityStateFactoryProxy.class);
    }
}
