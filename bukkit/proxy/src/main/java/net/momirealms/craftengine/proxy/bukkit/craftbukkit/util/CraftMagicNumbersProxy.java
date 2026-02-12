package net.momirealms.craftengine.proxy.bukkit.craftbukkit.util;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.Material;

import java.util.Map;

@ReflectionProxy(name = "org.bukkit.craftbukkit.util.CraftMagicNumbers")
public interface CraftMagicNumbersProxy {
    CraftMagicNumbersProxy INSTANCE = ASMProxyFactory.create(CraftMagicNumbersProxy.class);
    Map<Object, Material> BLOCK_MATERIAL = INSTANCE.getBlockMaterial();

    @FieldGetter(name = "BLOCK_MATERIAL", isStatic = true)
    Map<Object, Material> getBlockMaterial();
}
