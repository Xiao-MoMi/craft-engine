package net.momirealms.craftengine.proxy.minecraft.world.item.equipment.trim;

import net.momirealms.craftengine.proxy.minecraft.core.HolderLookupProxy;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryAccessProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Optional;

@ReflectionProxy(name = {"net.minecraft.world.item.equipment.trim.TrimPatterns", "net.minecraft.world.item.armortrim.TrimPatterns"})
public interface TrimPatternsProxy {
    TrimPatternsProxy INSTANCE = ASMProxyFactory.create(TrimPatternsProxy.class);

    @MethodInvoker(name = "getFromTemplate", isStatic = true, activeIf = "max_version=1.20.4")
    Optional<Object> getFromTemplate$0(@Type(clazz = RegistryAccessProxy.class) Object registriesLookup, @Type(clazz = ItemStackProxy.class) Object stack);

    @MethodInvoker(name = "getFromTemplate", isStatic = true, activeIf = "min_version=1.20.5 && max_version=1.21.4")
    Optional<Object> getFromTemplate$1(@Type(clazz = HolderLookupProxy.ProviderProxy.class) Object registriesLookup, @Type(clazz = ItemStackProxy.class) Object stack);
}
