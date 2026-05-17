package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class ZipUtils {
    private ZipUtils() {}

    public static void compress(Path in, Path out) throws IOException {
        try (OutputStream os = Files.newOutputStream(out);
             ZipOutputStream zos = new ZipOutputStream(os)) {

            Files.walkFileTree(in, new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
                    if (!dir.equals(in)) {
                        String relativePath = in.relativize(dir).toString().replace("\\", "/") + "/";
                        ZipEntry entry = new ZipEntry(relativePath);
                        zos.putNextEntry(entry);
                        zos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                    String relativePath = in.relativize(file).toString().replace("\\", "/");
                    ZipEntry entry = new ZipEntry(relativePath);
                    zos.putNextEntry(entry);
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
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
