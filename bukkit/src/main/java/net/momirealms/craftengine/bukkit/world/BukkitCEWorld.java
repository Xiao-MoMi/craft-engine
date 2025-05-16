package net.momirealms.craftengine.bukkit.world;

import net.momirealms.craftengine.bukkit.util.LightUtils;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.SectionPosUtils;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.SectionPos;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.chunk.storage.StorageAdaptor;
import net.momirealms.craftengine.core.world.chunk.storage.WorldDataStorage;

import java.util.HashSet;

public class BukkitCEWorld extends CEWorld {

    public BukkitCEWorld(World world, StorageAdaptor adaptor) {
        super(world, adaptor);
    }

    public BukkitCEWorld(World world, WorldDataStorage dataStorage) {
        super(world, dataStorage);
    }

    @Override
    public void tick() {
        HashSet<SectionPos> poses;
        synchronized (super.updatedSectionSet) {
            poses = new HashSet<>(super.updatedSectionSet);
            super.updatedSectionSet.clear();
        }
        if (Config.enableLightSystem()) {
            LightUtils.updateChunkLight(
                    (org.bukkit.World) world.platformWorld(),
                    SectionPosUtils.toMap(poses,
                            world.worldHeight().getMinSection() - 1,
                            world.worldHeight().getMaxSection() + 1
                    )
            );
        }
    }
}
