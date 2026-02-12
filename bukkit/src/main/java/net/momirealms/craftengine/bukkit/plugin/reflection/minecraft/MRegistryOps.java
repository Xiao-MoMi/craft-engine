package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.JsonOps;
import net.momirealms.craftengine.bukkit.plugin.reflection.ReflectionInitException;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.nbt.NbtOpsProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.RegistryOpsProxy;
import net.momirealms.sparrow.nbt.Tag;
import net.momirealms.sparrow.nbt.codec.LegacyJavaOps;
import net.momirealms.sparrow.nbt.codec.LegacyNBTOps;
import net.momirealms.sparrow.nbt.codec.NBTOps;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;

public final class MRegistryOps {
    private MRegistryOps() {}

    public static final DynamicOps<Object> NBT;
    public static final DynamicOps<Tag> SPARROW_NBT;
    public static final DynamicOps<Object> JAVA;
    public static final DynamicOps<JsonElement> JSON;

    static {
        Object registryAccess = RegistryUtils.getRegistryAccess();
        if (SparrowClass.existsNoRemap("com.mojang.serialization.JavaOps")) {
            // 1.20.5+
            JAVA = RegistryOpsProxy.INSTANCE.create(JavaOps.INSTANCE, registryAccess);
        } else if (!VersionHelper.isOrAbove1_20_5()) {
            // 1.20.1-1.20.4
            JAVA = RegistryOpsProxy.INSTANCE.create(LegacyJavaOps.INSTANCE, registryAccess);
        } else {
            throw new ReflectionInitException("Could not find JavaOps");
        }
        NBT = RegistryOpsProxy.INSTANCE.create(NbtOpsProxy.NBT_OPS_INSTANCE, registryAccess);
        JSON = RegistryOpsProxy.INSTANCE.create(JsonOps.INSTANCE, registryAccess);
        SPARROW_NBT = RegistryOpsProxy.INSTANCE.create(VersionHelper.isOrAbove1_20_5() ? NBTOps.INSTANCE : LegacyNBTOps.INSTANCE, registryAccess);
    }
}
