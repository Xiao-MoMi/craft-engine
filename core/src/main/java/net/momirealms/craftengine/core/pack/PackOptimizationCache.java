package net.momirealms.craftengine.core.pack;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import java.io.*;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

final class PackOptimizationCache {
    private static final int MAGIC = 0x43455043;
    private static final int VERSION = 2;
    private static final Result UNCHANGED = new Result(null);
    private final Cache<CacheKey, Result> results;
    private volatile long maximumBytes;
    private boolean loaded;
    private volatile boolean dirty;

    PackOptimizationCache(long maximumBytes) {
        this.maximumBytes = maximumBytes;
        this.results = Caffeine.newBuilder()
                .maximumWeight(maximumBytes)
                .weigher((CacheKey key, Result value) -> (int) Math.min(Integer.MAX_VALUE,
                        128L + (value.bytes == null ? 0 : value.bytes.length)))
                .build();
    }

    void setMaximumBytes(long maximumBytes) {
        if (this.maximumBytes == maximumBytes) return;
        this.maximumBytes = maximumBytes;
        this.results.policy().eviction().orElseThrow().setMaximum(maximumBytes);
        this.dirty = true;
        if (maximumBytes == 0) clear();
    }

    byte[] optimize(Type type, int options, byte[] input, Optimizer optimizer) throws IOException {
        if (this.maximumBytes == 0) return optimizer.optimize();
        CacheKey key = new CacheKey(type, options, Hashing.sha256().hashBytes(input));
        try {
            Result result = this.results.get(key, ignored -> {
                try {
                    byte[] output = optimizer.optimize();
                    this.dirty = true;
                    return output.length < input.length ? new Result(output) : UNCHANGED;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            return result.bytes == null ? input : result.bytes;
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    void clear() {
        this.results.invalidateAll();
        this.results.cleanUp();
        this.dirty = true;
    }

    // Called before workers start. Validate the entire snapshot before publishing any entries.
    synchronized void load(Path path) throws IOException {
        if (this.loaded || this.maximumBytes == 0) return;
        this.loaded = true;
        this.dirty = true;
        if (Files.notExists(path)) return;
        long remaining = Files.size(path) - 20; // Header and trailing checksum.
        var entries = new ArrayList<Map.Entry<CacheKey, Result>>();
        long weight = 0;
        boolean trimmed = false;
        CRC32 checksum = new CRC32();
        try (var input = new DataInputStream(new CheckedInputStream(new BufferedInputStream(Files.newInputStream(path)), checksum))) {
            if (input.readInt() != MAGIC || input.readInt() != VERSION) return;
            int count = input.readInt();
            if (count < 0 || count > remaining / 41) throw new IOException("Invalid cache entry count");
            Type[] types = Type.values();
            for (int i = 0; i < count; i++) {
                int type = input.readUnsignedByte();
                int options = input.readInt();
                byte[] hash = new byte[32];
                input.readFully(hash);
                int length = input.readInt();
                remaining -= 41;
                if (type >= types.length || length < -1 || Math.max(0, length) > remaining) {
                    throw new IOException("Invalid cache entry");
                }
                long entryWeight = 128L + Math.max(0, length);
                if (entryWeight <= this.maximumBytes - weight) {
                    byte[] bytes = length == -1 ? null : new byte[length];
                    if (bytes != null) input.readFully(bytes);
                    entries.add(Map.entry(new CacheKey(types[type], options, HashCode.fromBytes(hash)),
                            bytes == null ? UNCHANGED : new Result(bytes)));
                    weight += entryWeight;
                } else {
                    input.skipNBytes(Math.max(0, length));
                    trimmed = true;
                }
                remaining -= Math.max(0, length);
            }
            long expectedChecksum = checksum.getValue();
            if (remaining != 0 || input.readLong() != expectedChecksum || input.read() != -1) {
                throw new IOException("Invalid cache checksum or length");
            }
        }
        for (var entry : entries) this.results.put(entry.getKey(), entry.getValue());
        this.results.cleanUp();
        this.dirty = trimmed;
    }

    // Save after workers finish, so unchanged generations do not rewrite the file.
    synchronized void save(Path path) throws IOException {
        if (this.maximumBytes == 0) {
            Files.deleteIfExists(path);
            return;
        }
        if (!this.dirty) return;
        this.dirty = false;
        Path temporary = null;
        try {
            this.results.cleanUp();
            var entries = new ArrayList<Map.Entry<CacheKey, Result>>();
            long weight = 0;
            for (var entry : this.results.asMap().entrySet()) {
                long entryWeight = 128L + (entry.getValue().bytes == null ? 0 : entry.getValue().bytes.length);
                if (entryWeight > this.maximumBytes - weight) continue;
                entries.add(Map.entry(entry.getKey(), entry.getValue()));
                weight += entryWeight;
            }
            Path parent = path.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, "pack-optimization-", ".tmp");
            CRC32 checksum = new CRC32();
            try (var output = new DataOutputStream(new CheckedOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(temporary)), checksum))) {
                output.writeInt(MAGIC);
                output.writeInt(VERSION);
                output.writeInt(entries.size());
                for (var entry : entries) {
                    CacheKey key = entry.getKey();
                    byte[] bytes = entry.getValue().bytes;
                    output.writeByte(key.type.ordinal());
                    output.writeInt(key.options);
                    output.write(key.content.asBytes());
                    output.writeInt(bytes == null ? -1 : bytes.length);
                    if (bytes != null) output.write(bytes);
                }
                output.writeLong(checksum.getValue());
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            this.dirty = true;
            throw e;
        } finally {
            if (temporary != null) Files.deleteIfExists(temporary);
        }
    }

    enum Type {PNG, JSON, MODEL_JSON}

    @FunctionalInterface
    interface Optimizer {
        byte[] optimize() throws IOException;
    }

    private record CacheKey(Type type, int options, HashCode content) {
    }

    private record Result(byte[] bytes) {
    }
}
