package net.momirealms.craftengine.bukkit.plugin;

import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistryOps;
import net.momirealms.craftengine.core.plugin.Platform;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.Bukkit;

import java.util.Map;

public class BukkitPlatform implements Platform {

    @Override
    public void dispatchCommand(String command) {
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object snbtToJava(String nbt) {
        try {
            Object tag = FastNMS.INSTANCE.method$TagParser$parseCompoundFully("{\"root\":" + nbt + "}");
            Map<String, Object> map = (Map<String, Object>) MRegistryOps.NBT.convertTo(MRegistryOps.JAVA, tag);
            return map.get("root");
        } catch (CommandSyntaxException e) {
            throw new LocalizedResourceConfigException("warning.config.type.snbt.invalid_syntax", e, nbt);
        }
    }

    @Override
    public Tag jsonToSparrowNBT(JsonElement json) {
        return MRegistryOps.JSON.convertTo(MRegistryOps.SPARROW_NBT, json);
    }

    @Override
    public Tag snbtToSparrowNBT(String nbt) {
        try {
            Object tag = FastNMS.INSTANCE.method$TagParser$parseCompoundFully("{\"root\":" + nbt + "}");
            CompoundTag map = (CompoundTag) MRegistryOps.NBT.convertTo(MRegistryOps.SPARROW_NBT, tag);
            return map.get("root");
        } catch (CommandSyntaxException e) {
            throw new LocalizedResourceConfigException("warning.config.type.snbt.invalid_syntax", e, nbt);
        }
    }

    @Override
    public Tag javaToSparrowNBT(Object object) {
        return MRegistryOps.JAVA.convertTo(MRegistryOps.SPARROW_NBT, object);
    }
}
