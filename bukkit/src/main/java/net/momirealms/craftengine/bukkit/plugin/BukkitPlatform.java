package net.momirealms.craftengine.bukkit.plugin;

import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistryOps;
import net.momirealms.craftengine.bukkit.util.ParticleUtils;
import net.momirealms.craftengine.bukkit.world.particle.BukkitParticleType;
import net.momirealms.craftengine.core.plugin.Platform;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.particle.ParticleType;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.Bukkit;
import org.bukkit.Particle;

import java.util.Map;

public class BukkitPlatform implements Platform {
    private final BukkitCraftEngine plugin;

    public BukkitPlatform(BukkitCraftEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void dispatchCommand(String command) {
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    @Override
    public Tag jsonToSparrowNBT(JsonElement json) {
        return MRegistryOps.JSON.convertTo(MRegistryOps.SPARROW_NBT, json);
    }

    @Override
    public Tag javaToSparrowNBT(Object object) {
        return MRegistryOps.JAVA.convertTo(MRegistryOps.SPARROW_NBT, object);
    }

    @Override
    public World getWorld(String name) {
        org.bukkit.World world = Bukkit.getWorld(name);
        if (world == null) {
            return null;
        }
        return BukkitAdaptors.adapt(world);
    }

    @Override
    public ParticleType getParticleType(Key name) {
        Particle particle = ParticleUtils.getParticle(name);
        if (particle == null) {
            throw new IllegalArgumentException("Invalid particle: " + name);
        }
        return new BukkitParticleType(particle, name);
    }
}
