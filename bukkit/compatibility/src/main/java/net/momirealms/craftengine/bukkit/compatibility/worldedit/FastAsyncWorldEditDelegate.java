package net.momirealms.craftengine.bukkit.compatibility.worldedit;

import com.fastasyncworldedit.bukkit.adapter.CachedBukkitAdapter;
import com.fastasyncworldedit.bukkit.adapter.FaweAdapter;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.extent.processor.ExtentBatchProcessorHolder;
import com.fastasyncworldedit.core.util.ExtentTraverser;
import com.fastasyncworldedit.core.util.ProcessorTraverser;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.MaskingExtent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.injector.WorldStorageInjector;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.EmptyBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.SectionPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.CESection;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

import static java.util.Objects.requireNonNull;

public class FastAsyncWorldEditDelegate extends AbstractDelegateExtent {
    private static int[] ordinalToIbdID;
    private final Set<CEChunk> chunksToSave;
    private final CEWorld ceWorld;
    private final Set<ChunkPos> brokenChunks = Collections.synchronizedSet(new HashSet<>());

    protected FastAsyncWorldEditDelegate(EditSessionEvent event, Extent extent) {
        super(extent);
        this.chunksToSave = new HashSet<>();
        World weWorld = event.getWorld();
        org.bukkit.World world = Bukkit.getWorld(requireNonNull(weWorld).getName());
        CEWorld ceWorld = CraftEngine.instance().worldManager().getWorld(requireNonNull(world).getUID());
        this.ceWorld = requireNonNull(ceWorld);
    }

    public static void init() {
        Settings.settings().EXTENT.ALLOWED_PLUGINS.add(FastAsyncWorldEditDelegate.class.getCanonicalName());
        FaweAdapter<?, ?> adapter = (FaweAdapter<?, ?>) WorldEditPlugin.getInstance().getBukkitImplAdapter();
        Method ordinalToIbdIDMethod = ReflectionUtils.getDeclaredMethod(CachedBukkitAdapter.class, int.class.arrayType(), new String[]{"getOrdinalToIbdID"});
        try {
            assert ordinalToIbdIDMethod != null;
            ordinalToIbdID = (int[]) ordinalToIbdIDMethod.invoke(adapter);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to init FastAsyncWorldEdit compatibility", e);
        }
        WorldEdit.getInstance().getEventBus().register(new Object() {
            @Subscribe
            @SuppressWarnings("unused")
            public void onEditSessionEvent(EditSessionEvent event) {
                World weWorld = event.getWorld();
                if (weWorld == null) return;
                Extent currentExtent = event.getExtent();
                if (event.getStage() == EditSession.Stage.BEFORE_CHANGE) {
                    event.setExtent(new FastAsyncWorldEditDelegate(event, currentExtent));
                }
            }
        });
    }

    private static void injectLevelChunk(Object chunkSource, CEChunk ceChunk) {
        ChunkPos pos = ceChunk.chunkPos();
        Object levelChunk = FastNMS.INSTANCE.method$ServerChunkCache$getChunk(chunkSource, pos.x, pos.z, false);
        if (levelChunk != null) {
            Object[] sections = FastNMS.INSTANCE.method$ChunkAccess$getSections(levelChunk);
            CESection[] ceSections = ceChunk.sections();
            synchronized (sections) {
                for (int i = 0; i < ceSections.length; i++) {
                    CESection ceSection = ceSections[i];
                    Object section = sections[i];
                    int finalI = i;
                    WorldStorageInjector.injectLevelChunkSection(section, ceSection, ceChunk, new SectionPos(pos.x, ceChunk.sectionY(i), pos.z),
                            (injected) -> sections[finalI] = injected);
                }
            }
        }
    }

    @Override
    public int setBlocks(final Set<BlockVector3> vset, final Pattern pattern) {
        this.processBlocks(vset, pattern, getMask(), null);
        return super.setBlocks(vset, pattern);
    }

    @Override
    public int setBlocks(final Region region, final Pattern pattern) {
        this.processBlocks(region, pattern, getMask(), null);
        return super.setBlocks(region, pattern);
    }

    @Override
    public <B extends BlockStateHolder<B>> int setBlocks(final Region region, final B block) {
        this.processBlocks(region, block, getMask(), null);
        return super.setBlocks(region, block);
    }

    @Override
    public int replaceBlocks(Region region, Mask mask, Pattern pattern) {
        this.processBlocks(region, pattern, mask, null);
        return super.replaceBlocks(region, mask, pattern);
    }

    @Override
    public <B extends BlockStateHolder<B>> int replaceBlocks(final Region region, final Set<BaseBlock> filter, final B replacement) {
        this.processBlocks(region, replacement, null, filter);
        return super.replaceBlocks(region, filter, replacement);
    }

    @Override
    public int replaceBlocks(final Region region, final Set<BaseBlock> filter, final Pattern pattern) {
        this.processBlocks(region, pattern, null, filter);
        return super.replaceBlocks(region, filter, pattern);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block) {
        boolean result = super.setBlock(x, y, z, block);
        if (result) {
            Mask mask = getMask();
            if (mask != null && !mask.test(BlockVector3.at(x, y, z))) return true;
            BaseBlock oldBlockState = getBlock(x, y, z).toBaseBlock();
            this.processBlock(x, y, z, block.toBaseBlock(), oldBlockState);
        }
        return result;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block) {
        boolean result = super.setBlock(position, block);
        if (result) {
            Mask mask = getMask();
            if (mask != null && !mask.test(position)) return true;
            BaseBlock oldBlockState = getBlock(position).toBaseBlock();
            this.processBlock(position.x(), position.y(), position.z(), block.toBaseBlock(), oldBlockState);
        }
        return result;
    }

    public Mask getMask() {
        MaskingExtent maskingExtent = new ExtentTraverser<>(getExtent()).findAndGet(MaskingExtent.class);
        if (maskingExtent == null) {
            ExtentBatchProcessorHolder processorExtent =
                    new ExtentTraverser<>(getExtent()).findAndGet(ExtentBatchProcessorHolder.class);
            if (processorExtent != null) {
                maskingExtent =
                        new ProcessorTraverser<>(processorExtent.getProcessor()).find(MaskingExtent.class);
            }
        }
        return maskingExtent != null ? maskingExtent.getMask() : null;
    }

    @Override
    public @Nullable Operation commit() {
        saveAllChunks();
        Operation operation = super.commit();
        List<ChunkPos> chunks = new ArrayList<>(this.brokenChunks);
        this.brokenChunks.clear();
            Object worldServer = this.ceWorld.world().serverWorld();
            Object chunkSource = FastNMS.INSTANCE.method$ServerLevel$getChunkSource(worldServer);
            for (ChunkPos chunk : chunks) {
                CEChunk loaded = this.ceWorld.getChunkAtIfLoaded(chunk.longKey());
                // only inject loaded chunks
                if (loaded == null) continue;
                injectLevelChunk(chunkSource, loaded);
            }
        return operation;
    }

    private void processBlocks(Iterable<BlockVector3> region, Pattern pattern, @Nullable Mask mask, @Nullable Set<BaseBlock> filter) {
        final boolean hasMask = mask != null;
        final boolean hasFilter = filter != null;
        for (BlockVector3 position : region) {
            if (hasMask && !mask.test(position)) continue;
            BaseBlock blockState = pattern.applyBlock(position);
            if (hasFilter && filter.contains(blockState)) continue;
            BaseBlock oldBlockState = getBlock(position).toBaseBlock();
            int blockX = position.x();
            int blockY = position.y();
            int blockZ = position.z();
            this.processBlock(blockX, blockY, blockZ, blockState, oldBlockState);
        }
    }

    private void processBlock(int blockX, int blockY, int blockZ, BaseBlock newBlock, BaseBlock oldBlock) {
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        int newStateId = ordinalToIbdID[newBlock.getOrdinal()];
        int oldStateId = ordinalToIbdID[oldBlock.getOrdinal()];
        this.brokenChunks.add(ChunkPos.of(chunkX, chunkZ));
        if (BlockStateUtils.isVanillaBlock(newStateId) && BlockStateUtils.isVanillaBlock(oldStateId)) return;
        try {
            CEChunk ceChunk = Optional.ofNullable(this.ceWorld.getChunkAtIfLoaded(chunkX, chunkZ))
                    .orElse(this.ceWorld.worldDataStorage().readChunkAt(this.ceWorld, new ChunkPos(chunkX, chunkZ)));
            CESection ceSection = ceChunk.sectionById(SectionPos.blockToSectionCoord(blockY));
            ImmutableBlockState immutableBlockState = BukkitBlockManager.instance().getImmutableBlockState(newStateId);
            if (immutableBlockState == null) {
                ceSection.setBlockState(blockX & 15, blockY & 15, blockZ & 15, EmptyBlock.STATE);
            } else {
                ceSection.setBlockState(blockX & 15, blockY & 15, blockZ & 15, immutableBlockState);
            }
            this.chunksToSave.add(ceChunk);
        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Error when recording FastAsyncWorldEdit operation blocks", e);
        }
    }

    private void saveAllChunks() {
        try {
            for (CEChunk ceChunk : this.chunksToSave) {
                this.ceWorld.worldDataStorage().writeChunkAt(ceChunk.chunkPos(), ceChunk);
            }
            this.chunksToSave.clear();
        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Error when recording FastAsyncWorldEdit operation chunks", e);
        }
    }
}
