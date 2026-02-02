package net.momirealms.craftengine.bukkit.reflection.craftbukkit.util;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.Material;

import java.util.Map;

@ReflectionProxy(name = "org.bukkit.craftbukkit.util.CraftMagicNumbers")
public interface CraftMagicNumbersProxy {
    CraftMagicNumbersProxy INSTANCE = ReflectionHelper.getProxy(CraftMagicNumbersProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftMagicNumbersProxy.class);

    @FieldGetter(name = "BLOCK_MATERIAL", isStatic = true)
    Map<?, Material> BLOCK_MATERIAL();

    @FieldSetter(name = "BLOCK_MATERIAL", isStatic = true, strategy = Strategy.MH)
    void BLOCK_MATERIAL(Map<?, Material> BLOCK_MATERIAL);
}
