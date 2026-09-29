package net.momirealms.craftengine.bukkit.world;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.momirealms.craftengine.bukkit.util.LightUtils;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.SectionPosUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.world.TickRateManagerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.chunk.storage.StorageAdaptor;
import net.momirealms.craftengine.core.world.chunk.storage.WorldDataStorage;

public class BukkitCEWorld extends CEWorld {
    protected boolean runBlockEntityTick;
    private volatile long permittedAsyncTick;
    private long consumedAsyncTick;

    public BukkitCEWorld(World world, StorageAdaptor adaptor) {
        super(world, adaptor);
    }

    public BukkitCEWorld(World world, WorldDataStorage dataStorage) {
        super(world, dataStorage);
    }

    @Override
    public void syncTick() {
        this.runBlockEntityTick = !VersionHelper.isOrAbove1_20_3 || TickRateManagerProxy.INSTANCE.runsNormally(LevelProxy.INSTANCE.tickRateManager(this.world.minecraftWorld()));
        // Publish at most one async pass per permitted game tick. Slow async passes coalesce ticks.
        if (this.runBlockEntityTick) this.permittedAsyncTick++;
        super.syncTick();
    }

    @Override
    protected void tickSyncBlockEntities() {
        this.syncTickingBlockEntities.tick(this.runBlockEntityTick);
    }

    @Override
    protected void tickAsyncBlockEntities() {
        long permitted = this.permittedAsyncTick;
        boolean runTickers = permitted != this.consumedAsyncTick;
        this.consumedAsyncTick = permitted;
        // Rebindings and unloads must still be processed while gameplay is frozen.
        this.asyncTickingBlockEntities.tick(runTickers);
    }

    @Override
    public void updateLight() {
        if (!Config.enableBlockLightSystem() || !super.lightUpdateRunning.compareAndSet(false, true)) {
            return;
        }
        try {
            LongOpenHashSet sections = super.drainPendingLightSections();
            if (sections == null || sections.isEmpty()) {
                return;
            }
            LightUtils.updateChunkLight(
                    (org.bukkit.World) this.world.platformWorld(),
                    SectionPosUtils.toMap(sections,
                            this.world.worldHeight().getMinSection() - 1,
                            this.world.worldHeight().getMaxSection() + 1
                    )
            );
        } finally {
            super.lightUpdateRunning.set(false);
        }
    }
}
