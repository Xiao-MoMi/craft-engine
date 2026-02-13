package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.Nullable;

@ReflectionProxy(name = "net.minecraft.world.level.LevelWriter")
public interface LevelWriterProxy {
    LevelWriterProxy INSTANCE = ASMProxyFactory.create(LevelWriterProxy.class);

    @MethodInvoker(name = "destroyBlock")
    boolean destroyBlock(Object target, @Type(clazz = BlockPosProxy.class) Object pos, boolean drop);

    @MethodInvoker(name = "setBlock")
    boolean setBlock(Object target, @Type(clazz = BlockPosProxy.class) Object pos, @Type(clazz = BlockStateProxy.class) Object newState, int flags);

    @MethodInvoker(name = "addFreshEntity")
    boolean addFreshEntity(Object target, @Type(clazz = EntityProxy.class) Object entity, @Nullable CreatureSpawnEvent.SpawnReason reason);
}
