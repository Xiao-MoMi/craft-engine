package net.momirealms.craftengine.bukkit.compatibility.legacy.slimeworld;

import com.infernalsuite.aswm.api.events.LoadSlimeWorldEvent;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldManager;
import net.momirealms.craftengine.core.world.chunk.storage.CachedStorage;
import net.momirealms.craftengine.core.world.chunk.storage.DefaultStorageAdaptor;
import net.momirealms.craftengine.core.world.chunk.storage.WorldDataStorage;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.function.Function;

public final class LegacySlimeFormatStorageAdaptor extends DefaultStorageAdaptor implements Listener {
    private final WorldManager worldManager;
    private final Function<String, SlimeWorld> SLIME_WORLD_GETTER;

    @EventHandler
    public void onWorldLoad(LoadSlimeWorldEvent event) {
        org.bukkit.World world = Bukkit.getWorld(event.getSlimeWorld().getName());
        if (world == null) return;
        CEWorld ceWorld = this.worldManager.createWorld(this.worldManager.wrap(world),
                Config.enableChunkCache() ? new CachedStorage<>(new LegacySlimeWorldDataStorage(event.getSlimeWorld())) : new LegacySlimeWorldDataStorage(event.getSlimeWorld()));
        this.worldManager.loadWorld(ceWorld, true);
    }

    public LegacySlimeFormatStorageAdaptor(WorldManager worldManager, int version) {
        this.worldManager = worldManager;
        try {
            if (version == 1) {
                Plugin plugin = Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
                Class<?> slimeClass = Class.forName("com.infernalsuite.aswm.api.SlimePlugin");
                Method method = slimeClass.getMethod("getWorld", String.class);
                this.SLIME_WORLD_GETTER = (name) -> {
                    try {
                        return (SlimeWorld) method.invoke(plugin, name);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                };
            } else if (version == 2) {
                Class<?> apiClass = Class.forName("com.infernalsuite.aswm.api.AdvancedSlimePaperAPI");
                Object apiInstance = apiClass.getMethod("instance").invoke(null);
                Method method = apiClass.getMethod("getLoadedWorld", String.class);
                this.SLIME_WORLD_GETTER = (name) -> {
                    try {
                        return (SlimeWorld) method.invoke(apiInstance, name);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                };
            } else {
                throw new IllegalArgumentException("Unsupported version: " + version);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public SlimeWorld getWorld(String name) {
        return this.SLIME_WORLD_GETTER.apply(name);
    }

    // 请注意，在加载事件的时候，无法通过AdvancedSlimePaperAPI.instance().getLoadedWorld来判断是否为slime世界
    @Override
    public @NotNull WorldDataStorage adapt(@NotNull World world) {
        SlimeWorld slimeWorld = getWorld(world.name());
        if (slimeWorld == null) {
            return super.adapt(world);
        }
        return new LegacySlimeWorldDataStorage(slimeWorld);
    }
}
