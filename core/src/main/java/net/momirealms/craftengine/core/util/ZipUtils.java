package net.momirealms.craftengine.core.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private ZipUtils() {}

    /*
     * Local file header
     * Offset	Bytes	Description
     * 0	    4	    Local file header signature = 0x04034b50 (PK♥♦ or "PK\3\4")
     * 4	    2	    Version needed to extract (minimum)
     * 6	    2	    General purpose bit flag
     * 8	    2	    Compression method; e.g. none = 0, DEFLATE = 8 (or "\0x08\0x00")
     * 10	    2	    File last modification time
     * 12	    2	    File last modification date
     * 14	    4	    CRC-32 of uncompressed data
     * 18	    4	    Compressed size (or 0xffffffff for ZIP64)
     * 22	    4	    Uncompressed size (or 0xffffffff for ZIP64)
     * 26   	2	    File name length (n)
     * 28	    2	    Extra field length (m)
     * 30	    n	    File name
     * 30+n	    m	    Extra field
     */

    /*
     * Central directory file header (CDFH)
     * Offset	Bytes	Description
     * 0	    4	    Central directory file header signature = 0x02014b50
     * 4    	2	    Version made by
     * 6	    2	    Version needed to extract (minimum)
     * 8	    2	    General purpose bit flag
     * 10   	2	    Compression method
     * 12	    2   	File last modification time
     * 14	    2	    File last modification date
     * 16	    4	    CRC-32 of uncompressed data
     * 20	    4	    Compressed size (or 0xffffffff for ZIP64)
     * 24	    4	    Uncompressed size (or 0xffffffff for ZIP64)
     * 28	    2	    File name length (n)
     * 30	    2	    Extra field length (m)
     * 32	    2	    File comment length (k)
     * 34	    2	    Disk number where file starts (or 0xffff for ZIP64)
     * 36   	2	    Internal file attributes
     * 38	    4	    External file attributes
     * 42	    4	    Relative offset of local file header (or 0xffffffff for ZIP64). This is the number of bytes between the start of the first disk on which the file occurs, and the start of the local file header. This allows software reading the central directory to locate the position of the file inside the ZIP file.
     * 46	    n	    File name
     * 46+n 	m	    Extra field
     * 46+n+m	k	    File comment
     */

    /**
     * End of central directory record (EOCD)
     * Offset	Bytes	Description[33]
     * 0	    4	    End of central directory signature = 0x06054b50
     * 4	    2	    Number of this disk (or 0xffff for ZIP64)
     * 6	    2	    Disk where central directory starts (or 0xffff for ZIP64)
     * 8	    2	    Number of central directory records on this disk (or 0xffff for ZIP64)
     * 10	    2	    Total number of central directory records (or 0xffff for ZIP64)
     * 12	    4	    Size of central directory (bytes) (or 0xffffffff for ZIP64)
     * 16	    4	    Offset of start of central directory, relative to start of archive (or 0xffffffff for ZIP64)
     * 20	    2	    Comment length (n)
     * 22	    n	    Comment
     */
    private static final FileTime ZERO = FileTime.fromMillis(0);
    private static final byte[] FILE_HEADER = { (byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04 };
    private static final byte[] CDF_HEADER = { (byte) 0x50, (byte) 0x4B, (byte) 0x01, (byte) 0x02 };
    private static final byte[] EOCD = { (byte) 0x50, (byte) 0x4B, (byte) 0x05, (byte) 0x06 };
    private static final int EOCD_SIGNATURE = 0x504b0506;

    public static void zipDirectory(Path folderPath, Path zipFilePath) throws IOException {
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
    }

    public static void protect(Path path) {
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw")) {
            breakHeader(raf);
        } catch (IOException e) {
            throw new RuntimeException("Error modify the zip file", e);
        }
    }

    public static void breakHeader(RandomAccessFile randomAccessFile) throws IOException {
        long position = findZipEntryHeader(randomAccessFile);
        if (position == -1) {
            throw new RuntimeException("ZIP header signature not found.");
        }
        insertData(randomAccessFile, position, EOCD);
        randomAccessFile.seek(position + 4 + 6);
        randomAccessFile.write(new byte[] { (byte) 0x01 });
    }

    public static void modifyEOCD(RandomAccessFile raf) throws IOException {
        long location = findEndOfCentralDirectory(raf);
        if (location == -1) {
            throw new IllegalArgumentException("Central Directory End Record not found!");
        }
        raf.seek(location);
        byte[] eocdRecord = new byte[]{
                (byte) 0x50, (byte) 0x4B, (byte) 0x05, (byte) 0x06, // EOCD Signature (0x06054b50)
                (byte) 0xff, (byte) 0xff, // Disk number = 0xFFFF
                0x04, 0x00, // Disk with start of central directory
                0x00, 0x00, // Number of entries on this disk
                0x00, 0x00, // Total number of entries in the central directory = 0
                0x0f, 0x00, 0x00, 0x00, // Size of the central directory
                0x04, 0x00, 0x00, 0x00 // Offset to the central directory
        };
        raf.write(eocdRecord);
    }

    private static long findZipEntryHeader(RandomAccessFile randomAccessFile) throws IOException {
        long fileLength = randomAccessFile.length();
        for (long i = 0; i < fileLength - FILE_HEADER.length; i++) {
            randomAccessFile.seek(i);
            byte[] buffer = new byte[FILE_HEADER.length];
            randomAccessFile.read(buffer);
            if (compareArrays(buffer, FILE_HEADER)) {
                return i;
            }
        }
        return -1;
    }

    private static long findEndOfCentralDirectory(RandomAccessFile raf) throws IOException {
        long fileLength = raf.length();
        for (long i = fileLength - 22; i >= 0; i--) {
            raf.seek(i);
            if (raf.readInt() == EOCD_SIGNATURE) {
                return i;
            }
        }
        return -1;
    }


    private static boolean compareArrays(byte[] array1, byte[] array2) {
        if (array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }

    public static void addToZip(ZipEntry zipEntry, InputStream is, ZipOutputStream zos) throws IOException {
        zos.putNextEntry(zipEntry);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            zos.write(buffer, 0, bytesRead);
        }
        zos.closeEntry();
    }

    public static void insertData(RandomAccessFile randomAccessFile, long pos, byte[] data) throws IOException {
        long originalLength = randomAccessFile.length();
        int insertLength = data.length;
        long newLength = originalLength + insertLength;

        randomAccessFile.setLength(newLength);

        long bytesToMove = originalLength - pos;
        int bufferSize = 1024;
        long readEnd = originalLength - 1;

        while (bytesToMove > 0) {
            long chunkStart = Math.max(pos, readEnd - bufferSize + 1);
            int chunkSize = (int) (readEnd - chunkStart + 1);

            byte[] buffer = new byte[chunkSize];
            randomAccessFile.seek(chunkStart);
            randomAccessFile.readFully(buffer);

            long writePos = chunkStart + insertLength;
            randomAccessFile.seek(writePos);
            randomAccessFile.write(buffer);

            bytesToMove -= chunkSize;
            readEnd = chunkStart - 1;
        }

        randomAccessFile.seek(pos);
        randomAccessFile.write(data);
    }
}
