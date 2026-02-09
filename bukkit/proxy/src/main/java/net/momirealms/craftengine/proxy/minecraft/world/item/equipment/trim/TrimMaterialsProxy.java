package net.momirealms.craftengine.proxy.minecraft.world.item.equipment.trim;

import net.momirealms.craftengine.proxy.minecraft.core.HolderLookupProxy;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryAccessProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Optional;

@ReflectionProxy(name = {"net.minecraft.world.item.equipment.trim.TrimMaterials", "net.minecraft.world.item.armortrim.TrimMaterials"})
public interface TrimMaterialsProxy {
    TrimMaterialsProxy INSTANCE = ASMProxyFactory.create(TrimMaterialsProxy.class);

    @MethodInvoker(name = "getFromIngredient", activeIf = "min_version=1.20.5")
    Optional<Object> getFromIngredient$0(@Type(clazz = HolderLookupProxy.ProviderProxy.class) Object registriesLookup, @Type(clazz = ItemStackProxy.class) Object stack);

    @MethodInvoker(name = "getFromIngredient", activeIf = "max_version=1.20.4")
    Optional<Object> getFromIngredient$1(@Type(clazz = RegistryAccessProxy.class) Object registriesLookup, @Type(clazz = ItemStackProxy.class) Object stack);
}
