package net.momirealms.craftengine.proxy.minecraft.world.item;

import net.momirealms.craftengine.proxy.minecraft.core.HolderLookupProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Optional;

@ReflectionProxy(name = "net.minecraft.world.item.InstrumentItem", activeIf = "min_version=1.21.2 && max_version=1.21.11")
public interface InstrumentItemProxy {
    InstrumentItemProxy INSTANCE = ASMProxyFactory.create(InstrumentItemProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.InstrumentItem");

    @MethodInvoker(name = "getInstrument")
    Optional<?> getInstrument(Object target,
                              @Type(clazz = ItemStackProxy.class) Object stack,
                              @Type(clazz = HolderLookupProxy.ProviderProxy.class) Object registries);
}
