package net.momirealms.craftengine.bukkit.plugin;

import com.google.gson.JsonElement;
import io.netty.buffer.ByteBuf;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.bukkit.world.BukkitContainer;
import net.momirealms.craftengine.bukkit.world.particle.BukkitParticleType;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.Platform;
import net.momirealms.craftengine.core.plugin.network.id.PacketIds;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.Container;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.particle.ParticleType;
import net.momirealms.craftengine.proxy.paper.configuration.GlobalConfigurationProxy;
import net.momirealms.craftengine.proxy.spigotmc.SpigotConfigProxy;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class BukkitPlatform implements Platform {
    private final BukkitCraftEngine plugin;

    public BukkitPlatform(BukkitCraftEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void dispatchCommand(String command) {
        if (VersionHelper.hasFoliaPatch) {
            Bukkit.getGlobalRegionScheduler().run(this.plugin.javaPlugin(), (t) -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command));
        } else {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
        }
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
    public JsonElement sparrowNBTToJson(Tag tag) {
        return RegistryOps.SPARROW_NBT.convertTo(RegistryOps.JSON, tag);
    }

    @Override
    public Player getPlayer(UUID uuid) {
        return (Player) BukkitNetworkManager.instance().getOnlineUser(uuid);
    }

    @Override
    public World getWorld(String name) {
        org.bukkit.World world = Bukkit.getWorld(name);
        if (world == null) {
            return null;
        }
        return BukkitAdaptor.adapt(world);
    }

    @Override
    public ParticleType getParticleType(Key name) {
        Object particle = ParticleUtils.getParticleType(name);
        if (particle == null) {
            throw new IllegalArgumentException("Invalid particle: " + name);
        }
        return new BukkitParticleType(particle, name);
    }

    @Override
    public int biomeCount() {
        return RegistryUtils.currentBiomeRegistrySize();
    }

    @Override
    public Object createContainer(Container container) {
        if (container instanceof BukkitContainer bukkitContainer) {
            return FastNMS.INSTANCE.createContainer(bukkitContainer);
        } else {
            throw new IllegalArgumentException("Container is not a BukkitContainer");
        }
    }

    public Item readItem(ByteBuf buf) {
        return PacketUtils.readItem(buf);
    }

    public void writeItem(ByteBuf buf, Item item) {
        PacketUtils.writeItem(buf, item);
    }

    @Override
    public PacketIds packetIds() {
        return BukkitNetworkManager.PACKET_IDS;
    }

    @Override
    public int getServerPort() {
        return Bukkit.getPort();
    }

    @Override
    public boolean hasProxy() {
        boolean bungee = VersionHelper.hasSpigotPatch && SpigotConfigProxy.INSTANCE.getBungee();
        boolean velocity = false;
        if (VersionHelper.hasPaperPatch) {
            velocity = GlobalConfigurationProxy.ProxiesProxy.VelocityProxy.INSTANCE.getEnabled(
                    GlobalConfigurationProxy.ProxiesProxy.INSTANCE.getVelocity(
                            GlobalConfigurationProxy.INSTANCE.getProxies(
                                    GlobalConfigurationProxy.INSTANCE.get()
                            )
                    )
            );
        }
        return bungee || velocity;
    }

    @Override
    public AtomicInteger getEntityCounter() {
        return EntityUtils.ENTITY_COUNTER;
    }
}
