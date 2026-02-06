package net.momirealms.craftengine.proxy.minecraft.world.item.equipment.trim;

import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = {"net.minecraft.world.item.equipment.trim.TrimPattern", "net.minecraft.world.item.armortrim.TrimPattern"})
public interface TrimPatternProxy {
    TrimPatternProxy INSTANCE = ASMProxyFactory.create(TrimPatternProxy.class);

    @ConstructorInvoker(activeIf = "min_version=1.21.5")
    Object newInstance(@Type(clazz = IdentifierProxy.class) Object assetId,
                       @Type(clazz = ComponentProxy.class) Object description,
                       boolean decal);

    @ConstructorInvoker(activeIf = "min_version=1.20.2 && max_version=1.21.4")
    Object newInstance(@Type(clazz = IdentifierProxy.class) Object assetId,
                       @Type(clazz = HolderProxy.class) Object templateItem,
                       @Type(clazz = ComponentProxy.class) Object description,
                       boolean decal);

    @ConstructorInvoker(activeIf = "max_version=1.20.1")
    Object newInstance(@Type(clazz = IdentifierProxy.class) Object assetId,
                       @Type(clazz = HolderProxy.class) Object templateItem,
                       @Type(clazz = ComponentProxy.class) Object description);
}
