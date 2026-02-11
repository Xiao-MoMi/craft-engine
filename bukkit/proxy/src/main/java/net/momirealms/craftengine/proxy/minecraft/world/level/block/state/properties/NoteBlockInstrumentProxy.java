package net.momirealms.craftengine.proxy.minecraft.world.level.block.state.properties;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.state.properties.NoteBlockInstrument")
public interface NoteBlockInstrumentProxy {
    NoteBlockInstrumentProxy INSTANCE = ASMProxyFactory.create(NoteBlockInstrumentProxy.class);
    Enum<?>[] VALUES = INSTANCE.values();

    @MethodInvoker(name = "values", isStatic = true)
    Enum<?>[] values();
}
