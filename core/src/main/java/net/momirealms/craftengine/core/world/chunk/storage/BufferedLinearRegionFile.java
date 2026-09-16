package net.momirealms.craftengine.core.world.chunk.storage;

import ca.spottedleaf.concurrentutil.util.ConcurrentUtil;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import net.jpountz.xxhash.XXHash32;
import net.jpountz.xxhash.XXHashFactory;
import net.momirealms.craftengine.core.world.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * BufferedLinearRegionFile ported from Luminol <a href="https://github.com/LuminolMC/Luminol">...</a>
 * The implementation follows the upstream code as closely as possible. Deliberate divergences,
 * limited to CraftEngine adaptations:
 */
public final class BufferedLinearRegionFile implements AutoCloseable {
    private static final double SWAP_FILE_AUTO_COMPACT_PERCENT = 3.0 / 5.0; // 60 %
    private static final long SWAP_FILE_AUTO_COMPACT_SIZE = 1024 * 1024; // 1 MiB

    private static final long SWAP_FILE_SUPER_BLOCK = 0x1145141919810L;
    private static final int SWAP_FILE_HASH_SEED = 0x0721;
    private static final byte SWAP_FILE_VERSION = 0x02; // ver 2.0

    private static final long MASTER_FILE_SUPER_BLOCK = -0x200812250269L;
    private static final byte MASTER_FILE_VERSION_BUCKET = 0x03; // ver 3.0

    private static final int BUCKET_SHIFT = 6;
    private static final int BUCKET_SIZE = 1 << BUCKET_SHIFT;
    private static final int BUCKET_COUNT = 1024 / BUCKET_SIZE;

    private static final int COMPRESSION_LEVEL = 6;

    private static final StandardOpenOption[] SWAP_FILE_CHANNEL_OPTIONS = new StandardOpenOption[]{
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.READ,
            StandardOpenOption.DELETE_ON_CLOSE
    };

    private static final class Bucket {
        private final Object lock = new Object();
        private final AtomicLong writeEpoch = new AtomicLong();
        private final AtomicLong syncedEpoch = new AtomicLong();
        private volatile boolean loaded = false;
    }

    private final Bucket[] buckets = new Bucket[BUCKET_COUNT];

    private final Path masterFilePath;
    private final Path swapFilePath;

    private final ReadWriteLock regionObjectLock = new ReentrantReadWriteLock();
    private final XXHash32 xxHash32 = XXHashFactory.fastestInstance().hash32();
    private Sector[] sectors = new Sector[1024];
    private long currentAcquiredIndex = this.headerSize();
    private int xxHash32Seed = SWAP_FILE_HASH_SEED;
    private FileChannel swapFileChannel;

    private final LinearMasterFileParser masterFileParser = new LinearMasterFileParser();
    private final CompressingOps compressingOps = new CompressingOps();

    // managed by VarHandles following
    private boolean closed = false;
    private boolean beingSynced = false;
    private boolean synced = false;
    private long lastWritten = System.nanoTime();

    private static final VarHandle CLOSED_HANDLE = ConcurrentUtil.getVarHandle(BufferedLinearRegionFile.class, "closed", boolean.class);
    private static final VarHandle SYNCED_HANDLE = ConcurrentUtil.getVarHandle(BufferedLinearRegionFile.class, "synced", boolean.class);
    private static final VarHandle BEING_SYNCED_HANDLE = ConcurrentUtil.getVarHandle(BufferedLinearRegionFile.class, "beingSynced", boolean.class);
    private static final VarHandle LAST_WRITTEN_HANDLE = ConcurrentUtil.getVarHandle(BufferedLinearRegionFile.class, "lastWritten", long.class);

    private final LinearRegionFileFlusher flusher;

    public BufferedLinearRegionFile(Path masterFilePath, @NotNull LinearRegionFileFlusher flusher) throws IOException {
        this.masterFilePath = masterFilePath;
        this.swapFilePath = Path.of(this.masterFilePath.toString() + ".swp");

        for (int i = 0; i < this.buckets.length; i++) {
            this.buckets[i] = new Bucket();
        }

        this.cleanUpSwapFile();
        this.initSwapFile();

        this.flusher = flusher;
        this.flusher.addFile(this);
    }

    private static void writeFullyAt(FileChannel channel, @NotNull ByteBuffer buf, long startOffset) throws IOException {
        long offset = startOffset;
        while (buf.hasRemaining()) {
            offset += channel.write(buf, offset);
        }
    }

    private static void readFullyAt(FileChannel channel, @NotNull ByteBuffer buf, long startOffset) throws IOException {
        long offset = startOffset;
        while (buf.hasRemaining()) {
            final int read = channel.read(buf, offset);
            if (read < 0) throw new EOFException("Unexpected EOF at offset " + offset);
            offset += read;
        }
    }

    private void cleanUpSwapFile() throws IOException {
        Files.deleteIfExists(this.swapFilePath);
    }

    private void ensureBucketLoaded(int chunkIndex) throws IOException {
        final int bucketIndex = chunkIndex >> BUCKET_SHIFT;
        final Bucket bucket = this.buckets[bucketIndex];

        // bucket lock -> master read lock -> swap write lock
        synchronized (bucket.lock) {
            if (bucket.loaded) {
                return;
            }

            this.masterFileParser.loadBucketsFor(this.masterFilePath, bucketIndex);
            bucket.loaded = true;
        }
    }

    private long markBucketDirty(int chunkIndex) {
        return this.markBucketDirtyByIndex(chunkIndex >> BUCKET_SHIFT);
    }

    private long markBucketDirtyByIndex(int bucketIndex) {
        final Bucket bucket = this.buckets[bucketIndex];

        return bucket.writeEpoch.incrementAndGet();
    }

    private long getBucketWriteEpoch(int bucketIndex) {
        final Bucket bucket = this.buckets[bucketIndex];

        return bucket.writeEpoch.get();
    }

    private long getBucketSyncedEpoch(int bucketIndex) {
        final Bucket bucket = this.buckets[bucketIndex];

        return bucket.syncedEpoch.get();
    }

    private void markBucketSynced(int bucketIndex, long syncedEpoch) {
        final Bucket bucket = this.buckets[bucketIndex];

        bucket.syncedEpoch.accumulateAndGet(syncedEpoch, Math::max);
    }

    private boolean isBucketDirty(int bucketIndex) {
        final Bucket bucket = this.buckets[bucketIndex];

        return bucket.writeEpoch.get() != bucket.syncedEpoch.get();
    }

    public boolean markAsBeingSynced() {
        return BEING_SYNCED_HANDLE.compareAndSet(this, false, true);
    }

    public long getLastWritten() {
        return (long) LAST_WRITTEN_HANDLE.getVolatile(this);
    }

    public boolean shouldSync() {
        return !((boolean) SYNCED_HANDLE.getVolatile(this));
    }

    public boolean softReadLock() {
        // not done close logic yet
        return this.regionObjectLock.readLock().tryLock();
    }

    public void releaseReadLock() {
        this.regionObjectLock.readLock().unlock();
    }

    public boolean isClosedRaw() {
        return (boolean) CLOSED_HANDLE.getVolatile(this);
    }

    public boolean isClosed() {
        this.regionObjectLock.readLock().lock();
        try {
            return (boolean) CLOSED_HANDLE.getVolatile(this);
        } finally {
            this.regionObjectLock.readLock().unlock();
        }
    }

    public void syncIfNeeded() throws IOException {
        // the sync operation is just copying the data from swap file to the master file,
        // so we could simply acquire the read lock and won't block any other read operations
        try {
            // skip if closed already
            if (this.isClosed()) {
                return;
            }

            this.syncToMasterFile();
        } finally {
            BEING_SYNCED_HANDLE.setVolatile(this, false); // mark as not being synced
        }
    }

    private void syncToMasterFile() throws IOException {
        // The dirty flag alone cannot distinguish an in-flight sync from a completed one.
        // Share the master lock with close, including the check of that flag.
        this.masterFileParser.masterFileLock.writeLock().lock();
        try {
            if (this.isClosedRaw() || !SYNCED_HANDLE.compareAndSet(this, false, true)) {
                return;
            }

            try {
                this.masterFileParser.writeMainFileBucketed(this.masterFilePath);
            } catch (Throwable e) {
                SYNCED_HANDLE.setVolatile(this, false);

                throw new IOException("Failed to sync to master file!", e);
            }
        } finally {
            this.masterFileParser.masterFileLock.writeLock().unlock();
        }
    }

    private void initSwapFile() throws IOException {
        this.swapFileChannel = FileChannel.open(
                this.swapFilePath,
                SWAP_FILE_CHANNEL_OPTIONS
        );

        // fill default sectors
        for (int i = 0; i < 1024; i++) {
            this.sectors[i] = new Sector(i, this.headerSize(), 0);
        }
    }

    private void recalculateAcquiredIndex() {
        long newValue = this.headerSize();

        for (Sector sector : this.sectors) {
            if (sector.hasData()) {
                newValue = Math.max(newValue, sector.offset + sector.length);
            }
        }

        this.currentAcquiredIndex = newValue;
    }

    private void writeSwapFileHeaders(boolean forceFile, boolean forceMeta) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(this.headerSize());

        buffer.putLong(SWAP_FILE_SUPER_BLOCK); // Magic
        buffer.put(SWAP_FILE_VERSION); // Version
        buffer.putInt(this.xxHash32Seed); // XXHash32 seed
        buffer.putLong(this.currentAcquiredIndex); // Acquired index

        for (Sector sector : this.sectors) {
            // encode each sector
            buffer.put(sector.getEncoded());
        }

        buffer.flip();

        writeFullyAt(this.swapFileChannel, buffer, 0);

        if (forceFile) {
            this.swapFileChannel.force(forceMeta);
        }
    }

    private int sectorSize() {
        return this.sectors.length * Sector.sizeOfSingle();
    }

    private int headerSize() {
        int result = 0;

        result += Long.BYTES; // Magic
        result += Byte.BYTES; // Version
        result += Integer.BYTES; // XXHash32 seed
        result += Long.BYTES; // Acquired index
        result += this.sectorSize(); // Sectors

        return result;
    }

    private void flushInternal() throws IOException {
        boolean initiallySyncRequired;

        this.regionObjectLock.writeLock().lock();
        try {
            if (this.isClosedRaw()) {
                return;
            }

            long spareSize = this.currentAcquiredIndex;

            spareSize -= this.headerSize();
            for (Sector sector : this.sectors) {
                // skip no data sectors
                if (!sector.hasData()) {
                    continue;
                }

                spareSize -= sector.length;
            }

            long sectorSize = 0;
            for (Sector sector : this.sectors) {
                // skip no data sectors
                if (!sector.hasData()) {
                    continue;
                }

                sectorSize += sector.length;
            }

            final boolean compactRequested = spareSize > SWAP_FILE_AUTO_COMPACT_SIZE && (double) spareSize > ((double) sectorSize) * SWAP_FILE_AUTO_COMPACT_PERCENT;

            // try auto compact to clean the garbage area
            if (compactRequested) {
                // do compact
                this.compactSwapFile();
            }

            // prevent syncing after compact because it could be time costing sometimes
            initiallySyncRequired = !Files.exists(this.masterFilePath) && !compactRequested;
        } finally {
            this.regionObjectLock.writeLock().unlock();
        }

        if (initiallySyncRequired) {
            this.syncToMasterFile();
        }
    }

    private void closeInternal() throws IOException {
        // Match the sync/load lock order: master first, then swap. Wait for in-flight
        // syncs and exclude writes until the final sync and swap deletion both finish.
        this.masterFileParser.masterFileLock.writeLock().lock();
        try {
            this.regionObjectLock.writeLock().lock();
            try {
                this.syncToMasterFile();
                this.markClosed();

                this.swapFileChannel.close();
            } finally {
                this.regionObjectLock.writeLock().unlock();
            }
        } finally {
            this.masterFileParser.masterFileLock.writeLock().unlock();
        }
    }

    private void markClosed() throws IOException {
        if (!CLOSED_HANDLE.compareAndSet(this, false, true)) {
            throw new IOException("Already closed!");
        }

        this.flusher.removeFile(this);
    }

    private void compactSwapFile() throws IOException {
        this.writeSwapFileHeaders(true, true); // save headers for compact

        final Sector[] newSectorsToBeReplaced = new Sector[this.sectors.length];

        for (int i = 0; i < this.sectors.length; i++) {
            final Sector old = this.sectors[i];

            if (old.hasData()) {
                newSectorsToBeReplaced[i] = old;
                continue;
            }

            // note:
            // we reset length to 0 and this would make length <= newLength(which is >= 0) always true,
            // so that the following write operation wouldn't override the data of other sectors
            // see the write method in Sector class
            newSectorsToBeReplaced[i] = new Sector(i, 0, 0);
        }

        long newAcquiredIndex;

        final Path targetTemp = new File(this.swapFilePath.toString() + ".tmp").toPath();

        try (FileChannel tempChannel = FileChannel.open(
                targetTemp,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
                StandardOpenOption.READ,
                StandardOpenOption.TRUNCATE_EXISTING
        )) {
            long offsetPointer = this.headerSize();
            tempChannel.position(offsetPointer);

            for (Sector sector : newSectorsToBeReplaced) {
                // skip cleared or no data-contained sectors
                if (!sector.hasData()) {
                    continue;
                }

                // transfer to target
                sector.transferTo(this.swapFileChannel, tempChannel);

                // recalculate the offset and length
                final Sector newRecalculated = new Sector(sector.index, offsetPointer, sector.length);
                newRecalculated.hasData = true;

                offsetPointer += sector.length;
                newSectorsToBeReplaced[sector.index] = newRecalculated; // update sector infos
            }

            tempChannel.force(true);

            newAcquiredIndex = offsetPointer;
        } catch (Throwable ex) {
            // recalculate acquired index
            this.recalculateAcquiredIndex();
            // delete the target temp file
            Files.deleteIfExists(targetTemp);
            // fast-fail
            // note: we don't block new write operations here as this is recoverable
            throw new IOException("Failed to compact swap file!", ex);
        }

        this.swapFileChannel.close();

        // replace swap file
        try {
            Files.move(
                    targetTemp,
                    this.swapFilePath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (Throwable e) {
            // atomic move might be unsupported on some file systems, so give it an attempt to retry without atomic move
            try {
                Files.move(
                        targetTemp,
                        this.swapFilePath,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (Throwable ex) {
                // now we are totally failed
                e.addSuppressed(ex);

                // delete file that failed to replace
                Files.deleteIfExists(targetTemp);
                // recalculate acquired index
                this.recalculateAcquiredIndex();
                // reopen closed channel
                this.reopenSwapFileChannel();
                // fast-fail
                this.markClosed(); // prevent new writing & sync operations
                throw new IOException("Failed to replace original swap file!", e);
            }
        }

        try {
            // reopen file channel
            this.reopenSwapFileChannel();

            // replace with recalculated file headers
            this.sectors = newSectorsToBeReplaced;
            this.currentAcquiredIndex = newAcquiredIndex;

            // flush to file
            this.writeSwapFileHeaders(true, true);
        } catch (Throwable ex) {
            // we are totally failed here,
            // directly mark as closed as the swap file is already replaced, and we failed to update the
            // data which is still in the memory
            //
            // which means we might write any data into any incorrect indexed sectors which will blow the whole data
            this.markClosed();
            throw new IOException(ex);
        }
    }

    private void reopenSwapFileChannel() throws IOException {
        if (this.swapFileChannel.isOpen()) {
            this.swapFileChannel.close();
        }

        this.swapFileChannel = FileChannel.open(
                this.swapFilePath,
                SWAP_FILE_CHANNEL_OPTIONS
        );
    }

    private void writeChunkDataRaw(int index, ByteBuffer chunkData, boolean skipSync) throws IOException {
        final ByteBuffer committed = this.compressingOps.compress(chunkData); // run compression out of lock

        this.regionObjectLock.writeLock().lock();
        try {
            if (this.isClosedRaw()) {
                throw new IOException("Already closed!");
            }
            final Sector sector = this.sectors[index];

            sector.store(committed, this.swapFileChannel);
            if (!skipSync) {
                this.markBucketDirty(index);
                this.markAsToSync();
            }
        } finally {
            this.regionObjectLock.writeLock().unlock();
        }
    }

    private @Nullable ByteBuffer readChunkDataRaw(int index) throws IOException {
        return this.readChunkDataRaw(index, true);
    }

    private @Nullable ByteBuffer readChunkDataRaw(int index, boolean acquireLock) throws IOException {
        final ByteBuffer raw;

        this.regionObjectLock.readLock().lock();
        try {
            final Sector sector = this.sectors[index];

            if (!sector.hasData()) {
                return null;
            }

            raw = sector.read(this.swapFileChannel);
        } finally {
            this.regionObjectLock.readLock().unlock();
        }

        return this.compressingOps.decompress(raw);
    }

    private void clearChunkData(int index) throws IOException {
        this.ensureBucketLoaded(index);

        this.regionObjectLock.writeLock().lock();
        try {
            if (this.isClosedRaw()) {
                throw new IOException("Already closed!");
            }
            final Sector sector = this.sectors[index];

            sector.clear();
            this.markBucketDirty(index);
            this.markAsToSync();
        } finally {
            this.regionObjectLock.writeLock().unlock();
        }
    }

    private void markAsToSync() {
        SYNCED_HANDLE.setVolatile(this, false); // mark as unsynced
        LAST_WRITTEN_HANDLE.setVolatile(this, System.nanoTime()); // update last written time
    }

    private static int getChunkIndex(int x, int z) {
        return (x & 31) + ((z & 31) << 5);
    }

    private boolean hasData(int index) throws IOException {
        this.ensureBucketLoaded(index);

        this.regionObjectLock.readLock().lock();
        try {
            return this.sectors[index].hasData();
        } finally {
            this.regionObjectLock.readLock().unlock();
        }
    }

    private void writeChunk(int x, int z, @NotNull ByteBuffer data) throws IOException {
        final int chunkIndex = getChunkIndex(x, z);

        if (data.remaining() > RegionFile.MAX_CHUNK_SIZE) {
            throw new RegionFile.RegionFileSizeException("Writing too large chunk, limit : " + RegionFile.MAX_CHUNK_SIZE + " but got : " + data.remaining());
        }

        final int oldPositionOfData = data.position();
        final int xxHash32OfData = this.xxHash32.hash(data, this.xxHash32Seed);
        data.position(oldPositionOfData);

        // uncompressed length(int) + timestamp(long) + xxhash32(int)
        final ByteBuffer chunkSectionBuilder = ByteBuffer.allocate(data.remaining() + 4 + 8 + 4);

        chunkSectionBuilder.putInt(data.remaining()); // Length(int)
        chunkSectionBuilder.putLong(System.currentTimeMillis()); // Timestamp(long)
        chunkSectionBuilder.putInt(xxHash32OfData); // xxHash32 of the original data(int)
        chunkSectionBuilder.put(data); // Data(bytes)
        chunkSectionBuilder.flip();

        this.writeChunkDataRaw(chunkIndex, chunkSectionBuilder, false);
    }

    private @Nullable ByteBuffer readChunk(int x, int z) throws IOException {
        final int chunkIndex = getChunkIndex(x, z);

        this.ensureBucketLoaded(chunkIndex);

        final ByteBuffer data = this.readChunkDataRaw(chunkIndex);

        if (data == null) {
            return null;
        }

        final int length = data.getInt(); // length(int)
        final long timestamp = data.getLong(); // timestamp(long)
        final int dataXXHash32 = data.getInt(); // XXHash32 for validation(int)

        final IOException xxHash32CheckFailedEx = this.checkXXHash32(dataXXHash32, data);
        if (xxHash32CheckFailedEx != null) {
            throw xxHash32CheckFailedEx; // prevent from loading
        }

        return data;
    }

    private @Nullable IOException checkXXHash32(long originalXXHash32, @NotNull ByteBuffer input) {
        final int oldPositionOfInput = input.position();
        final int currentXXHash32 = this.xxHash32.hash(input, this.xxHash32Seed);
        input.position(oldPositionOfInput);

        if (originalXXHash32 != currentXXHash32) {
            return new IOException("XXHash32 check failed ! Expected: " + originalXXHash32 + ",but got: " + currentXXHash32);
        }

        return null;
    }

    public Path getPath() {
        return this.masterFilePath;
    }

    /**
     * Reads the raw chunk payload (5-byte header + compressed NBT) and decodes it into a
     * data input stream, sharing the exact decode logic with MCA region files.
     *
     * @param pos The position of the chunk within the region file.
     * @return A DataInputStream for the chunk's data if it exists and is valid, or null otherwise.
     * @throws IOException If an I/O error occurs while reading.
     */
    @Nullable
    public DataInputStream getChunkDataInputStream(ChunkPos pos) throws IOException {
        final ByteBuffer data = this.readChunk(pos.x(), pos.z());

        if (data == null) {
            return null;
        }

        return RegionFile.decodeChunkData(pos, data);
    }

    public boolean doesChunkExist(@NotNull ChunkPos pos) throws IOException {
        return this.hasData(getChunkIndex(pos.x(), pos.z()));
    }

    public void clear(@NotNull ChunkPos pos) throws IOException {
        this.clearChunkData(getChunkIndex(pos.x(), pos.z()));
    }

    public boolean hasChunk(@NotNull ChunkPos pos) throws IOException {
        return this.hasData(getChunkIndex(pos.x(), pos.z()));
    }

    /**
     * Writes an already encoded chunk payload (see {@link RegionFile#encodeChunkData}).
     *
     * @param pos The position of the chunk within the region file.
     * @param data The encoded chunk bytes (5-byte header + compressed NBT).
     * @throws IOException If an I/O error occurs while writing.
     */
    public void writeChunkData(@NotNull ChunkPos pos, byte @NotNull [] data) throws IOException {
        final int chunkIndex = getChunkIndex(pos.x(), pos.z());

        this.ensureBucketLoaded(chunkIndex);

        this.writeChunk(pos.x(), pos.z(), ByteBuffer.wrap(data));
    }

    public void flush() throws IOException {
        this.flushInternal();
    }

    @Override
    public void close() throws IOException {
        this.closeInternal();
    }

    // here we use this tool to prevent the swap file from going too large
    // sometimes when a region contains all chunks, it might be very huge without any compressions(around 100MiB)
    private static class CompressingOps {
        private final LZ4Compressor lz4Compressor = LZ4Factory.fastestInstance().fastCompressor();
        private final LZ4FastDecompressor lz4Decompressor = LZ4Factory.fastestInstance().fastDecompressor();

        public @NotNull ByteBuffer compress(@NotNull ByteBuffer in) {
            final int bufferLenToAllocate = this.lz4Compressor.maxCompressedLength(in.remaining());
            final ByteBuffer result = ByteBuffer.allocate(bufferLenToAllocate + 4);

            result.putInt(in.remaining());
            this.lz4Compressor.compress(in, result);

            return result.flip();
        }

        public @NotNull ByteBuffer decompress(@NotNull ByteBuffer flippedIn) {
            final int originalLen = flippedIn.getInt();
            final byte[] raw = new byte[flippedIn.remaining()];
            flippedIn.get(raw);

            final byte[] decompressed = new byte[originalLen];
            this.lz4Decompressor.decompress(raw, decompressed);

            return ByteBuffer.wrap(decompressed);
        }
    }

    public class Sector {
        private final int index;
        private long offset;
        private long length;
        private boolean hasData = false;

        private Sector(int index, long offset, long length) {
            this.index = index;
            this.offset = offset;
            this.length = length;
        }

        public void transferTo(@NotNull FileChannel source, @NotNull FileChannel target) throws IOException {
            long transferred = 0;
            while (transferred < this.length) {
                transferred += source.transferTo(
                        this.offset + transferred,
                        this.length - transferred,
                        target);
            }
        }

        public @NotNull ByteBuffer read(@NotNull FileChannel channel) throws IOException {
            final ByteBuffer result = ByteBuffer.allocate((int) this.length);

            readFullyAt(channel, result, this.offset);

            result.flip();
            return result;
        }

        public void store(@NotNull ByteBuffer newData, @NotNull FileChannel channel) throws IOException {
            final long oldLength = this.length;
            final long newDataLength = newData.remaining();

            this.hasData = true;
            this.length = newDataLength;

            // data is smaller or its length equals to the local buffer we hold, write it directly
            if (newDataLength <= oldLength) {
                writeFullyAt(channel, newData, this.offset);

                return;
            }

            // or we will append to the end of file
            this.offset = BufferedLinearRegionFile.this.currentAcquiredIndex;

            BufferedLinearRegionFile.this.currentAcquiredIndex += this.length;

            writeFullyAt(channel, newData, this.offset);
        }

        private @NotNull ByteBuffer getEncoded() {
            final ByteBuffer buffer = ByteBuffer.allocate(sizeOfSingle());

            buffer.putLong(this.offset);
            buffer.putLong(this.length);
            buffer.put((byte) (this.hasData ? 1 : 0));
            buffer.flip();

            return buffer;
        }

        public void restoreFrom(@NotNull ByteBuffer buffer) {
            this.offset = buffer.getLong();
            this.length = buffer.getLong();
            this.hasData = buffer.get() == 1;

            if (this.length < 0 || this.offset < 0) {
                throw new IllegalStateException("Invalid sector data: " + this);
            }
        }

        public void clear() {
            this.hasData = false;
        }

        public boolean hasData() {
            return this.hasData;
        }

        static int sizeOfSingle() {
            //     offset + length  hasData
            return Long.BYTES * 2 + 1;
        }
    }

    private final class LinearMasterFileParser {
        // V3 new format layout:
        //   [0,  14): header  — superblock(8) + version(1) + compressionLevel(1) + xxHash32Seed(4)
        //   [14, 142): position table — BUCKET_COUNT(16) × long(8) each; 0 = no data for that bucket
        //   [526, EOF): bucket data — originalLen(int) + compressedLen(int) + compressedData
        private static final long V3_POS_TABLE_OFFSET = 14L;
        private static final int V3_POS_TABLE_SIZE = BUCKET_COUNT * Long.BYTES; // 128
        private static final long V3_DATA_AREA_OFFSET = V3_POS_TABLE_OFFSET + V3_POS_TABLE_SIZE; // 142

        private final ReadWriteLock masterFileLock = new ReentrantReadWriteLock();

        public void writeMainFileBucketed(@NotNull Path mainFile) throws IOException {
            final Path tmpFilePath = Path.of(mainFile + ".tmp");
            final long[] syncedBucketEpochs = new long[BUCKET_COUNT];
            final long[] newPositionTable = new long[BUCKET_COUNT];

            // Open old file to copy non-dirty buckets
            long[] oldPositionTable = null;
            FileChannel oldChannel = null;

            this.masterFileLock.writeLock().lock();
            try {
                if (Files.exists(mainFile)) {
                    try {
                        oldChannel = FileChannel.open(mainFile, StandardOpenOption.READ);
                        if (oldChannel.size() >= V3_DATA_AREA_OFFSET) {
                            final ByteBuffer hdr = ByteBuffer.allocate(14);
                            readFullyAt(oldChannel, hdr, 0);
                            hdr.flip();
                            if (hdr.getLong() == MASTER_FILE_SUPER_BLOCK && hdr.get() == MASTER_FILE_VERSION_BUCKET) {
                                oldPositionTable = this.parseOffsetTable(oldChannel);
                            } else {
                                oldChannel.close();
                                oldChannel = null;
                            }
                        } else {
                            oldChannel.close();
                            oldChannel = null;
                        }
                    } catch (Throwable e) {
                        if (oldChannel != null) {
                            try {
                                oldChannel.close();
                            } catch (IOException e2) {
                                e.addSuppressed(e2);
                            }
                        }

                        throw new RuntimeException(e);
                    }
                }

                BufferedLinearRegionFile.this.regionObjectLock.readLock().lock();
                try (FileChannel outChannel = FileChannel.open(tmpFilePath,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {

                    // Write header (14 bytes)
                    final ByteBuffer header = ByteBuffer.allocate(14);
                    header.putLong(MASTER_FILE_SUPER_BLOCK);
                    header.put(MASTER_FILE_VERSION_BUCKET);
                    header.put((byte) COMPRESSION_LEVEL); // compressionLevel
                    header.putInt(BufferedLinearRegionFile.this.xxHash32Seed);
                    header.flip();
                    writeFullyAt(outChannel, header, 0);

                    // Write position table placeholder (all zeros, filled in at the end)
                    writeFullyAt(outChannel, ByteBuffer.allocate(V3_POS_TABLE_SIZE), V3_POS_TABLE_OFFSET);

                    long dataOffset = V3_DATA_AREA_OFFSET;

                    for (int bucketIndex = 0; bucketIndex < BUCKET_COUNT; bucketIndex++) {
                        final long bucketWriteEpoch = BufferedLinearRegionFile.this.getBucketWriteEpoch(bucketIndex);
                        final boolean isBucketDirty = bucketWriteEpoch != BufferedLinearRegionFile.this.getBucketSyncedEpoch(bucketIndex);

                        if (isBucketDirty) {
                            final int baseChunk = bucketIndex << BUCKET_SHIFT;
                            final ByteArrayOutputStream rawBuf = new ByteArrayOutputStream();
                            final DataOutputStream rawOut = new DataOutputStream(rawBuf);
                            boolean hasAny = false;

                            for (int i = 0; i < BUCKET_SIZE; i++) {
                                // swap read lock
                                final ByteBuffer data = BufferedLinearRegionFile.this.readChunkDataRaw(baseChunk + i, false);

                                // note: null -> no data contained
                                if (data == null) {
                                    rawOut.writeInt(0);
                                } else {
                                    final byte[] arr = new byte[data.remaining()];
                                    data.get(arr);
                                    rawOut.writeInt(arr.length);
                                    rawOut.write(arr);
                                    hasAny = true;
                                }
                            }
                            rawOut.flush();

                            if (hasAny) {
                                final byte[] raw = rawBuf.toByteArray();
                                final byte[] compressed = CompressionMethod.zstdCompress(raw, COMPRESSION_LEVEL);

                                newPositionTable[bucketIndex] = dataOffset;

                                final ByteBuffer bucketBuf = ByteBuffer.allocate(8 + compressed.length);
                                bucketBuf.putInt(raw.length);        // original (uncompressed) length
                                bucketBuf.putInt(compressed.length); // compressed length
                                bucketBuf.put(compressed);
                                bucketBuf.flip();
                                writeFullyAt(outChannel, bucketBuf, dataOffset);
                                dataOffset += bucketBuf.limit();
                            }
                            // else: newPositionTable[bucketIndex] stays 0

                            syncedBucketEpochs[bucketIndex] = bucketWriteEpoch;
                        } else {
                            // Not dirty: copy bytes from old file if available
                            if (oldPositionTable != null && oldPositionTable[bucketIndex] != 0) {
                                final long oldOffset = oldPositionTable[bucketIndex];
                                final ByteBuffer lensBuf = ByteBuffer.allocate(8);
                                readFullyAt(oldChannel, lensBuf, oldOffset);
                                lensBuf.flip();
                                lensBuf.getInt(); // skip originalLen
                                final int compressedLen = lensBuf.getInt();

                                final long bucketTotalSize = 8L + compressedLen;
                                newPositionTable[bucketIndex] = dataOffset;
                                outChannel.position(dataOffset);
                                long remaining = bucketTotalSize;
                                long srcPos = oldOffset;
                                while (remaining > 0) {
                                    final long transferred = oldChannel.transferTo(srcPos, remaining, outChannel);
                                    srcPos += transferred;
                                    remaining -= transferred;
                                }
                                dataOffset += bucketTotalSize;
                            }
                        }
                    }

                    // Write the finalized position table
                    final ByteBuffer posTableBuf = ByteBuffer.allocate(V3_POS_TABLE_SIZE);
                    for (final long pos : newPositionTable) {
                        posTableBuf.putLong(pos);
                    }
                    posTableBuf.flip();
                    writeFullyAt(outChannel, posTableBuf, V3_POS_TABLE_OFFSET);

                    outChannel.force(true);
                } finally {
                    BufferedLinearRegionFile.this.regionObjectLock.readLock().unlock();

                    if (oldChannel != null) {
                        oldChannel.close();
                    }
                }

                try {
                    Files.move(tmpFilePath, mainFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (Throwable e) {
                    try {
                        Files.move(tmpFilePath, mainFile, StandardCopyOption.REPLACE_EXISTING);
                    } catch (Throwable ex) {
                        Files.deleteIfExists(tmpFilePath);

                        e.addSuppressed(ex);

                        throw new IOException("Failed to replace master file!", e);
                    }
                }
            } finally {
                this.masterFileLock.writeLock().unlock();
            }

            for (int i = 0; i < syncedBucketEpochs.length; i++) {
                final long syncedEpoch = syncedBucketEpochs[i];
                if (syncedEpoch != 0L) {
                    BufferedLinearRegionFile.this.markBucketSynced(i, syncedEpoch);
                }
            }
        }

        private void loadBucketsFor(Path file, int bucketIndex) throws IOException {
            final int beginChunkIndex = bucketIndex << BUCKET_SHIFT;

            this.masterFileLock.readLock().lock();

            try {
                if (!Files.exists(file)) {
                    return;
                }

                try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
                    if (channel.size() < V3_DATA_AREA_OFFSET) {
                        return;
                    }

                    final ByteBuffer headerBuf = ByteBuffer.allocate(14);
                    readFullyAt(channel, headerBuf, 0);
                    headerBuf.flip();

                    final long superblock = headerBuf.getLong();
                    if (superblock != MASTER_FILE_SUPER_BLOCK)
                        throw new IOException("Invalid superblock " + superblock + "!");

                    final byte version = headerBuf.get();
                    if (version != MASTER_FILE_VERSION_BUCKET)
                        throw new IOException("Unknown version: " + version);

                    // compressionLevel and hashSeed consumed but not used here
                    headerBuf.get();
                    headerBuf.getInt();

                    final long[] posTable = this.parseOffsetTable(channel);

                    // New format: jump directly to bucket data
                    final long bucketDataOffset = posTable[bucketIndex];
                    if (bucketDataOffset == 0) return;

                    final ByteBuffer lensBuf = ByteBuffer.allocate(8);
                    readFullyAt(channel, lensBuf, bucketDataOffset);
                    lensBuf.flip();
                    final int originalLen = lensBuf.getInt();
                    final int compressedLen = lensBuf.getInt();

                    final byte[] compressedData = new byte[compressedLen];
                    readFullyAt(channel, ByteBuffer.wrap(compressedData), bucketDataOffset + 8);

                    final ByteBuffer decompressed = ByteBuffer.wrap(CompressionMethod.zstdDecompress(compressedData, originalLen));
                    this.loadChunksFromBucketData(decompressed, beginChunkIndex);
                }
            } finally {
                this.masterFileLock.readLock().unlock();
            }
        }

        private long @NotNull [] parseOffsetTable(FileChannel channel) throws IOException {
            final ByteBuffer buf = ByteBuffer.allocate(V3_POS_TABLE_SIZE);
            readFullyAt(channel, buf, V3_POS_TABLE_OFFSET);
            buf.flip();

            final long[] table = new long[BUCKET_COUNT];

            Arrays.fill(table, 0L);
            for (int i = 0; i < BUCKET_COUNT; i++) {
                table[i] = buf.getLong();
            }

            return table;
        }

        private void loadChunksFromBucketData(ByteBuffer decompressed, int beginChunkIndex) throws IOException {
            for (int chunkIndex = beginChunkIndex; chunkIndex < beginChunkIndex + BUCKET_SIZE; chunkIndex++) {
                final int chunkSectionDataSize = decompressed.getInt();
                if (chunkSectionDataSize <= 0) continue;

                final byte[] chunkSectionData = new byte[chunkSectionDataSize];
                decompressed.get(chunkSectionData);

                BufferedLinearRegionFile.this.writeChunkDataRaw(chunkIndex, ByteBuffer.wrap(chunkSectionData), true);
            }
        }
    }
}
