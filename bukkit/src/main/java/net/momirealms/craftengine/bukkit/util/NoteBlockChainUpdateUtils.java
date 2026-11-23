package net.momirealms.craftengine.bukkit.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.core.block.BlockKeys;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.Vec3iProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerChunkCacheProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.ShulkerBoxBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.entity.ShulkerBoxBlockEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.piston.PistonMovingBlockEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy.BlockStateBaseProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.StateDefinitionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.StateHolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.properties.BlockStatePropertiesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.properties.NoteBlockInstrumentProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.LevelChunkProxy;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.lang.ref.WeakReference;
import java.util.Locale;

public final class NoteBlockChainUpdateUtils {
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Object NOTE = block(BlockKeys.NOTE_BLOCK);
    private static final Object WIRE = block(BlockKeys.TRIPWIRE);
    private static final Object MOVING = block(Key.of("minecraft:moving_piston"));
    private static final Object PISTON = block(Key.of("minecraft:piston"));
    private static final Object STICKY_PISTON = block(Key.of("minecraft:sticky_piston"));
    private static final Object INSTRUMENT = property(NOTE, "instrument");
    private static final Object[] CONNECTION = new Object[DIRECTIONS.length];

    static {
        for (Direction direction : DIRECTIONS) {
            if (direction.stepY() == 0) {
                CONNECTION[direction.ordinal()] = property(WIRE, direction.name().toLowerCase(Locale.ROOT));
            }
        }
    }

    private NoteBlockChainUpdateUtils() {
    }

    private static Object block(Key key) {
        return RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.BLOCK, KeyUtils.toIdentifier(key));
    }

    private static Object property(Object block, String name) {
        return StateDefinitionProxy.INSTANCE.getProperty(BlockProxy.INSTANCE.getStateDefinition(block), name);
    }

    public static void onUpdateShape(Object raw, Object level, Object pos, Object direction, Object neighbor) {
        Object sourceBlock = BlockStateBaseProxy.INSTANCE.getBlock(neighbor);
        if (!isAnimationBlock(sourceBlock)) {
            return;
        }
        if (!ServerLevelProxy.CLASS.isInstance(level)) {
            return;
        }
        Direction towardsSource = DIRECTIONS[((Enum<?>) direction).ordinal()];
        Object state = visual(raw);
        if (!isSensitive(state, towardsSource)) {
            return;
        }
        int x = Vec3iProxy.INSTANCE.getX(pos);
        int y = Vec3iProxy.INSTANCE.getY(pos);
        int z = Vec3iProxy.INSTANCE.getZ(pos);
        ChainUpdate update = ChainUpdate.acquire(LevelProxy.INSTANCE.getWorld(level), ServerLevelProxy.INSTANCE.getChunkSource(level));
        try {
            if (update.read(x, y, z) != raw) {
                return;
            }
            Object source = update.read(x + towardsSource.stepX(), y + towardsSource.stepY(), z + towardsSource.stepZ());
            // Vanilla can notify with air after a moving head has already occupied the origin.
            if (source == neighbor || (source != null && neighbor == BlocksProxy.AIR$defaultState && BlockStateBaseProxy.INSTANCE.getBlock(source) == MOVING)) {
                update.start(x, y, z, towardsSource, state, source);
            }
        } finally {
            update.release();
        }
    }

    public static void onVanillaPhysics(Block target, Block sourceBlock) {
        int x = target.getX();
        int y = target.getY();
        int z = target.getZ();
        int dx = sourceBlock.getX() - x;
        int dy = sourceBlock.getY() - y;
        int dz = sourceBlock.getZ() - z;
        int distance = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
        if (distance != 1) {
            return;
        }
        World world = target.getWorld();
        Object level = CraftWorldProxy.INSTANCE.getWorld(world);
        ChainUpdate update = ChainUpdate.acquire(world, ServerLevelProxy.INSTANCE.getChunkSource(level));
        try {
            Object source = update.read(x + dx, y + dy, z + dz);
            if (source == null || !isAnimationBlock(BlockStateBaseProxy.INSTANCE.getBlock(source))) {
                return;
            }
            Object raw = update.read(x, y, z);
            if (raw == null) {
                return;
            }
            Object block = BlockStateBaseProxy.INSTANCE.getBlock(raw);
            if (block != NOTE && block != WIRE) {
                return;
            }
            Object state = visual(raw);
            Direction direction = neighborDirection(dx, dy, dz);
            if (isSensitive(state, direction)) {
                update.start(x, y, z, direction, state, source);
            }
        } finally {
            update.release();
        }
    }

    private static Direction neighborDirection(int dx, int dy, int dz) {
        if (dy != 0) {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        }
        if (dx != 0) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static boolean isAnimationBlock(Object block) {
        return block == MOVING || block == BlocksProxy.AIR || block == PISTON || block == STICKY_PISTON || ShulkerBoxBlockProxy.CLASS.isInstance(block);
    }

    private static boolean isSensitive(Object state, Direction towardsSource) {
        Object block = BlockStateBaseProxy.INSTANCE.getBlock(state);
        if (towardsSource.stepY() != 0) {
            return block == NOTE;
        }
        return block == WIRE && StateHolderProxy.INSTANCE.<Boolean>getValue(state, CONNECTION[towardsSource.ordinal()]);
    }

    private static Object visual(Object state) {
        int id = BlockStateUtils.blockStateToId(state);
        int mapped = BukkitNetworkManager.instance().remapBlockState(id, false);
        return id == mapped ? state : BlockStateUtils.idToBlockState(mapped);
    }

    private static final class ChainUpdate {
        private static final ThreadLocal<WeakReference<ChainUpdate>> LOCAL = new ThreadLocal<>();
        private static final int MAX_RETAINED_POSITIONS = 128;
        private World world;
        private Object chunkSource;
        private final Object cursor = BlockPosProxy.MutableBlockPosProxy.INSTANCE.newInstance();
        private LongOpenHashSet changed;
        private LongArrayList pending;
        private ObjectArrayList<Object> pendingStates;
        private long firstKey = ChunkPos.INVALID_CHUNK_POS;
        private Object firstChunk;
        private Long2ObjectOpenHashMap<Object> otherChunks;

        private static ChainUpdate acquire(World world, Object chunkSource) {
            WeakReference<ChainUpdate> reference = LOCAL.get();
            ChainUpdate update = reference == null ? null : reference.get();
            if (update == null) {
                update = new ChainUpdate();
                LOCAL.set(new WeakReference<>(update));
            } else if (update.world != null) {
                // A nested notification must not overwrite the outer traversal.
                update = new ChainUpdate();
            }
            update.world = world;
            update.chunkSource = chunkSource;
            return update;
        }

        private void release() {
            this.world = null;
            this.chunkSource = null;
            this.firstKey = ChunkPos.INVALID_CHUNK_POS;
            this.firstChunk = null;
            if (this.otherChunks != null) {
                if (this.otherChunks.size() > 16) {
                    this.otherChunks = null;
                } else {
                    this.otherChunks.clear();
                }
            }
            if (this.pending != null && !this.pending.isEmpty()) {
                if (this.pending.size() > MAX_RETAINED_POSITIONS) {
                    this.pending = null;
                    this.pendingStates = null;
                    this.changed = null;
                } else {
                    this.pending.clear();
                    this.pendingStates.clear();
                    this.changed.clear();
                }
            }
        }

        private Object chunkAt(int x, int z) {
            int cx = x >> 4;
            int cz = z >> 4;
            long key = ChunkPos.asLong(cx, cz);
            if (key == this.firstKey) {
                return this.firstChunk;
            }
            if (this.firstKey == ChunkPos.INVALID_CHUNK_POS) {
                this.firstKey = key;
                this.firstChunk = this.loadedChunk(cx, cz);
                return this.firstChunk;
            }
            if (this.otherChunks == null) {
                this.otherChunks = new Long2ObjectOpenHashMap<>(4);
            }
            Object chunk = this.otherChunks.get(key);
            if (chunk == null && !this.otherChunks.containsKey(key)) {
                chunk = this.loadedChunk(cx, cz);
                this.otherChunks.put(key, chunk);
            }
            return chunk;
        }

        private Object loadedChunk(int cx, int cz) {
            return VersionHelper.hasFoliaPatch && !Bukkit.isOwnedByCurrentRegion(this.world, cx, cz) ? null : LevelUtils.getChunkAtIfLoaded(this.chunkSource, cx, cz);
        }

        private Object read(int x, int y, int z) {
            Object chunk = this.chunkAt(x, z);
            if (chunk == null) {
                return null;
            }
            if (VersionHelper.hasPaperPatch) {
                return LevelChunkProxy.INSTANCE.getBlockState(chunk, x, y, z);
            }
            return BlockGetterProxy.INSTANCE.getBlockState(chunk, this.position(x, y, z));
        }

        private Object position(int x, int y, int z) {
            Vec3iProxy.INSTANCE.setX(this.cursor, x);
            Vec3iProxy.INSTANCE.setY(this.cursor, y);
            Vec3iProxy.INSTANCE.setZ(this.cursor, z);
            return this.cursor;
        }

        private Object visualAt(int x, int y, int z) {
            Object state = this.read(x, y, z);
            return state == null ? null : visual(state);
        }

        private void start(int x, int y, int z, Direction towardsSource, Object state, Object sourceState) {
            int sx = x + towardsSource.stepX();
            int sy = y + towardsSource.stepY();
            int sz = z + towardsSource.stepZ();
            Object sourceBlock = BlockStateBaseProxy.INSTANCE.getBlock(sourceState);
            Object sourceVisual = visual(sourceState);
            if (towardsSource.stepY() != 0 && !this.instrumentChanged(state, x, y, z, towardsSource.opposite(), sourceVisual)) {
                return;
            }
            if (ShulkerBoxBlockProxy.CLASS.isInstance(sourceBlock)) {
                if (!ShulkerBoxBlockProxy.CLASS.isInstance(BlockStateBaseProxy.INSTANCE.getBlock(sourceVisual)) || !this.isMovingLid(sx, sy, sz)) {
                    return;
                }
            } else if (!this.isPistonSource(sx, sy, sz, sourceState)) {
                return;
            }
            this.mark(x, y, z, state);
            this.propagate();
        }

        private boolean isMovingLid(int x, int y, int z) {
            Object entity = BlockGetterProxy.INSTANCE.getBlockEntity(this.chunkAt(x, z), this.position(x, y, z));
            if (!ShulkerBoxBlockEntityProxy.CLASS.isInstance(entity)) {
                return false;
            }
            ShulkerBoxBlockEntityProxy box = ShulkerBoxBlockEntityProxy.INSTANCE;
            Object status = box.getAnimationStatus(entity);
            // The final shape update runs after the lid has already switched to OPENED or CLOSED.
            return status == ShulkerBoxBlockEntityProxy.AnimationStatusProxy.OPENING
                    || status == ShulkerBoxBlockEntityProxy.AnimationStatusProxy.CLOSING
                    || box.getProgress(entity, 0.0f) != box.getProgress(entity, 1.0f);
        }

        private boolean isPistonSource(int sx, int sy, int sz, Object state) {
            Object block = BlockStateBaseProxy.INSTANCE.getBlock(state);
            if (block == MOVING) {
                return true;
            }
            if (block != BlocksProxy.AIR) {
                Enum<?> facing = (Enum<?>) StateHolderProxy.INSTANCE.getValue(state, BlockStatePropertiesProxy.FACING);
                Direction direction = DIRECTIONS[facing.ordinal()];
                Object headState = this.read(sx + direction.stepX(), sy + direction.stepY(), sz + direction.stepZ());
                return headState != null
                        && BlockStateBaseProxy.INSTANCE.getBlock(headState) == MOVING
                        && StateHolderProxy.INSTANCE.getValue(headState, BlockStatePropertiesProxy.FACING) == facing;
            }
            // A vacated origin has a moving piston one step away in the actual direction of travel.
            for (Direction step : DIRECTIONS) {
                int x = sx + step.stepX();
                int y = sy + step.stepY();
                int z = sz + step.stepZ();
                Object adjacent = this.read(x, y, z);
                if (adjacent == null || BlockStateBaseProxy.INSTANCE.getBlock(adjacent) != MOVING) {
                    continue;
                }
                Enum<?> facing = (Enum<?>) StateHolderProxy.INSTANCE.getValue(adjacent, BlockStatePropertiesProxy.FACING);
                if (facing.ordinal() != step.ordinal() && facing.ordinal() != step.opposite().ordinal()) {
                    continue;
                }
                Object entity = BlockGetterProxy.INSTANCE.getBlockEntity(this.chunkAt(x, z), this.position(x, y, z));
                if (PistonMovingBlockEntityProxy.CLASS.isInstance(entity) && PistonMovingBlockEntityProxy.INSTANCE.isExtending(entity) == (facing.ordinal() == step.ordinal())) {
                    return true;
                }
            }
            return false;
        }

        private void mark(int x, int y, int z, Object visualState) {
            if (this.changed == null) {
                this.changed = new LongOpenHashSet();
                this.pending = new LongArrayList();
                this.pendingStates = new ObjectArrayList<>();
            }
            long pos = BlockPos.asLong(x, y, z);
            this.changed.add(pos);
            this.pending.add(pos);
            this.pendingStates.add(visualState);
            ServerChunkCacheProxy.INSTANCE.blockChanged(this.chunkSource, this.position(x, y, z));
        }

        private void propagate() {
            if (this.pending == null || this.pending.isEmpty()) {
                return;
            }
            Object airAppearance = BlockStateBaseProxy.INSTANCE.getBlock(visual(BlocksProxy.AIR$defaultState));
            boolean skipAir = airAppearance != NOTE && airAppearance != WIRE;
            // Traverse in waves so the existing limit measures distance from the original shape update.
            int index = 0;
            for (int depth = 1; index < this.pending.size() && depth < Config.maxNoteBlockChainUpdate(); depth++) {
                int end = this.pending.size();
                while (index < end) {
                    long pos = this.pending.getLong(index);
                    this.visitNeighbors(BlockPos.unpackX(pos), BlockPos.unpackY(pos), BlockPos.unpackZ(pos), this.pendingStates.get(index), false, skipAir);
                    index++;
                }
            }
        }

        private void visitNeighbors(int sx, int sy, int sz, Object sourceState, boolean vanillaRootsOnly, boolean skipAir) {
            boolean sourceIsWire = BlockStateBaseProxy.INSTANCE.getBlock(sourceState) == WIRE;
            for (Direction direction : DIRECTIONS) {
                int x = sx + direction.stepX();
                int y = sy + direction.stepY();
                int z = sz + direction.stepZ();
                Object raw = this.read(x, y, z);
                if (raw == null || (skipAir && raw == BlocksProxy.AIR$defaultState)) {
                    continue;
                }
                if (this.changed != null && this.changed.contains(BlockPos.asLong(x, y, z))) {
                    continue;
                }
                if (vanillaRootsOnly) {
                    Object owner = BlockStateBaseProxy.INSTANCE.getBlock(raw);
                    if (owner != NOTE && owner != WIRE) {
                        continue;
                    }
                }
                Object state = visual(raw);
                Object block = BlockStateBaseProxy.INSTANCE.getBlock(state);
                boolean different = direction.stepY() != 0
                        ? block == NOTE && this.instrumentChanged(state, x, y, z, direction, sourceState)
                        : block == WIRE && sourceIsWire != StateHolderProxy.INSTANCE.<Boolean>getValue(state, CONNECTION[direction.opposite().ordinal()]);
                if (different) {
                    this.mark(x, y, z, state);
                }
            }
        }

        private boolean instrumentChanged(Object note, int x, int y, int z, Direction direction, Object sourceState) {
            Object expected = StateHolderProxy.INSTANCE.getValue(note, INSTRUMENT);
            // Only instruments supplied by a head can override the block below.
            if (direction == Direction.UP && expected != NoteBlockInstrumentProxy.HARP
                    && expected != BlockStateBaseProxy.INSTANCE.getInstrument(sourceState)
                    && !NoteBlockInstrumentProxy.INSTANCE.worksAboveNoteBlock(expected)) {
                return true;
            }
            Object above = direction == Direction.DOWN ? sourceState : this.visualAt(x, y + 1, z);
            if (above == null) {
                return false;
            }
            Object instrument = BlockStateBaseProxy.INSTANCE.getInstrument(above);
            if (!NoteBlockInstrumentProxy.INSTANCE.worksAboveNoteBlock(instrument)) {
                Object below = direction == Direction.UP ? sourceState : this.visualAt(x, y - 1, z);
                if (below == null) {
                    return false;
                }
                instrument = BlockStateBaseProxy.INSTANCE.getInstrument(below);
                if (NoteBlockInstrumentProxy.INSTANCE.worksAboveNoteBlock(instrument)) {
                    instrument = NoteBlockInstrumentProxy.HARP;
                }
            }
            return expected != instrument;
        }
    }
}
