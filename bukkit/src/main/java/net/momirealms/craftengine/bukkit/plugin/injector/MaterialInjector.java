package net.momirealms.craftengine.bukkit.plugin.injector;

import com.google.common.base.Suppliers;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.block.DelegatingBlock;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.bukkit.MaterialProxy;
import net.momirealms.sparrow.reflection.SReflection;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class MaterialInjector {
    private static final Map<DelegatingBlock, Material> BY_BLOCK = new HashMap<>();
    private static final MethodHandle Enum$name;
    private static final MethodHandle Enum$ordinal;
    private static final MethodHandle Class$enumConstantDirectory;
    private static final MethodHandle Class$enumConstants;

    static {
        try {
            Enum$name = SReflection.unreflectSetter(Enum.class.getDeclaredField("name"));
            Enum$ordinal = SReflection.unreflectSetter(Enum.class.getDeclaredField("ordinal"));
            Class$enumConstantDirectory = SReflection.unreflectSetter(Class.class.getDeclaredField("enumConstantDirectory"));
            Class$enumConstants = SReflection.unreflectSetter(Class.class.getDeclaredField("enumConstants"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private MaterialInjector() {}

    public static Material createMaterial(Key id, int ordinal, DelegatingBlock block) {
        Material material = (Material) MaterialProxy.UNSAFE_CONSTRUCTOR.newInstance();
        MaterialProxy.INSTANCE.setId(material, -1);
        MaterialProxy.INSTANCE.setCtor(material, MaterialProxy.constructor$MaterialData);
        MaterialProxy.INSTANCE.setData(material, MaterialProxy.clazz$MaterialData);
        MaterialProxy.INSTANCE.setLegacy(material, false);
        NamespacedKey key = KeyUtils.toNamespacedKey(id);
        MaterialProxy.INSTANCE.setKey(material, key);
        if (VersionHelper.isOrAbove1_21()) {
            MaterialProxy.INSTANCE.setItemType(material, () -> null);
            MaterialProxy.INSTANCE.setBlockType(material, Suppliers.memoize(() -> Registry.BLOCK.get(key)));
        }
        MaterialProxy.INSTANCE.setIsBlock(material, true);
        MaterialProxy.INSTANCE.setMaxStack(material, 64);
        MaterialProxy.INSTANCE.setDurability(material, (short) 0);
        try {
            Enum$name.invoke(material, (id.namespace() + "_" + id.value()).toUpperCase(Locale.ROOT));
            Enum$ordinal.invoke(material, ordinal);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        BY_BLOCK.put(block, material);
        return material;
    }

    public static void resetMaterial(Material[] newValues) {
        MaterialProxy.INSTANCE.setValues(newValues);
        MaterialProxy.BY_NAME.clear();
        for (Material material : newValues) {
            MaterialProxy.BY_NAME.put(material.name(), material);
        }
        try {
            Class$enumConstantDirectory.invoke(Material.class, null);
            Class$enumConstants.invoke(Material.class, null);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Material getByBlock(DelegatingBlock block) {
        return Objects.requireNonNull(BY_BLOCK.get(block), "block not found");
    }
}
