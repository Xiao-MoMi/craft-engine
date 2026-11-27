package net.momirealms.craftengine.proxy.minecraft.world.level.block.entity;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.persistence.PersistentDataContainer;

@ReflectionProxy(name = "net.minecraft.world.level.block.entity.BlockEntity")
public interface BlockEntityProxy {
    BlockEntityProxy INSTANCE = ASMProxyFactory.create(BlockEntityProxy.class);

    @FieldGetter(name = "persistentDataContainer")
    PersistentDataContainer getPersistentDataContainer(Object blockEntity);
}
