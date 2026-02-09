package net.momirealms.craftengine.proxy.bukkit.craftbukkit.event;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerPlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.AbstractContainerMenuProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.bukkit.event.block.BlockRedstoneEvent;

@ReflectionProxy(name = "org.bukkit.craftbukkit.event.CraftEventFactory")
public interface CraftEventFactoryProxy {
    CraftEventFactoryProxy INSTANCE = ASMProxyFactory.create(CraftEventFactoryProxy.class);

    @MethodInvoker(name = "callInventoryOpenEvent", isStatic = true)
    Object callInventoryOpenEvent(
            @Type(clazz = ServerPlayerProxy.class) Object player,
            @Type(clazz = AbstractContainerMenuProxy.class) Object container
    );

    @MethodInvoker(name = "handleBlockGrowEvent", isStatic = true, activeIf = "min_version=1.21.5")
    boolean handleBlockGrowEvent(
            @Type(clazz = LevelProxy.class) Object level,
            @Type(clazz = BlockPosProxy.class) Object pos,
            @Type(clazz = BlockStateProxy.class) Object state,
            int flags
    );

    @MethodInvoker(name = "handleBlockGrowEvent", isStatic = true, activeIf = "max_version=1.21.4")
    boolean handleBlockGrowEvent(
            @Type(clazz = LevelProxy.class) Object level,
            @Type(clazz = BlockPosProxy.class) Object pos,
            @Type(clazz = BlockStateProxy.class) Object state
    );

    @MethodInvoker(name = "handleBlockFormEvent", isStatic = true)
    boolean handleBlockFormEvent(
            @Type(clazz = LevelProxy.class) Object level,
            @Type(clazz = BlockPosProxy.class) Object pos,
            @Type(clazz = BlockStateProxy.class) Object state,
            int flags
    );

    @MethodInvoker(name = "callRedstoneChange", isStatic = true)
    BlockRedstoneEvent callRedstoneChange(
            @Type(clazz = LevelAccessorProxy.class) Object level,
            @Type(clazz = BlockPosProxy.class) Object pos,
            int oldCurrent,
            int newCurrent
    );
}
