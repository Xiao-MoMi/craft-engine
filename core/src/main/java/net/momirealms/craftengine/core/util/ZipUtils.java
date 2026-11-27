package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class ZipUtils {
    private static final int ZIP_OUTPUT_BUFFER_SIZE = 64 * 1024;

    private ZipUtils() {}

    public static void compress(Path in, Path out) throws IOException {
        compress(in, out, false);
    }

    public static void compress(Path in, Path out, boolean storePng) throws IOException {
        compress(in, out, storePng, true);
    }

    public static void compress(Path in, Path out, boolean storePng, boolean includeDirectories) throws IOException {
        try (OutputStream output = Files.newOutputStream(out)) {
            compress(in, output, storePng, includeDirectories);
        }
    }

    public static void compress(Path in, OutputStream output, boolean storePng, boolean includeDirectories) throws IOException {
        try (ZipOutputStream archive = new ZipOutputStream(new BufferedOutputStream(output, ZIP_OUTPUT_BUFFER_SIZE))) {
            writeDirectory(in, archive, storePng, includeDirectories);
        }
    }

    public static void compressWithStoredEntry(Path in, Path out, String entryName, InputStream input, long size, long crc) throws IOException {
        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(out), ZIP_OUTPUT_BUFFER_SIZE);
             ZipOutputStream zos = new ZipOutputStream(os)) {
            writeDirectory(in, zos, false, false);
            ZipEntry entry = new ZipEntry(entryName);
            entry.setTime(0L);
            entry.setMethod(ZipEntry.STORED);
            entry.setSize(size);
            entry.setCrc(crc);
            zos.putNextEntry(entry);
            input.transferTo(zos);
            zos.closeEntry();
        }
    }

    private static void writeDirectory(Path in, ZipOutputStream zos, boolean storePng, boolean includeDirectories) throws IOException {
        byte[] buffer = new byte[16 * 1024];
        CRC32 crc = storePng ? new CRC32() : null;
        Files.walkFileTree(in, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
                if (includeDirectories && !dir.equals(in)) {
                    String relativePath = in.relativize(dir).toString().replace("\\", "/") + "/";
                    ZipEntry entry = new ZipEntry(relativePath);
                    entry.setTime(0L);
                    zos.putNextEntry(entry);
                    zos.closeEntry();
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                String relativePath = in.relativize(file).toString().replace("\\", "/");
                ZipEntry entry = new ZipEntry(relativePath);
                entry.setTime(0L);
                if (storePng && FileUtils.isPngFile(file)) {
                    prepareStoredEntry(entry, file, attrs.size(), crc, buffer);
                }
                zos.putNextEntry(entry);
                try (InputStream input = Files.newInputStream(file)) {
                    FileUtils.copy(input, zos, buffer);
                }
                zos.closeEntry();
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void prepareStoredEntry(ZipEntry entry, Path file, long size, CRC32 crc, byte[] buffer) throws IOException {
        crc.reset();
        try (InputStream input = Files.newInputStream(file)) {
            int length;
            while ((length = input.read(buffer)) != -1) {
                crc.update(buffer, 0, length);
            }
        }
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(size);
        entry.setCompressedSize(size);
        entry.setCrc(crc.getValue());
    }

    public static void decompress(Path source, Path target) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(source))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = target.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(target)) {
                    throw new IOException("Bad zip entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Path parent = entryPath.getParent();
                    if (parent != null && Files.notExists(parent)) {
                        Files.createDirectories(parent);
                    }
                    if (Files.notExists(entryPath)) {
                        Files.copy(zis, entryPath);
                    }
                }
                zis.closeEntry();
            }
        }
    }
}
