package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {
    private static final Random RANDOM = new Random();
    private static final byte[] BUFFER = new byte[1024 * 8];
    private static final String FILE_NAME = "C/E/".repeat(16383) + "C";
    private static final byte[] FILE_NAME_BYTES = ("C/E/".repeat(16383) + "C/E").getBytes();

    public static void zipDirectory(Path folderPath, Path zipFilePath,
                                    int protectZipLevel, int obfuscateLevel) throws IOException {
        if (protectZipLevel == 0) {
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
                try (Stream<Path> paths = Files.walk(folderPath)) {
                    for (Path path : (Iterable<Path>) paths::iterator) {
                        if (Files.isDirectory(path)) {
                            continue;
                        }
                        String zipEntryName = folderPath.relativize(path).toString().replace("\\", "/");
                        ZipEntry zipEntry = new ZipEntry(zipEntryName);
                        try (InputStream is = Files.newInputStream(path)) {
                            addToZip(zipEntry, is, zos);
                        }
                    }
                }
            }
        } else {
            try (OutputStream os = Files.newOutputStream(zipFilePath);
                 CountingOutputStream cos = new CountingOutputStream(os)) {
                List<CentralDirectoryEntry> centralDirEntries = new ArrayList<>();
                Path rootPath = folderPath.toAbsolutePath();
                createZipHeader(cos, protectZipLevel);

                Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                    @Override
                    public @NotNull FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                        processFile(file, cos, centralDirEntries, rootPath, obfuscateLevel);
                        return FileVisitResult.CONTINUE;
                    }
                });

                if (protectZipLevel == 3) {
                    for (int i = 0; i < 10; i++) {
                        processFakeFile(i, "/" + FILE_NAME, cos, centralDirEntries);
                    }
                }

                long centralDirStartOffset = cos.getCount();

                for (CentralDirectoryEntry entry : centralDirEntries) {
                    writeCentralDirectoryEntry(cos, entry);
                }

                writeEndOfCentralDirectoryRecord(
                        cos,
                        centralDirEntries.size(),
                        centralDirStartOffset,
                        cos.getCount() - centralDirStartOffset
                );
            }
        }
    }

    private static void addToZip(ZipEntry zipEntry, InputStream is, ZipOutputStream zos) throws IOException {
        zos.putNextEntry(zipEntry);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            zos.write(buffer, 0, bytesRead);
        }
        zos.closeEntry();
    }

    private static void processFakeFile(int index, String fileName, CountingOutputStream cos,
                                        List<CentralDirectoryEntry> entries) throws IOException {
        long headerOffset = cos.getCount();

        writeLocalFileHeader(cos);
        cos.write(new byte[] {});

        CentralDirectoryEntry entry = new CentralDirectoryEntry();
        entry.localHeaderOffset = headerOffset;
        entry.compressedSize = 0xFFFFFFFFL;
        entry.uncompressedSize = 0xFFFFFFFFL;
        entry.fileName = (index + fileName).replace(File.separatorChar, '/');
        entry.compressionMethod = 0;
        entries.add(entry);
    }

    private static void processFile(Path file, CountingOutputStream cos, List<CentralDirectoryEntry> entries,
                                    Path rootPath, int obfuscateLevel) throws IOException {
        String relativePath = rootPath.relativize(file).toString().replace(File.separatorChar, '/');
        byte[] originalData = Files.readAllBytes(file);

        byte[] compressedData = compressData(originalData);
        boolean useCompression = compressedData.length < originalData.length;

        long headerOffset = cos.getCount();

        int compressionMethod = useCompression ? 8 : 0;
        long compressedSize = useCompression ? compressedData.length : originalData.length;
        long uncompressedSize = originalData.length;

        writeLocalFileHeader(cos);
        cos.write(useCompression ? compressedData : originalData);

        CentralDirectoryEntry entry = new CentralDirectoryEntry();
        entry.localHeaderOffset = headerOffset;
        entry.compressedSize = compressedSize;
        entry.uncompressedSize = uncompressedSize;
        entry.fileName = relativePath;
        entry.compressionMethod = compressionMethod;
        entries.add(entry);
    }

    private static byte[] compressData(byte[] input) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
        deflater.setInput(input);
        deflater.finish();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = BUFFER;
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            baos.write(buffer, 0, count);
        }
        deflater.end();
        return baos.toByteArray();
    }

    private static void createZipHeader(OutputStream out, int protectZipLevel) throws IOException {
        writeInt(out, 0x06054B50);
        if (protectZipLevel == 2 || protectZipLevel == 3) {
            writeInt(out, 0x06054B50);
            writeInt(out, 0x04034B50);
            writeShort(out, 0);
            writeShort(out, 1);
            writeShort(out, 8);
            writeShort(out, 0xFFFF);
            writeShort(out, 0xFFFF);
            writeInt(out, 0xFFFFFFFFL);
            writeInt(out, 0xFFFFFFFFL);
            writeInt(out, 0xFFFFFFFFL);
            writeShort(out, FILE_NAME_BYTES.length);
            writeShort(out, 0);
            out.write(FILE_NAME_BYTES);
        }
    }

    private static void writeLocalFileHeader(OutputStream out) throws IOException {
        writeInt(out, 0x04034B50);
        writeShort(out, 0);
        writeShort(out, 1);
        writeShort(out, 0);
        writeShort(out, 0);
        writeShort(out, 0);
        writeInt(out, 0);
        writeInt(out, 0);
        writeInt(out, 0);
        writeShort(out, 0);
        writeShort(out, 0);
    }

    private static void writeCentralDirectoryEntry(OutputStream out, CentralDirectoryEntry entry) throws IOException {
        writeInt(out, 0x02014B50);
        writeShort(out, 0);
        writeShort(out, 0);
        writeShort(out, 0);
        writeShort(out, entry.compressionMethod);
        writeShort(out, 0);
        writeShort(out, 0);
        writeInt(out, 0);
        writeInt(out, entry.compressedSize + 1);
        writeInt(out, 0);
        writeShort(out, entry.fileName.getBytes(StandardCharsets.UTF_8).length);
        writeShort(out, 0);
        writeShort(out, 0);
        writeShort(out, RANDOM.nextInt(65536));
        writeShort(out, 0);
        writeInt(out, 1);
        writeInt(out, (int) entry.localHeaderOffset);
        out.write(entry.fileName.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeEndOfCentralDirectoryRecord(OutputStream out, int numEntries,
                                                         long centralDirOffset, long centralDirSize) throws IOException {
        writeInt(out, 0x06054B50);
        writeShort(out, 0xFF);
        writeShort(out, 0);
        writeShort(out, 0);
        writeShort(out, 0);
        writeInt(out, centralDirSize);
        writeInt(out, (int) centralDirOffset);
        writeShort(out, 0);
    }

    private static void writeShort(OutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    private static void writeInt(OutputStream out, long value) throws IOException {
        out.write((int) (value & 0xFF));
        out.write((int) ((value >> 8) & 0xFF));
        out.write((int) ((value >> 16) & 0xFF));
        out.write((int) ((value >> 24) & 0xFF));
    }

    private static class CentralDirectoryEntry {
        long localHeaderOffset;
        long compressedSize;
        long uncompressedSize;
        String fileName;
        int compressionMethod;
    }

    private static class CountingOutputStream extends OutputStream {
        private final OutputStream out;
        private long count = 0;

        public CountingOutputStream(OutputStream out) {
            this.out = out;
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            count++;
        }

        @Override
        public void write(byte @NotNull [] b) throws IOException {
            out.write(b);
            count += b.length;
        }

        @Override
        public void write(byte @NotNull [] b, int off, int len) throws IOException {
            out.write(b, off, len);
            count += len;
        }

        public long getCount() {
            return count - 4;
        }
    }
}
