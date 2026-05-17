package net.momirealms.craftengine.proxy.minecraft.world.item.equipment.trim;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.equipment.trim.MaterialAssetGroup", activeIf = "min_version=1.21.5")
public interface MaterialAssetGroupProxy {
    MaterialAssetGroupProxy INSTANCE = ASMProxyFactory.create(MaterialAssetGroupProxy.class);

    @MethodInvoker(name = "create", isStatic = true)
    Object create(String name);
}
