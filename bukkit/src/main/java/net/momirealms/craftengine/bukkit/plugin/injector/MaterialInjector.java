package net.momirealms.craftengine.bukkit.plugin.injector;

import com.google.common.base.Suppliers;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.block.DelegatingBlock;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.bukkit.MaterialProxy;
import net.momirealms.sparrow.reflection.field.SField;
import net.momirealms.sparrow.reflection.field.SparrowField;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class MaterialInjector {
    private static final Map<DelegatingBlock, Material> BY_BLOCK = new HashMap<>();
    private static final SField Enum$name;
    private static final SField Enum$ordinal;
    private static final SField Class$enumConstantDirectory;
    private static final SField Class$enumConstants;

    static {
        try {
            Enum$name = SparrowField.of(Enum.class.getDeclaredField("name")).mh();
            Enum$ordinal = SparrowField.of(Enum.class.getDeclaredField("ordinal")).mh();
            Class$enumConstantDirectory = SparrowField.of(Class.class.getDeclaredField("enumConstantDirectory")).mh();
            Class$enumConstants = SparrowField.of(Class.class.getDeclaredField("enumConstants")).mh();
        } catch (Exception e) {
            throw new InjectionException("Failed to access required fields", e);
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
        if (VersionHelper.isOrAbove1_21) {
            MaterialProxy.INSTANCE.setItemType(material, () -> null);
            MaterialProxy.INSTANCE.setBlockType(material, Suppliers.memoize(() -> Registry.BLOCK.get(key)));
        }
        MaterialProxy.INSTANCE.setIsBlock(material, true);
        MaterialProxy.INSTANCE.setMaxStack(material, 64);
        MaterialProxy.INSTANCE.setDurability(material, (short) 0);
        Enum$name.set(material, (id.namespace() + "_" + id.value()).toUpperCase(Locale.ROOT));
        Enum$ordinal.set(material, ordinal);
        BY_BLOCK.put(block, material);
        return material;
    }

    public static void resetMaterial(Material[] newValues) {
        MaterialProxy.INSTANCE.setValues(newValues);
        MaterialProxy.BY_NAME.clear();
        for (Material material : newValues) {
            MaterialProxy.BY_NAME.put(material.name(), material);
        }
        Class$enumConstantDirectory.set(Material.class, null);
        Class$enumConstants.set(Material.class, null);
    }

    public static Material getByBlock(DelegatingBlock block) {
        return Objects.requireNonNull(BY_BLOCK.get(block), "block not found");
    }

    public static void check() throws Throwable {
        Material material = (Material) MaterialProxy.UNSAFE_CONSTRUCTOR.newInstance();
        MaterialProxy.INSTANCE.setId(material, -1);
        Enum$name.set(material, "CHECK");
        Class$enumConstantDirectory.get(Material.class);
        Class$enumConstants.get(Material.class);
    }
}
