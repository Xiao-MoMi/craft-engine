package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;

public class ZipUtils {

    private static final byte[] BUFFER = new byte[1024 * 8];

    public static void zipDirectory(Path folderPath, Path zipFilePath) throws IOException {
        try (OutputStream os = Files.newOutputStream(zipFilePath);
             CountingOutputStream cos = new CountingOutputStream(os)) {

            List<CentralDirectoryEntry> centralDirEntries = new ArrayList<>();
            Path rootPath = folderPath.toAbsolutePath();

            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                    processFile(file, cos, centralDirEntries, rootPath);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult preVisitDirectory(Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
                    if (!dir.equals(rootPath)) {
                        processDirectory(dir, cos, centralDirEntries, rootPath);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

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

    private static void processFile(Path file, CountingOutputStream cos,
                                    List<CentralDirectoryEntry> entries, Path rootPath) throws IOException {
        String relativePath = rootPath.relativize(file).toString().replace(File.separatorChar, '/');
        byte[] originalData = Files.readAllBytes(file);

        // 压缩数据
        byte[] compressedData = compressData(originalData);
        boolean useCompression = compressedData.length < originalData.length;

        byte[] fileNameBytes = relativePath.getBytes(StandardCharsets.UTF_8);
        long headerOffset = cos.getCount();

        // 根据是否使用压缩设置方法标记
        int compressionMethod = useCompression ? 8 : 0;
        long compressedSize = useCompression ? compressedData.length : originalData.length;
        long uncompressedSize = originalData.length;

        writeLocalFileHeader(cos, compressedSize, uncompressedSize,
                fileNameBytes.length, compressionMethod);
        cos.write(fileNameBytes);
        cos.write(useCompression ? compressedData : originalData);

        CentralDirectoryEntry entry = new CentralDirectoryEntry();
        entry.localHeaderOffset = headerOffset;
        entry.compressedSize = compressedSize;
        entry.uncompressedSize = uncompressedSize;
        entry.fileName = relativePath;
        entry.compressionMethod = compressionMethod;
        entries.add(entry);
    }

    private static void processDirectory(Path dir, CountingOutputStream cos,
                                         List<CentralDirectoryEntry> entries, Path rootPath) throws IOException {
        String relativePath = rootPath.relativize(dir).toString().replace(File.separatorChar, '/') + "/";

        byte[] fileNameBytes = relativePath.getBytes(StandardCharsets.UTF_8);
        long headerOffset = cos.getCount();

        writeLocalFileHeader(cos, 0, 0, fileNameBytes.length, 0);
        cos.write(fileNameBytes);

        CentralDirectoryEntry entry = new CentralDirectoryEntry();
        entry.localHeaderOffset = headerOffset;
        entry.compressedSize = 0;
        entry.uncompressedSize = 0;
        entry.fileName = relativePath;
        entry.compressionMethod = 0;
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

    private static void writeLocalFileHeader(OutputStream out,
                                             long compressedSize, long uncompressedSize,
                                             int fileNameLength, int compressionMethod) throws IOException {
        writeInt(out, 0x04034B50);    // 文件头签名
        writeShort(out, 0);           // 最低版本
        writeShort(out, 0x0800);      // UTF-8标志
        writeShort(out, compressionMethod); // 压缩方法
        writeShort(out, 0);           // 文件最后修改时间
        writeShort(out, 0);           // 文件最后修改日期
        writeInt(out, 0);             // 未压缩数据的 CRC-32
        writeInt(out, compressedSize);      // 压缩数据的大小
        writeInt(out, uncompressedSize);    // 未压缩数据的大小
        writeShort(out, fileNameLength);    // 文件名长度
        writeShort(out, 0);           // 额外字段长度
    }

    private static void writeCentralDirectoryEntry(OutputStream out, CentralDirectoryEntry entry) throws IOException {
        writeInt(out, 0x02014B50);                                             // 中央目录头签名
        writeShort(out, 0x14);                                                 // 创建版本（MS-DOS）
        writeShort(out, 0);                                                    // 需要版本
        writeShort(out, 0x0800);                                               // UTF-8标志
        writeShort(out, entry.compressionMethod);                                    // 压缩方法
        writeShort(out, 0);                                                    // 文件最后修改时间
        writeShort(out, 0);                                                    // 文件最后修改日期
        writeInt(out, 0);                                                      // 未压缩数据的 CRC-32
        writeInt(out, entry.compressedSize);                                         // 压缩数据的大小
        writeInt(out, entry.uncompressedSize);                                       // 未压缩数据的大小
        writeShort(out, entry.fileName.getBytes(StandardCharsets.UTF_8).length);     // 文件名长度
        writeShort(out, 0);                                                    // 额外字段长度
        writeShort(out, 0);                                                    // 注释长度
        writeShort(out, 0);                                                    // 磁盘编号
        writeShort(out, 0);                                                    // 内部属性
        writeInt(out, 0);                                                      // 外部属性
        writeInt(out, (int) entry.localHeaderOffset);                                // 本地文件头的相对偏移量
        out.write(entry.fileName.getBytes(StandardCharsets.UTF_8));                  // 文件名
    }

    private static void writeEndOfCentralDirectoryRecord(OutputStream out, int numEntries,
                                                         long centralDirOffset, long centralDirSize) throws IOException {
        writeInt(out, 0x06054B50);       // 结束记录签名
        writeShort(out, 0);              // 磁盘编号
        writeShort(out, 0);              // 中央目录起始磁盘
        writeShort(out, numEntries);           // 当前磁盘条目数
        writeShort(out, numEntries);           // 总条目数
        writeInt(out, centralDirSize);         // 中央目录大小
        writeInt(out, (int) centralDirOffset); // 中央目录偏移
        writeShort(out, 0);              // 注释长度
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
            return count;
        }
    }
}