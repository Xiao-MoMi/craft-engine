package net.momirealms.craftengine.core.util;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public final class ClientPngReader {
    private static final long SIGNATURE = 0x89504e470d0a1a0aL;
    private static final int IDAT = 0x49444154;

    private ClientPngReader() {
    }

    public static BufferedImage read(InputStream input) throws IOException {
        byte[] png = input.readAllBytes();
        BufferedImage image;
        try {
            image = PngOptimizer.readPng(new ByteArrayInputStream(png));
        } catch (IOException exception) {
            // The client's PNG decoder tolerates the CRC/Adler corruption used by pack protection.
            // Restore only checksums in our private copy, leaving pixel data and the source ZIP untouched.
            restoreChecksums(png);
            image = PngOptimizer.readPng(new ByteArrayInputStream(png));
        }
        if (image != null && PngOptimizer.isRawGrayscale(image)) {
            // Preserve PNG gray samples before getRGB()/Graphics2D applies linear-gray conversion.
            BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            int[] pixels = ((DataBufferInt) result.getRaster().getDataBuffer()).getData();
            PngOptimizer.readPixels(image, pixels, 0, image.getWidth());
            return result;
        }
        return image;
    }

    private static void restoreChecksums(byte[] png) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(png);
        if (png.length < 8 || buffer.getLong(0) != SIGNATURE) {
            throw new IOException("Invalid PNG signature");
        }
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        for (int offset = 8; offset < png.length;) {
            int length = chunkLength(buffer, offset);
            if (buffer.getInt(offset + 4) == IDAT) {
                compressed.write(png, offset + 8, length);
            }
            offset += length + 12;
        }
        byte[] data = compressed.toByteArray();
        if (data.length < 6) {
            throw new IOException("Missing PNG zlib stream");
        }
        Adler32 adler = new Adler32();
        Inflater inflater = new Inflater(true);
        try {
            inflater.setInput(data, 2, data.length - 6);
            byte[] scanlines = new byte[8192];
            while (!inflater.finished()) {
                int count = inflater.inflate(scanlines);
                if (count == 0 && !inflater.finished()) {
                    throw new IOException("Incomplete PNG deflate stream");
                }
                adler.update(scanlines, 0, count);
            }
        } catch (DataFormatException exception) {
            throw new IOException("Invalid PNG deflate stream", exception);
        } finally {
            inflater.end();
        }
        int checksum = (int) adler.getValue();
        int compressedOffset = 0;
        CRC32 crc = new CRC32();
        for (int offset = 8; offset < png.length;) {
            int length = chunkLength(buffer, offset);
            if (buffer.getInt(offset + 4) == IDAT) {
                for (int index = Math.max(0, data.length - 4 - compressedOffset); index < length; index++) {
                    int trailerIndex = compressedOffset + index - (data.length - 4);
                    png[offset + 8 + index] = (byte) (checksum >>> ((3 - trailerIndex) * 8));
                }
                compressedOffset += length;
            }
            crc.reset();
            crc.update(png, offset + 4, length + 4);
            buffer.putInt(offset + 8 + length, (int) crc.getValue());
            offset += length + 12;
        }
    }

    private static int chunkLength(ByteBuffer buffer, int offset) throws IOException {
        if (buffer.limit() - offset < 12) {
            throw new IOException("Truncated PNG chunk");
        }
        int length = buffer.getInt(offset);
        if (length < 0 || length > buffer.limit() - offset - 12) {
            throw new IOException("Invalid PNG chunk length");
        }
        return length;
    }
}
