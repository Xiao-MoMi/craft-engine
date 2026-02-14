package net.momirealms.craftengine.bukkit.plugin;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.util.RegistryOps;
import net.momirealms.craftengine.bukkit.util.ParticleUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.bukkit.world.particle.BukkitParticleType;
import net.momirealms.craftengine.core.plugin.Platform;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.particle.ParticleType;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.Bukkit;
import org.bukkit.Particle;

public class BukkitPlatform implements Platform {
    private final BukkitCraftEngine plugin;

    public BukkitPlatform(BukkitCraftEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void dispatchCommand(String command) {
        if (VersionHelper.isFolia()) {
            Bukkit.getGlobalRegionScheduler().run(this.plugin.javaPlugin(), (t) -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command));
        } else {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    @Override
    public boolean isStopping() {
        return Bukkit.isStopping();
    }

    @Override
    public Tag jsonToSparrowNBT(JsonElement json) {
        return RegistryOps.JSON.convertTo(RegistryOps.SPARROW_NBT, json);
    }

    @Override
    public Tag javaToSparrowNBT(Object object) {
        return RegistryOps.JAVA.convertTo(RegistryOps.SPARROW_NBT, object);
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

    @Override
    public int biomeCount() {
        return RegistryUtils.currentBiomeRegistrySize();
    }
}
