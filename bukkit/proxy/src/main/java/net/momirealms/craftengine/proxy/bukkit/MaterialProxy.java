package net.momirealms.craftengine.proxy.bukkit;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.UnsafeConstructor;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.function.Supplier;

@ReflectionProxy(clazz = Material.class)
public interface MaterialProxy {
    MaterialProxy INSTANCE = ASMProxyFactory.create(MaterialProxy.class);
    Map<String, Material> BY_NAME = INSTANCE.getByName();
    UnsafeConstructor UNSAFE_CONSTRUCTOR = new UnsafeConstructor(Material.class);
    Class<?> clazz$MaterialData = SparrowClass.find("org.bukkit.material.MaterialData");
    Constructor<?> constructor$MaterialData = SparrowClass.of(clazz$MaterialData).getConstructor(ConstructorMatcher.takeArguments(Material.class, byte.class));

    @FieldSetter(name = "$VALUES", isStatic = true)
    void setValues(Material[] values);

    @FieldGetter(name = "BY_NAME", isStatic = true)
    Map<String, Material> getByName();

    @FieldSetter(name = "id")
    void setId(Object material, int id);

    @FieldSetter(name = "ctor")
    void setCtor(Object material, Constructor<?> ctor);

    @FieldSetter(name = "data")
    void setData(Object material, Class<?> data);

    @FieldSetter(name = "legacy")
    void setLegacy(Object material, boolean legacy);

    @FieldSetter(name = "key")
    void setKey(Object material, NamespacedKey key);

    @FieldSetter(name = "itemType", activeIf = "min_version=1.21")
    default void setItemType(Object material, Supplier<?> itemType) {
    }

    @FieldSetter(name = "blockType", activeIf = "min_version=1.21")
    default void setBlockType(Object material, Supplier<?> blockType) {
    }

    @FieldSetter(name = "isBlock", activeIf = "max_version=1.20.4")
    default void setIsBlock(Object material, boolean isBlock) {
    }

    @FieldSetter(name = "maxStack", activeIf = "max_version=1.21.10")
    default void setMaxStack(Object material, int maxStack) {
    }

    @FieldSetter(name = "durability", activeIf = "max_version=1.21.4")
    default void setDurability(Object material, short durability) {
    }
}
