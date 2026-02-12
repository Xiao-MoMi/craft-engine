package net.momirealms.craftengine.core.world.chunk.storage;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.logger.PluginLogger;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.NBT;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public final class RegionFile implements AutoCloseable {
    private static final PluginLogger LOGGER = CraftEngine.instance().logger();
    private static final byte FORMAT_VERSION = 1;
    public static final int SECTOR_BYTES = 4096;
    public static final int CHUNK_HEADER_SIZE = 5;
    public static final int EXTERNAL_STREAM_FLAG = 128;
    public static final int EXTERNAL_CHUNK_THRESHOLD = 256;
    public static final int MAX_CHUNK_SIZE = 500 * 1024 * 1024;
    public static final int INFO_NOT_PRESENT = 0;

    public static final String EXTERNAL_FILE_SUFFIX = ".mcc";
    public static final String EXTERNAL_FILE_PREFIX = "c.";

    private static final ByteBuffer PADDING_BUFFER = ByteBuffer.allocateDirect(1);

    private final FileChannel fileChannel;
    private final Path directory;
    private final CompressionMethod compression;
    private final ByteBuffer header;
    private final IntBuffer sectorInfo;
    private final IntBuffer timestamps;
    private final RegionBitmap usedSectors;
    public final ReentrantLock fileLock = new ReentrantLock(true);
    public final Path regionFile;

    private static final List<Function<DataInputStream, DataInputStream>> FORMAT_UPDATER = List.of(
            // version 0 -> 1 (Use nameless compound tag)
            (old) -> {
                try {
                    CompoundTag tag = NBT.readCompound(new DataInputStream(old), true);
                    return new DataInputStream(new ByteArrayInputStream(NBT.toBytes(tag)));
                } catch (IOException e) {
                    CraftEngine.instance().logger().warn("Failed to migrate data from version 0 -> 1", e);
                    return null;
                }
            }
    );

    public RegionFile(Path path, Path directory, CompressionMethod compressionMethod) throws IOException {
        this.header = ByteBuffer.allocateDirect(8192);
        this.regionFile = path;
        this.usedSectors = new RegionBitmap();
        this.compression = compressionMethod;
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Expected directory, got " + directory.toAbsolutePath());
        }
        this.directory = directory;
        this.sectorInfo = this.header.asIntBuffer();
        this.sectorInfo.limit(1024);
        this.header.position(4096);
        this.timestamps = this.header.asIntBuffer();
        this.fileChannel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
        this.usedSectors.allocate(0, 2);
        this.header.position(0);
        int byteAmount = this.fileChannel.read(this.header, 0L);
        if (byteAmount == -1) {
            return;
        }
        if (byteAmount != 8192) {
            LOGGER.warn(String.format("Region file %s has truncated header: %s", path, byteAmount));
        }
        long regionSize = Files.size(path);
        for (int chunkLocation = 0; chunkLocation < 32 * 32; ++chunkLocation) {
            int sectorInfo = this.sectorInfo.get(chunkLocation);
            if (sectorInfo != 0) {
                int offset = unpackSectorOffset(sectorInfo);
                int size = unpackSectorSize(sectorInfo);
                if (offset < 2) {
                    LOGGER.warn(String.format("Region file %s has invalid sector at index: %s; sector %s overlaps with header", path, chunkLocation, offset));
                    this.sectorInfo.put(chunkLocation, 0);
                } else if (size == 0) {
                    LOGGER.warn(String.format("Region file %s has an invalid sector at index: %s; size has to be > 0", path, chunkLocation));
                    this.sectorInfo.put(chunkLocation, 0);
                } else if ((long) offset * 4096L > regionSize) {
                    LOGGER.warn(String.format("Region file %s has an invalid sector at index: %s; sector %s is out of bounds", path, chunkLocation, offset));
                    this.sectorInfo.put(chunkLocation, 0);
                } else {
                    this.usedSectors.allocate(offset, size);
                }
            }
        }
    }

    /**
     * Retrieves the data input stream for a given chunk. The method reads and processes the
     * chunk's header, checks for potential errors, and returns a valid input stream for the chunk data.
     *
     * @param pos The position of the chunk within the region file.
     * @return A DataInputStream for the chunk's data if it exists and is valid, or null if there is an error.
     * @throws IOException If an I/O error occurs while reading from the file.
     */
    @Nullable
    public synchronized DataInputStream getChunkDataInputStream(ChunkPos pos) throws IOException {
        int sectorInfo = this.getSectorInfo(pos);
        // If no sector information is found (sectorInfo == 0), return null (indicating chunk doesn't exist).
        if (sectorInfo == INFO_NOT_PRESENT) {
            return null;
        }

        int sectorOffset = RegionFile.unpackSectorOffset(sectorInfo);
        int sectorSize = RegionFile.unpackSectorSize(sectorInfo);

        // Calculate the total size of the chunk data in bytes (sectorSize * 4096 bytes per sector).
        int totalSize = sectorSize * SECTOR_BYTES;
        ByteBuffer bytebuffer = ByteBuffer.allocate(totalSize);
        this.fileChannel.read(bytebuffer, (long) sectorOffset * SECTOR_BYTES);
        ((Buffer) bytebuffer).flip();

        // If the buffer has less than 5 bytes, the chunk's header is corrupted (or truncated).
        if (bytebuffer.remaining() < 5) {
            LOGGER.severe(String.format("Chunk %s header is truncated: expected %s but read %s", pos, totalSize, bytebuffer.remaining()));
            return null;
        }

        // Read the chunk's stub information
        int size = bytebuffer.getInt();
        byte flags = bytebuffer.get();
        byte compressionScheme = (byte) (flags & 0b00000111);
        byte version = (byte) ((flags & 0b01111000) >>> 3);

        if (size == 0) {
            LOGGER.warn(String.format("Chunk %s is allocated, but stream is missing", pos));
            return null;
        }

        // Calculate the actual data size
        int actualSize = size - 1;
        if (RegionFile.isExternalStreamChunk(flags)) {
            // If the chunk has both internal and external streams, log a warning.
            if (actualSize != 0) {
                LOGGER.warn("Chunk has both internal and external streams");
            }
            // Create and return an input stream for the external chunk.
            if (version == FORMAT_VERSION) {
                return this.createExternalChunkInputStream(pos, RegionFile.getExternalChunkVersion(compressionScheme));
            } else {
                int currentVersion = version;
                DataInputStream inputStream = this.createExternalChunkInputStream(pos, RegionFile.getExternalChunkVersion(compressionScheme));
                while (currentVersion < FORMAT_VERSION) {
                    inputStream = FORMAT_UPDATER.get(currentVersion).apply(inputStream);
                    if (inputStream == null) break;
                    currentVersion++;
                }
                return inputStream;
            }
        } else if (actualSize > bytebuffer.remaining()) {
            // If the declared size of the chunk is greater than the remaining bytes in the buffer, the stream is truncated.
            LOGGER.severe(String.format("Chunk %s stream is truncated: expected %s but read %s", pos, actualSize, bytebuffer.remaining()));
            return null;
        } else if (actualSize < 0) {
            // If the declared chunk size is negative, log an error.
            LOGGER.severe(String.format("Declared size %s of chunk %s is negative", size, pos));
            return null;
        } else {
            if (version == FORMAT_VERSION) {
                // Otherwise, create and return a standard input stream for the chunk data.
                return this.createChunkInputStream(pos, compressionScheme, RegionFile.createInputStream(bytebuffer, actualSize));
            } else {
                int currentVersion = version;
                DataInputStream inputStream = this.createChunkInputStream(pos, compressionScheme, RegionFile.createInputStream(bytebuffer, actualSize));
                while (currentVersion < FORMAT_VERSION) {
                    inputStream = FORMAT_UPDATER.get(currentVersion).apply(inputStream);
                    if (inputStream == null) break;
                    currentVersion++;
                }
                return inputStream;
            }
        }
    }

    public static byte encodeFlag(byte compressionScheme, byte version, boolean external) {
        if (compressionScheme <= 0 || compressionScheme > 7) {
            throw new IllegalArgumentException("compression method can only be a number between 1 and 7");
        }
        if (version < 0 || version > 15) {
            throw new IllegalArgumentException("Version number can only be a number between 0 and 15");
        }
        return (byte) (
                (external ? 0b10000000 : 0) |
                        ((version & 0b00001111) << 3) |
                        (compressionScheme & 0b00000111)
        );
    }

    private static int getTimestamp() {
        return (int) (Instant.now().toEpochMilli() / 1000L);
    }

    private static boolean isExternalStreamChunk(byte flags) {
        return (flags & EXTERNAL_STREAM_FLAG) != 0;
    }

    private static byte getExternalChunkVersion(byte flags) {
        return (byte) (flags & -129);
    }

    @Nullable
    private DataInputStream createChunkInputStream(ChunkPos pos, byte flags, InputStream stream) throws IOException {
        CompressionMethod compressionMethod = CompressionMethod.fromId(flags);
        if (compressionMethod == null) {
            LOGGER.severe(String.format("Chunk %s has invalid chunk stream version %s", pos, flags));
            return null;
        } else {
            return new DataInputStream(compressionMethod.wrap(stream));
        }
    }

    @Nullable
    private DataInputStream createExternalChunkInputStream(ChunkPos pos, byte flags) throws IOException {
        Path path = this.getExternalChunkPath(pos);
        if (!Files.isRegularFile(path)) {
            LOGGER.severe(String.format("External chunk path %s is not file", path));
            return null;
        } else {
            return this.createChunkInputStream(pos, flags, Files.newInputStream(path));
        }
    }

    private static ByteArrayInputStream createInputStream(ByteBuffer buffer, int length) {
        return new ByteArrayInputStream(buffer.array(), buffer.position(), length);
    }

    private int packSectorOffset(int offset, int size) {
        return offset << 8 | size;
    }

    private static int unpackSectorSize(int sectorData) {
        return sectorData & 0xFF;
    }

    private static int unpackSectorOffset(int sectorData) {
        return (sectorData >> 8) & 0xFFFFFF;
    }

    private static int sizeToSectors(int byteCount) {
        return (byteCount + SECTOR_BYTES - 1) / SECTOR_BYTES;
    }

    /**
     * Checks whether a chunk exists at the specified position.
     * The method verifies if the chunk's data is valid and can be accessed.
     *
     * @param pos The position of the chunk within the region file.
     * @return True if the chunk exists and is valid, false otherwise.
     */
    public synchronized boolean doesChunkExist(ChunkPos pos) {
        int sectorInfo = this.getSectorInfo(pos);
        if (sectorInfo == INFO_NOT_PRESENT) {
            return false;
        }
        int sectorOffset = unpackSectorOffset(sectorInfo);
        int sectorSize = unpackSectorSize(sectorInfo);
        ByteBuffer bytebuffer = ByteBuffer.allocate(CHUNK_HEADER_SIZE);
        try {
            this.fileChannel.read(bytebuffer, (long) sectorOffset * SECTOR_BYTES);
            ((Buffer) bytebuffer).flip();
            if (bytebuffer.remaining() != CHUNK_HEADER_SIZE) {
                return false;
            }
            int size = bytebuffer.getInt();
            byte type = bytebuffer.get();
            if (isExternalStreamChunk(type)) {
                if (!CompressionMethod.isValid(getExternalChunkVersion(type)))
                    return false;
                return Files.isRegularFile(this.getExternalChunkPath(pos));
            } else {
                if (!CompressionMethod.isValid(type))
                    return false;
                if (size == 0)
                    return false;
                int actualSize = size - 1;
                return actualSize >= 0 && actualSize <= SECTOR_BYTES * sectorSize;
            }
        } catch (IOException ioexception) {
            return false;
        }
    }

    public DataOutputStream getChunkDataOutputStream(ChunkPos pos) throws IOException {
        return new DataOutputStream(this.compression.wrap(new ChunkBuffer(pos)));
    }

    public void flush() throws IOException {
        this.fileChannel.force(true);
    }

    public void clear(ChunkPos pos) throws IOException {
        int chunkLocation = RegionFile.getChunkLocation(pos);
        int sectorInfo = this.sectorInfo.get(chunkLocation);
        if (sectorInfo != INFO_NOT_PRESENT) {
            this.sectorInfo.put(chunkLocation, 0);
            this.timestamps.put(chunkLocation, RegionFile.getTimestamp());
            this.writeHeader();
            Files.deleteIfExists(this.getExternalChunkPath(pos));
            this.usedSectors.free(RegionFile.unpackSectorOffset(sectorInfo), RegionFile.unpackSectorSize(sectorInfo));
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected synchronized void write(ChunkPos pos, ByteBuffer buf) throws IOException {
        // get old offset info
        int offsetIndex = RegionFile.getChunkLocation(pos);
        int previousSectorInfo = this.sectorInfo.get(offsetIndex);
        int previousSectorOffset = RegionFile.unpackSectorOffset(previousSectorInfo);
        int previousSectorSize = RegionFile.unpackSectorSize(previousSectorInfo);
        // count the sectors to write
        int sizeToWrite = buf.remaining();
        int sectorsToWrite = RegionFile.sizeToSectors(sizeToWrite);
        int sectorStartPosition;
        CommitOp regionFileOperation;
        // If the chunk file is too large, store it as an additional file
        if (sectorsToWrite >= EXTERNAL_CHUNK_THRESHOLD) {
            Path path = this.getExternalChunkPath(pos);
            LOGGER.warn(String.format("Saving oversized chunk %s (%s bytes) to external file %s", pos.x() + "," + pos.z(), sizeToWrite, path));
            sectorsToWrite = 1;
            sectorStartPosition = this.usedSectors.allocate(sectorsToWrite);
            regionFileOperation = this.writeToExternalFileSafely(path, buf);
            ByteBuffer externalBuf = this.createExternalHeader();
            this.fileChannel.write(externalBuf, (long) sectorStartPosition * SECTOR_BYTES);
        } else {
            sectorStartPosition = this.usedSectors.allocate(sectorsToWrite);
            // delete external chunk
            regionFileOperation = () -> Files.deleteIfExists(this.getExternalChunkPath(pos));
            this.fileChannel.write(buf, (long) sectorStartPosition * SECTOR_BYTES);
        }
        this.sectorInfo.put(offsetIndex, this.packSectorOffset(sectorStartPosition, sectorsToWrite));
        this.timestamps.put(offsetIndex, RegionFile.getTimestamp());
        this.writeHeader();
        regionFileOperation.run();
        // clear old data
        if (previousSectorOffset != 0) {
            this.usedSectors.free(previousSectorOffset, previousSectorSize);
        }
    }

    private ByteBuffer createExternalHeader() {
        return this.createExternalHeader(this.compression);
    }

    private ByteBuffer createExternalHeader(CompressionMethod compression) {
        ByteBuffer bytebuffer = ByteBuffer.allocate(CHUNK_HEADER_SIZE);
        bytebuffer.putInt(1);
        bytebuffer.put(encodeFlag((byte) compression.getId(), FORMAT_VERSION, true));
        //bytebuffer.put((byte) (compression.getId() | EXTERNAL_STREAM_FLAG));
        ((Buffer) bytebuffer).flip();
        return bytebuffer;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private RegionFile.CommitOp writeToExternalFileSafely(Path destination, ByteBuffer buf) throws IOException {
        Path tempFile = Files.createTempFile(this.directory, "tmp", null);
        FileChannel filechannel = FileChannel.open(tempFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            ((Buffer) buf).position(CHUNK_HEADER_SIZE);
            filechannel.write(buf);
        } catch (Throwable t1) {
            if (filechannel != null) {
                try {
                    filechannel.close();
                } catch (Throwable t2) {
                    t1.addSuppressed(t2);
                }
            }
            throw t1;
        }
        filechannel.close();
        return () -> Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void writeHeader() throws IOException {
        this.header.position(0);
        this.fileChannel.write(this.header, 0L);
    }

    private int getSectorInfo(ChunkPos pos) {
        return this.sectorInfo.get(RegionFile.getChunkLocation(pos));
    }

    public boolean hasChunk(ChunkPos pos) {
        return this.getSectorInfo(pos) != INFO_NOT_PRESENT;
    }

    private static int getChunkLocation(ChunkPos pos) {
        return pos.regionLocalX() + pos.regionLocalZ() * 32;
    }

    public void close() throws IOException {
        this.fileLock.lock();
        synchronized (this) {
            try {
                try {
                    this.padToFullSector();
                } finally {
                    try {
                        this.fileChannel.force(true);
                    } finally {
                        this.fileChannel.close();
                    }
                }
            } finally {
                this.fileLock.unlock();
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void padToFullSector() throws IOException {
        int currentSize = (int) this.fileChannel.size();
        int expectedSize = RegionFile.sizeToSectors(currentSize) * SECTOR_BYTES;
        if (currentSize != expectedSize) {
            ByteBuffer bytebuffer = RegionFile.PADDING_BUFFER.duplicate();
            ((Buffer) bytebuffer).position(0);
            this.fileChannel.write(bytebuffer, expectedSize - 1);
        }
    }

    private static int getChunkIndex(int x, int z) {
        return (x & 31) + (z & 31) * 32;
    }

    private Path getExternalChunkPath(ChunkPos chunkPos) {
        String s = EXTERNAL_FILE_PREFIX + chunkPos.x + "." + chunkPos.z + EXTERNAL_FILE_SUFFIX;
        return this.directory.resolve(s);
    }

    private class ChunkBuffer extends ByteArrayOutputStream {

        private final ChunkPos pos;

        public ChunkBuffer(ChunkPos pos) {
            super(8096);
            // chunk size 4 bytes
            super.write(0);
            super.write(0);
            super.write(0);
            super.write(0);
            // compression method
            super.write(encodeFlag((byte) RegionFile.this.compression.getId(), FORMAT_VERSION, false));
            this.pos = pos;
        }

        @Override
        public void write(final int b) {
            if (this.count > MAX_CHUNK_SIZE) {
                throw new RegionFileSizeException("Region file too large: " + this.count);
            }
            super.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) {
            if (this.count + len > MAX_CHUNK_SIZE) {
                throw new RegionFileSizeException("Region file too large: " + (this.count + len));
            }
            super.write(b, off, len);
        }

        public void close() throws IOException {
            ByteBuffer bytebuffer = ByteBuffer.wrap(this.buf, 0, this.count);
            bytebuffer.putInt(0, this.count - CHUNK_HEADER_SIZE + 1);
            RegionFile.this.write(this.pos, bytebuffer);
        }
    }

    private interface CommitOp {

        void run() throws IOException;
    }

    public static final class RegionFileSizeException extends RuntimeException {

        public RegionFileSizeException(String message) {
            super(message);
        }
    }
}
