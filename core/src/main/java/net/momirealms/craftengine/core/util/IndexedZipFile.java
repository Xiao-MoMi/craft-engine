package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class IndexedZipFile implements AutoCloseable {
    private final ZipFile archive;
    private final Map<String, ZipEntry> entries;

    public IndexedZipFile(Path path) throws IOException {
        this.archive = new ZipFile(path.toFile());
        try {
            Map<String, ZipEntry> entries = HashMap.newHashMap(this.archive.size());
            Enumeration<? extends ZipEntry> iterator = this.archive.entries();
            while (iterator.hasMoreElements()) {
                ZipEntry entry = iterator.nextElement();
                entries.put(entry.getName(), entry);
            }
            this.entries = Collections.unmodifiableMap(entries);
        } catch (RuntimeException | Error exception) {
            this.archive.close();
            throw exception;
        }
    }

    @Nullable
    public ZipEntry getEntry(String path) {
        ZipEntry entry = this.entries.get(path);
        if (entry == null && !path.endsWith("/")) {
            entry = this.entries.get(path + '/');
        }
        return entry;
    }

    public Collection<ZipEntry> entries() {
        return this.entries.values();
    }

    public InputStream getInputStream(ZipEntry entry) throws IOException {
        return this.archive.getInputStream(entry);
    }

    @Override
    public void close() throws IOException {
        this.archive.close();
    }
}
