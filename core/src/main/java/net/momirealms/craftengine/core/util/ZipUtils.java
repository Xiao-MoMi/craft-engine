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
import java.util.Random;

public class ZipUtils {
    private static final Random RANDOM = new Random();
    private static final byte[] BUFFER = new byte[1024 * 8];

    public static void zipDirectory(Path folderPath, Path zipFilePath) throws IOException {
        try (OutputStream os = Files.newOutputStream(zipFilePath);
             CountingOutputStream cos = new CountingOutputStream(os)) {
            writeInt(cos, 0x06054B50);

            List<CentralDirectoryEntry> centralDirEntries = new ArrayList<>();
            Path rootPath = folderPath.toAbsolutePath();

            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                    processFile(file, cos, centralDirEntries, rootPath);
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

        byte[] compressedData = compressData(originalData);
        boolean useCompression = compressedData.length < originalData.length;

        byte[] fileNameBytes = relativePath.toUpperCase().getBytes(StandardCharsets.UTF_8);
        long headerOffset = cos.getCount();

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
        writeInt(out, 0x04034B50);             // 文件头签名          50 4b 03 04     50 4b 03 04
        writeShort(out, 0x0014);               // 最低版本            14 00           14 00
        writeShort(out, 0x0001);               // UTF-8 标志和伪加密   00 00           01 00
        writeShort(out, compressionMethod);          // 压缩方法            08 00           08 00
        writeShort(out, 0);                    // 文件最后修改时间      00 00           00 00
        writeShort(out, 0);                    // 文件最后修改日期      21 00           21 00
        writeInt(out, 0);                      // 未压缩数据的 CRC-32  89 0c f1 6a     00 00 00 00
        writeInt(out, compressedSize + 7);     // 压缩数据的大小        6c 00 00 00     6d 00 00 00
        writeInt(out, uncompressedSize + 7);   // 未压缩数据的大小      9b 00 00 00     9a 00 00 00
        writeShort(out, fileNameLength);             // 文件名长度           24 00           24 00
        writeShort(out, 0);                    // 额外字段长度         00 00            00 00
    }

    private static void writeCentralDirectoryEntry(OutputStream out, CentralDirectoryEntry entry) throws IOException {
        writeInt(out, 0x02014B50);                                             // 中央目录头签名      50 4b 01 02    50 4b 01 02    50 4b 01 02
        writeShort(out, 0x031E);                                               // 创建版本(MS-DOS)   1e 03          1e 03          1e 03
        writeShort(out, 0x0014);                                               // 最低版本           14 00          14 00          14 00
        writeShort(out, 0);                                                    // UTF-8 标志         00 00         00 00          00 00
        writeShort(out, entry.compressionMethod);                                    // 压缩方法           08 00          08 00          08 00
        writeShort(out, 0);                                                    // 文件最后修改时间     00 00         00 00           00 00
        writeShort(out, 0);                                                    // 文件最后修改日期     21 00         00 00           21 00
        writeInt(out, 0);                                                      // 未压缩数据的 CRC-32  89 0c f1 6a   00 00 00 00    f9 fe 34 f4
        writeInt(out, entry.compressedSize + 7);                               // 压缩数据的大小       6a 00 00 00   6d 00 00 00     75 01 00 00
        writeInt(out, entry.uncompressedSize + 7);                             // 未压缩数据的大小     6a 10 00 00   9a 00 00 00     75 11 00 00
        writeShort(out, entry.fileName.getBytes(StandardCharsets.UTF_8).length);     // 文件名长度          24 00         24 00           2d 00
        writeShort(out, 0);                                                    // 额外字段长度         00 00         00 00          00 00
        writeShort(out, 0);                                                    // 注释长度            00 00         00 00           00 00
        writeShort(out, RANDOM.nextInt(65536));                               // 磁盘编号            44 bc         c2 45           9e d6
        writeShort(out, 0);                                                    // 内部属性            00 00         00 00           00 00
        writeInt(out, 1);                                                      // 外部属性            01 00 00 00   01 00 00 00     01 00 00 00
        writeInt(out, (int) entry.localHeaderOffset);                                // 本地文件头的相对偏移量 00 00 00 00   00 00 00 00     8d 40 01 00
        out.write(entry.fileName.getBytes(StandardCharsets.UTF_8));                  // 文件名
    }

    private static void writeEndOfCentralDirectoryRecord(OutputStream out, int numEntries,
                                                         long centralDirOffset, long centralDirSize) throws IOException {
        writeInt(out, 0x06054B50);       // 结束记录签名     50 4b 05 06    50 4b 05 06
        writeShort(out, 0xFFFF);         // 磁盘编号        ff ff          ff ff
        writeShort(out, 0);              // 中央目录起始磁盘  00 00          00 00
        writeShort(out, numEntries);           // 当前磁盘条目数    6f 01         68 01
        writeShort(out, numEntries);           // 总条目数         6f 01         68 01
        writeInt(out, centralDirSize);         // 中央目录大小     ad 84 00 00    ad 84 00 00
        writeInt(out, (int) centralDirOffset); // 中央目录偏移     95 bb 02 00    1a 21 04 00
        writeShort(out, 0);              // 注释长度         00 00         00 00
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