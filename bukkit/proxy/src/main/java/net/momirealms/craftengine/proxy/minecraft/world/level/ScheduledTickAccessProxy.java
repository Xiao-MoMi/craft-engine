package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidProxy;
import net.momirealms.craftengine.proxy.minecraft.world.ticks.TickPriorityProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.ScheduledTickAccess", activeIf = "min_version=1.21.2")
public interface ScheduledTickAccessProxy {
    ScheduledTickAccessProxy INSTANCE = ASMProxyFactory.create(ScheduledTickAccessProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.ScheduledTickAccess");

    @MethodInvoker(name = "scheduleTick")
    void scheduleTick$0(Object target, @Type(clazz = BlockPosProxy.class) Object pos, @Type(clazz = BlockProxy.class) Object block, int delay, @Type(clazz = TickPriorityProxy.class) Object priority);

    @MethodInvoker(name = "scheduleTick")
    void scheduleTick$0(Object target, @Type(clazz = BlockPosProxy.class) Object pos, @Type(clazz = BlockProxy.class) Object block, int delay);

    @MethodInvoker(name = "scheduleTick")
    void scheduleTick$1(Object target, @Type(clazz = BlockPosProxy.class) Object pos, @Type(clazz = FluidProxy.class) Object fluid, int delay, @Type(clazz = TickPriorityProxy.class) Object priority);

    @MethodInvoker(name = "scheduleTick")
    void scheduleTick$1(Object target, @Type(clazz = BlockPosProxy.class) Object pos, @Type(clazz = FluidProxy.class) Object fluid, int delay);
}
