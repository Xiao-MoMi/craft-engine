package net.momirealms.craftengine.bukkit.reflection.craftbukkit.block;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.block.TileState;

@ReflectionProxy(name = "org.bukkit.craftbukkit.block.CraftBlockEntityState")
public interface CraftBlockEntityStateProxy {
    CraftBlockEntityStateProxy INSTANCE = ReflectionHelper.getProxy(CraftBlockEntityStateProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftBlockEntityStateProxy.class);

    @FieldGetter(names = {"blockEntity", "tileEntity"})
    Object blockEntity(TileState instance);

    @FieldSetter(names = {"blockEntity", "tileEntity"}, strategy = Strategy.MH)
    void blockEntity(TileState instance, Object blockEntity);
}
