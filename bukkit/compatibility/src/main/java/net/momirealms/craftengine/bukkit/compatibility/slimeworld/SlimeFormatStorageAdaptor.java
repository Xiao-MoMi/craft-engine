package net.momirealms.craftengine.bukkit.compatibility.slimeworld;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.events.LoadSlimeWorldEvent;
import com.infernalsuite.asp.api.world.SlimeWorld;
import net.momirealms.craftengine.bukkit.world.BukkitStorageAdaptor;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.bukkit.world.BukkitWorldManager;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.chunk.storage.CachedStorage;
import net.momirealms.craftengine.core.world.chunk.storage.WorldDataStorage;
import net.momirealms.craftengine.proxy.adventure.nbt.ByteArrayBinaryTagProxy;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public final class SlimeFormatStorageAdaptor extends BukkitStorageAdaptor implements Listener {
    private final BukkitWorldManager worldManager;

    @EventHandler
    public void onWorldLoad(LoadSlimeWorldEvent event) {
        org.bukkit.World world = Bukkit.getWorld(event.getSlimeWorld().getName());
        if (world == null) return;
        Debugger.CHUNK.debug(() -> "LoadSlimeWorldEvent -> " + world.getName());
        BukkitWorld bukkitWorld = this.worldManager.injectCraftWorld(world);
        WorldDataStorage storage = new CachedStorage<>(new SlimeWorldDataStorage(event.getSlimeWorld(), this));
        this.worldManager.installStorageWorld(bukkitWorld, this.worldManager.createStorageWorld(bukkitWorld, storage));
        this.worldManager.handleWorldLoad(bukkitWorld);
    }

    public SlimeFormatStorageAdaptor(BukkitWorldManager worldManager) {
        this.worldManager = worldManager;
    }

    public SlimeWorld getWorld(String name) {
        return AdvancedSlimePaperAPI.instance().getLoadedWorld(name);
    }

    // 请注意，在加载事件的时候，无法通过AdvancedSlimePaperAPI.instance().getLoadedWorld来判断是否为slime世界
    @Override
    public @NotNull WorldDataStorage adapt(@NotNull World world) {
        SlimeWorld slimeWorld = getWorld(world.name());
        if (slimeWorld == null) {
            return super.adapt(world);
        }
        return new CachedStorage<>(new SlimeWorldDataStorage(slimeWorld, this));
    }

    public byte[] byteArrayTagToBytes(Object byteArrayTag) {
        return ByteArrayBinaryTagProxy.INSTANCE.value(byteArrayTag);
    }

    public Object bytesToByteArrayTag(byte[] bytes) {
        return ByteArrayBinaryTagProxy.INSTANCE.byteArrayBinaryTag(bytes);
    }
}
