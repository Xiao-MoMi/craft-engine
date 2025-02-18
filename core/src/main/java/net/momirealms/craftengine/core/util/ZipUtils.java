package net.momirealms.craftengine.core.util;

// import net.momirealms.craftengine.core.plugin.CraftEngine;
//
// import java.io.FileOutputStream;
// import java.io.IOException;
// import java.io.InputStream;
// import java.io.RandomAccessFile;
// import java.math.BigInteger;
// import java.nio.ByteBuffer;
// import java.nio.ByteOrder;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.attribute.FileTime;
// import java.util.stream.Stream;
// import java.util.zip.ZipEntry;
// import java.util.zip.ZipOutputStream;
//
// public class ZipUtils {
//
//     private ZipUtils() {}
//
//     /*
//      * Local file header
//      * Offset	Bytes	Description
//      * 0	    4	    Local file header signature = 0x04034b50 (PK♥♦ or "PK\3\4")
//      * 4	    2	    Version needed to extract (minimum)
//      * 6	    2	    General purpose bit flag
//      * 8	    2	    Compression method; e.g. none = 0, DEFLATE = 8 (or "\0x08\0x00")
//      * 10	    2	    File last modification time
//      * 12	    2	    File last modification date
//      * 14	    4	    CRC-32 of uncompressed data
//      * 18	    4	    Compressed size (or 0xffffffff for ZIP64)
//      * 22	    4	    Uncompressed size (or 0xffffffff for ZIP64)
//      * 26   	2	    File name length (n)
//      * 28	    2	    Extra field length (m)
//      * 30	    n	    File name
//      * 30+n	    m	    Extra field
//      */
//
//     /*
//      * Central directory file header (CDFH)
//      * Offset	Bytes	Description
//      * 0	    4	    Central directory file header signature = 0x02014b50  50 4b 01 02   50 4b 01 02
//      * 4    	2	    Version made by                                       1e 03         2d 00
//      * 6	    2	    Version needed to extract (minimum)                   14 00         2d 00
//      * 8	    2	    General purpose bit flag                              00 00         08 08
//      * 10   	2	    Compression method                                    08 00         08 00
//      * 12	    2   	File last modification time                           00 00         a8 a8
//      * 14	    2	    File last modification date                           21 00         51 5a
//      * 16	    4	    CRC-32 of uncompressed data                           f9 ad 0c 83   00 10 00 00
//      * 20	    4	    Compressed size (or 0xffffffff for ZIP64)             c2 00 00 00   ba 01 00 00
//      * 24	    4	    Uncompressed size (or 0xffffffff for ZIP64)           c2 10 00 00   ff ff ff ff
//      * 28	    2	    File name length (n)                                  36 00         36 00
//      * 30	    2	    Extra field length (m)                                00 00         0c 00
//      * 32	    2	    File comment length (k)                               00 00         00 00
//      * 34	    2	    Disk number where file starts (or 0xffff for ZIP64)   92 e0         00 00
//      * 36   	2	    Internal file attributes                              00 00         00 00
//      * 38	    4	    External file attributes                              01 00 00 00   00 00 00 00
//      * 42	    4	    Relative offset of local file header (or 0xffffffff   c9 44 00 00   87 72 00 00   for ZIP64). This is the number of bytes between the start of the first disk on which the file occurs, and the start of the local file header. This allows software reading the central directory to locate the position of the file inside the ZIP file.
//      * 46	    n	    File name                                             6173736574732f6d696e6563726166742f74657874757265732f626c6f636b2f637573746f6d2f70616c6d5f6c65617665732e706e67
//      * 46+n 	m	    Extra field                                           01 00 08 00 6c 0f 55 fd 5d 5b 95 13
//      * 46+n+m	k	    File comment
//      */
//
//     /**
//      * End of central directory record (EOCD)
//      * Offset	Bytes	Description[33]
//      * 0	    4	    End of central directory signature = 0x06054b50                                              50 4b 05 06
//      * 4	    2	    Number of this disk (or 0xffff for ZIP64)                                                    ff ff
//      * 6	    2	    Disk where central directory starts (or 0xffff for ZIP64)                                    00 00
//      * 8	    2	    Number of central directory records on this disk (or 0xffff for ZIP64)                       91 00
//      * 10	    2	    Total number of central directory records (or 0xffff for ZIP64)                              91 00
//      * 12	    4	    Size of central directory (bytes) (or 0xffffffff for ZIP64)                                  38 35 00 00
//      * 16	    4	    Offset of start of central directory, relative to start of archive (or 0xffffffff for ZIP64) e3 d3 00 00
//      * 20	    2	    Comment length (n)                                                                           00 00
//      * 22	    n	    Comment
//      */
//     private static final FileTime ZERO = FileTime.fromMillis(0);
//     private static final byte[] FILE_HEADER = { (byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04 };
//     private static final byte[] CDFH = { (byte) 0x50, (byte) 0x4B, (byte) 0x01, (byte) 0x02 };
//     private static final byte[] EOCD = { (byte) 0x50, (byte) 0x4B, (byte) 0x05, (byte) 0x06 };
//     private static final int CDFH_SIGNATURE = 0x504b0102;
//     private static final int EOCD_SIGNATURE = 0x504b0506;
//
//     public static void zipDirectory(Path folderPath, Path zipFilePath, CraftEngine plugin) throws IOException {
//         try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
//             try (Stream<Path> paths = Files.walk(folderPath)) {
//                 for (Path path : (Iterable<Path>) paths::iterator) {
//                     if (Files.isDirectory(path)) {
//                         continue;
//                     }
//                     String zipEntryName = folderPath.relativize(path).toString().replace("\\", "/");
//                     ZipEntry zipEntry = new ZipEntry(zipEntryName);
//                     try (InputStream is = Files.newInputStream(path)) {
//                         addToZip(zipEntry, is, zos, plugin);
//                     }
//                 }
//             }
//         }
//     }
//
//     public static void protect(Path path) {
//         try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw")) {
//             breakHeader(raf);
//         } catch (IOException e) {
//             throw new RuntimeException("Error modify the zip file", e);
//         }
//     }
//
//     public static void breakHeader(RandomAccessFile randomAccessFile) throws IOException {
//         long position = findZipEntryHeader(randomAccessFile);
//         if (position == -1) {
//             throw new RuntimeException("ZIP header signature not found.");
//         }
//         insertData(randomAccessFile, position, EOCD);
//         randomAccessFile.seek(position + 4 + 6);
//         randomAccessFile.write(new byte[] { (byte) 0x01 });
//     }
//
//     public static void modifyEOCD(RandomAccessFile raf) throws IOException {
//         long locationEOCD = findEndOfCentralDirectory(raf);
//         if (locationEOCD == -1) {
//             throw new IllegalArgumentException("Central Directory End Record not found!");
//         }
//         long locationCDFH = findCentralDirectory(raf);
//         if (locationCDFH == -1) {
//             throw new IllegalArgumentException("Central Directory File Header not found!");
//         }
//
//         raf.seek(locationEOCD);
//         raf.write(EOCD);
//         raf.write(new byte[]{(byte) 0xff, (byte) 0xff});
//         // central_Directory_Start_Disk_Number
//         raf.write(new byte[]{(byte) 0x00, (byte) 0x00});
//         // central_Directory_Entry_Count_Current_Disk
//         raf.write(new byte[]{(byte) 0x00, (byte) 0x00});
//         // total_Central_Directory_Entry_Count
//         raf.write(new byte[]{(byte) 0x00, (byte) 0x00});
//         // central_Directory_Size
//         raf.write(longToU32Bytes(locationEOCD - locationCDFH));
//         // central_Directory_Start_Offset
//         raf.write(longToU32Bytes(locationCDFH));
//
//         // byte[] eocdRecord = new byte[]{
//         //         (byte) 0x50, (byte) 0x4B, (byte) 0x05, (byte) 0x06, // EOCD Signature (0x06054b50)
//         //         (byte) 0xff, (byte) 0xff, // Disk number = 0xFFFF
//         //         0x04, 0x00, // Disk with start of central directory
//         //         0x00, 0x00, // Number of entries on this disk
//         //         0x00, 0x00, // Total number of entries in the central directory = 0
//         //         0x0f, 0x00, 0x00, 0x00, // Size of the central directory
//         //         0x04, 0x00, 0x00, 0x00 // Offset to the central directory
//         // };
//         // raf.write(eocdRecord);
//     }
//
//     private static long findZipEntryHeader(RandomAccessFile raf) throws IOException {
//         long fileLength = raf.length();
//         for (long i = 0; i < fileLength - FILE_HEADER.length; i++) {
//             raf.seek(i);
//             byte[] buffer = new byte[FILE_HEADER.length];
//             raf.read(buffer);
//             if (compareArrays(buffer, FILE_HEADER)) {
//                 return i;
//             }
//         }
//         return -1;
//     }
//
//     private static long findEndOfCentralDirectory(RandomAccessFile raf) throws IOException {
//         long fileLength = raf.length();
//         for (long i = fileLength - 22; i >= 0; i--) {
//             raf.seek(i);
//             if (raf.readInt() == EOCD_SIGNATURE) {
//                 return i;
//             }
//         }
//         return -1;
//     }
//
//     private static long findCentralDirectory(RandomAccessFile raf) throws IOException {
//         long fileLength = raf.length();
//         for (long i = fileLength - 22; i >= 0; i--) {
//             raf.seek(i);
//             if (raf.readInt() == CDFH_SIGNATURE) {
//                 return i;
//             }
//         }
//         return -1;
//     }
//
//
//     private static boolean compareArrays(byte[] array1, byte[] array2) {
//         if (array1.length != array2.length) {
//             return false;
//         }
//         for (int i = 0; i < array1.length; i++) {
//             if (array1[i] != array2[i]) {
//                 return false;
//             }
//         }
//         return true;
//     }
//
//     public static void addToZip(ZipEntry zipEntry, InputStream is, ZipOutputStream zos, CraftEngine plugin) throws IOException {
//         zos.putNextEntry(zipEntry);
//         byte[] buffer = new byte[4096];
//         int bytesRead;
//         while ((bytesRead = is.read(buffer)) != -1) {
//             zos.write(buffer, 0, bytesRead);
//         }
//         zos.closeEntry();
//         if (plugin.configManager().settings().getSection("resource-pack").getSection("protection").getBoolean("break-zip-format")) {
//             zipEntry.setCrc(buffer.length);
//             zipEntry.setSize(new BigInteger(buffer).mod(BigInteger.valueOf(Long.MAX_VALUE)).longValue());
//         }
//     }
//
//     public static void insertData(RandomAccessFile randomAccessFile, long pos, byte[] data) throws IOException {
//         long originalLength = randomAccessFile.length();
//         int insertLength = data.length;
//         long newLength = originalLength + insertLength;
//
//         randomAccessFile.setLength(newLength);
//
//         long bytesToMove = originalLength - pos;
//         int bufferSize = 1024;
//         long readEnd = originalLength - 1;
//
//         while (bytesToMove > 0) {
//             long chunkStart = Math.max(pos, readEnd - bufferSize + 1);
//             int chunkSize = (int) (readEnd - chunkStart + 1);
//
//             byte[] buffer = new byte[chunkSize];
//             randomAccessFile.seek(chunkStart);
//             randomAccessFile.readFully(buffer);
//
//             long writePos = chunkStart + insertLength;
//             randomAccessFile.seek(writePos);
//             randomAccessFile.write(buffer);
//
//             bytesToMove -= chunkSize;
//             readEnd = chunkStart - 1;
//         }
//
//         randomAccessFile.seek(pos);
//         randomAccessFile.write(data);
//     }
//
//     private static byte[] longToU32Bytes(long number) {
//         int lower32 = (int) (number & 0xFFFFFFFFL);
//
//         ByteBuffer buffer = ByteBuffer.allocate(4);
//         buffer.order(ByteOrder.LITTLE_ENDIAN);
//         buffer.putInt(lower32);
//
//         return buffer.array();
//     }
// }

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

    private static final int BUFFER_SIZE = 1024 * 8;

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
        entry.externalAttr = 0x20;
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
        entry.externalAttr = 0x10;
        entries.add(entry);
    }

    private static byte[] compressData(byte[] input) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
        deflater.setInput(input);
        deflater.finish();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
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
        writeInt(out, 0x04034B50);  // 文件头签名
        writeShort(out, 0);        // 最低版本
        writeShort(out, 0x0800);    // UTF-8标志
        writeShort(out, compressionMethod); // 压缩方法
        writeShort(out, 0);
        writeShort(out, 0);
        writeInt(out, 0);
        writeInt(out, compressedSize);
        writeInt(out, uncompressedSize);
        writeShort(out, fileNameLength);
        writeShort(out, 0);         // 额外字段长度
    }

    private static void writeCentralDirectoryEntry(OutputStream out, CentralDirectoryEntry entry) throws IOException {
        writeInt(out, 0x02014B50);  // 中央目录头签名
        writeShort(out, 0x14);      // 创建版本（MS-DOS）
        writeShort(out, 0);         // 需要版本
        writeShort(out, 0x0800);     // UTF-8标志
        writeShort(out, entry.compressionMethod); // 压缩方法
        writeShort(out, 0);
        writeShort(out, 0);
        writeInt(out, 0);
        writeInt(out, entry.compressedSize);
        writeInt(out, entry.uncompressedSize);
        writeShort(out, entry.fileName.getBytes(StandardCharsets.UTF_8).length);
        writeShort(out, 0);         // 额外字段长度
        writeShort(out, 0);         // 注释长度
        writeShort(out, 0);         // 磁盘编号
        writeShort(out, 0);         // 内部属性
        writeInt(out, entry.externalAttr); // 外部属性
        writeInt(out, (int) entry.localHeaderOffset);
        out.write(entry.fileName.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeEndOfCentralDirectoryRecord(OutputStream out, int numEntries,
                                                         long centralDirOffset, long centralDirSize) throws IOException {
        writeInt(out, 0x06054B50); // 结束记录签名
        writeShort(out, 0);        // 磁盘编号
        writeShort(out, 0);        // 中央目录起始磁盘
        writeShort(out, numEntries); // 当前磁盘条目数
        writeShort(out, numEntries); // 总条目数
        writeInt(out, centralDirSize); // 中央目录大小
        writeInt(out, (int) centralDirOffset); // 中央目录偏移
        writeShort(out, 0);        // 注释长度
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
        int externalAttr;
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