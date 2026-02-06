package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import com.google.common.hash.HashCode;
import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.ReflectionInitException;
import net.momirealms.craftengine.bukkit.util.BukkitReflectionUtils;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.resources.RegistryOpsProxy;
import net.momirealms.craftengine.proxy.minecraft.util.HashOpsProxy;
import net.momirealms.sparrow.nbt.Tag;
import net.momirealms.sparrow.nbt.codec.LegacyJavaOps;
import net.momirealms.sparrow.nbt.codec.LegacyNBTOps;
import net.momirealms.sparrow.nbt.codec.NBTOps;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("unchecked")
public final class MRegistryOps {
    private MRegistryOps() {}

    public static final DynamicOps<Object> NBT;
    public static final DynamicOps<Tag> SPARROW_NBT;
    public static final DynamicOps<Object> JAVA;
    public static final DynamicOps<JsonElement> JSON;
    public static final @Nullable DynamicOps<HashCode> HASHCODE; // 1.21.5+

    // 1.20.5+
    public static final Class<?> clazz$JavaOps = ReflectionUtils.getClazz("com.mojang.serialization.JavaOps");

    public static final Class<?> clazz$NbtOps = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "nbt.DynamicOpsNBT",
                    "nbt.NbtOps"
            )
    );

    static {
        try {
            if (clazz$JavaOps != null) {
                // 1.20.5+
                Object javaOps = ReflectionUtils.getDeclaredField(clazz$JavaOps, clazz$JavaOps, 0).get(null);
                JAVA = (DynamicOps<Object>) RegistryOpsProxy.INSTANCE.create((DynamicOps<?>) javaOps, FastNMS.INSTANCE.registryAccess());
            } else if (!VersionHelper.isOrAbove1_20_5()) {
                // 1.20.1-1.20.4
                JAVA = (DynamicOps<Object>) RegistryOpsProxy.INSTANCE.create(LegacyJavaOps.INSTANCE, FastNMS.INSTANCE.registryAccess());
            } else {
                throw new ReflectionInitException("Could not find JavaOps");
            }
            NBT = (DynamicOps<Object>) RegistryOpsProxy.INSTANCE.create((DynamicOps<?>) ReflectionUtils.getDeclaredField(clazz$NbtOps, clazz$NbtOps, 0).get(null), FastNMS.INSTANCE.registryAccess());
            JSON = (DynamicOps<JsonElement>) RegistryOpsProxy.INSTANCE.create(JsonOps.INSTANCE, FastNMS.INSTANCE.registryAccess());
            SPARROW_NBT = (DynamicOps<Tag>) RegistryOpsProxy.INSTANCE.create(VersionHelper.isOrAbove1_20_5() ? NBTOps.INSTANCE : LegacyNBTOps.INSTANCE, FastNMS.INSTANCE.registryAccess());
            HASHCODE = VersionHelper.isOrAbove1_21_5() ? (DynamicOps<HashCode>) RegistryOpsProxy.INSTANCE.create((DynamicOps<?>) HashOpsProxy.CRC32C_INSTANCE, FastNMS.INSTANCE.registryAccess()) : null;
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to init DynamicOps", e);
        }
    }
}
