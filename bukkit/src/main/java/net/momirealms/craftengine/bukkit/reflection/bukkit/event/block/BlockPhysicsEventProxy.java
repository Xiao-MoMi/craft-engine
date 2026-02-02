package net.momirealms.craftengine.bukkit.reflection.bukkit.event.block;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.block.BlockPhysicsEvent;

@ReflectionProxy(clazz = BlockPhysicsEvent.class)
public interface BlockPhysicsEventProxy {
    BlockPhysicsEventProxy INSTANCE = ReflectionHelper.getProxy(BlockPhysicsEventProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(BlockPhysicsEventProxy.class);

    @FieldGetter(name = "changed")
    BlockData changed(BlockPhysicsEvent instance);

    @FieldSetter(name = "changed", strategy = Strategy.MH)
    void changed(BlockPhysicsEvent instance, BlockData changed);
}
