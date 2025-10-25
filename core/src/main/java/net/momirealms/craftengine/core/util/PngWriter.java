/**
 * This file is based on the work from the Apache Commons Imaging project.
 * <p>
 * Original source: https://github.com/apache/commons-imaging/blob/master/src/main/java/org/apache/commons/imaging/formats/png/PngWriter.java
 * <p>
 * Modifications have been made to the original code.
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 *      http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.momirealms.craftengine.core.util;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.apache.commons.imaging.common.Allocator;
import org.apache.commons.imaging.formats.png.ChunkType;
import org.apache.commons.imaging.formats.png.InterlaceMethod;
import org.apache.commons.imaging.formats.png.PngConstants;
import org.apache.commons.imaging.palette.Palette;
import org.apache.commons.imaging.palette.PaletteFactory;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class PngWriter {

    private record ImageHeader(int width, int height, byte bitDepth, PngColorType pngColorType, byte compressionMethod,
                               byte filterMethod, InterlaceMethod interlaceMethod) {
    }

    public void write(BufferedImage src, OutputStream os, PaletteFactory paletteFactory) throws IOException {
        final int width = src.getWidth();
        final int height = src.getHeight();
        final boolean hasAlpha = paletteFactory.hasTransparency(src);
        boolean isGrayscale = paletteFactory.isGrayscale(src);

        Pair<PngColorType, byte[]> bestChoice = findBestCompressMethod(src, paletteFactory, hasAlpha, isGrayscale);

        byte bitDepth = 8;
        {
            PngConstants.PNG_SIGNATURE.writeTo(os);
        }
        {
            final byte compressionMethod = PngConstants.COMPRESSION_TYPE_INFLATE_DEFLATE;
            final byte filterMethod = PngConstants.FILTER_METHOD_ADAPTIVE;
            final InterlaceMethod interlaceMethod = InterlaceMethod.NONE;
            final ImageHeader imageHeader = new ImageHeader(width, height, bitDepth, bestChoice.left(), compressionMethod, filterMethod, interlaceMethod);
            writeChunkIHDR(os, imageHeader);
        }

        os.write(bestChoice.right());
        writeChunkIEND(os);
        os.close();
    }

    private Pair<PngColorType, byte[]> findBestCompressMethod(BufferedImage src, PaletteFactory paletteFactory,  boolean hasAlpha, boolean isGrayscale) throws IOException {
        byte[] paletteData = null;
        if (countUniqueColors(src, 257) <= 256) {
            paletteData = tryPalette(src, paletteFactory, hasAlpha);
        }
        byte[] normalData = tryNormal(src, hasAlpha, isGrayscale);
        if (paletteData != null && paletteData.length < normalData.length) {
            return Pair.of(PngColorType.INDEXED_COLOR, paletteData);
        } else {
            if (isGrayscale) {
                return Pair.of(hasAlpha ? PngColorType.GREYSCALE_WITH_ALPHA : PngColorType.GREYSCALE, normalData);
            } else {
                return Pair.of(hasAlpha ? PngColorType.TRUE_COLOR_WITH_ALPHA : PngColorType.TRUE_COLOR, normalData);
            }
        }
    }

    private int countUniqueColors(BufferedImage src, int maxCount) {
        int width = src.getWidth();
        int height = src.getHeight();
        Set<Integer> colors = new IntOpenHashSet();
        int[] row = Allocator.intArray(width);
        for (int y = 0; y < height; y++) {
            src.getRGB(0, y, width, 1, row, 0, width);
            for (int x = 0; x < width; x++) {
                colors.add(row[x]);
                if (colors.size() >= maxCount) {
                    return colors.size();
                }
            }
        }
        return colors.size();
    }

    private byte[] tryNormal(BufferedImage src, boolean hasAlpha, boolean isGrayscale) throws IOException {
        byte[] bytes = generatePngData(src, hasAlpha, isGrayscale);
        return compressImage(bytes, false, Deflater.BEST_COMPRESSION);
    }

    private byte[] generatePngData(BufferedImage src, boolean hasAlpha, boolean isGrayscale) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int width = src.getWidth();
        int height = src.getHeight();
        int[] row = Allocator.intArray(width);
        for (int y = 0; y < height; y++) {
            src.getRGB(0, y, width, 1, row, 0, width);
            baos.write(FilterType.NONE.ordinal());
            for (int x = 0; x < width; x++) {
                final int argb = row[x];
                final int alpha = 0xff & argb >> 24;
                final int red = 0xff & argb >> 16;
                final int green = 0xff & argb >> 8;
                final int blue = 0xff & argb >> 0;
                if (isGrayscale) {
                    final int gray = (red + green + blue) / 3;
                    baos.write(gray);
                } else {
                    baos.write(red);
                    baos.write(green);
                    baos.write(blue);
                }
                if (hasAlpha) {
                    baos.write(alpha);
                }
            }
        }
        return baos.toByteArray();
    }

    private byte[] tryPalette(BufferedImage src, PaletteFactory paletteFactory, boolean hasAlpha) throws IOException {
        ByteArrayOutputStream paletteOs = new ByteArrayOutputStream();
        Palette palette;
        final int maxColors = 256;
        if (hasAlpha) {
            palette = paletteFactory.makeQuantizedRgbaPalette(src, true, maxColors);
            writeChunkPLTE(paletteOs, palette);
            writeChunkTRNS(paletteOs, palette);
        } else {
            palette = paletteFactory.makeQuantizedRgbPalette(src, maxColors);
            writeChunkPLTE(paletteOs, palette);
        }
        byte[] bytes = generatePaletteData(src, palette);
        paletteOs.write(compressImage(bytes, false, Deflater.BEST_COMPRESSION));
        return paletteOs.toByteArray();
    }

    private byte[] generatePaletteData(BufferedImage src, Palette palette) throws IOException {
        int width = src.getWidth();
        int height = src.getHeight();
        final ByteArrayOutputStream dataOs = new ByteArrayOutputStream();
        final int[] row = Allocator.intArray(width);
        for (int y = 0; y < height; y++) {
            src.getRGB(0, y, width, 1, row, 0, width);
            dataOs.write(FilterType.NONE.ordinal());
            for (int x = 0; x < width; x++) {
                final int argb = row[x];
                final int index = palette.getPaletteIndex(argb);
                dataOs.write(0xff & index);
            }
        }
        return dataOs.toByteArray();
    }

    private byte[] compressImage(byte[] uncompressed, boolean useFiltered, int level) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final int chunkSize = 32 * 1024;
        final Deflater deflater = new Deflater(level);
        if (useFiltered) {
            deflater.setStrategy(Deflater.FILTERED);
        }

        final DeflaterOutputStream dos = new DeflaterOutputStream(baos, deflater, chunkSize);

        for (int index = 0; index < uncompressed.length; index += chunkSize) {
            final int end = Math.min(uncompressed.length, index + chunkSize);
            final int length = end - index;

            dos.write(uncompressed, index, length);
            dos.flush();
            baos.flush();

            final byte[] compressed = baos.toByteArray();
            baos.reset();
            if (compressed.length > 0) {
                writeChunkIDAT(output, compressed);
            }
        }

        {
            dos.finish();
            final byte[] compressed = baos.toByteArray();
            if (compressed.length > 0) {
                writeChunkIDAT(output, compressed);
            }
        }

        return output.toByteArray();
    }

    private void writeChunkIDAT(final OutputStream os, final byte[] bytes) throws IOException {
        writeChunk(os, ChunkType.IDAT, bytes);
    }

    private void writeChunkIEND(final OutputStream os) throws IOException {
        writeChunk(os, ChunkType.IEND, null);
    }

    private void writeChunkTRNS(final OutputStream os, final Palette palette) throws IOException {
        final byte[] bytes = Allocator.byteArray(palette.length());
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (0xff & palette.getEntry(i) >> 24);
        }
        writeChunk(os, ChunkType.tRNS, bytes);
    }

    private void writeChunkPLTE(final OutputStream os, final Palette palette) throws IOException {
        final int length = palette.length();
        final byte[] bytes = Allocator.byteArray(length * 3);
        for (int i = 0; i < length; i++) {
            final int rgb = palette.getEntry(i);
            final int index = i * 3;
            bytes[index + 0] = (byte) (0xff & rgb >> 16);
            bytes[index + 1] = (byte) (0xff & rgb >> 8);
            bytes[index + 2] = (byte) (0xff & rgb >> 0);
        }
        writeChunk(os, ChunkType.PLTE, bytes);
    }

    private void writeChunkIHDR(final OutputStream os, final ImageHeader value) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeInt(baos, value.width);
        writeInt(baos, value.height);
        baos.write(0xff & value.bitDepth);
        baos.write(0xff & value.pngColorType.getValue());
        baos.write(0xff & value.compressionMethod);
        baos.write(0xff & value.filterMethod);
        baos.write(0xff & value.interlaceMethod.ordinal());
        writeChunk(os, ChunkType.IHDR, baos.toByteArray());
    }

    private void writeInt(final OutputStream os, final int value) throws IOException {
        os.write(0xff & value >> 24);
        os.write(0xff & value >> 16);
        os.write(0xff & value >> 8);
        os.write(0xff & value >> 0);
    }

    private void writeChunk(final OutputStream os, final ChunkType chunkType, final byte[] data) throws IOException {
        final int dataLength = data == null ? 0 : data.length;
        byte[] chunkTypeBytes = chunkType.name().getBytes(StandardCharsets.UTF_8);
        writeInt(os, dataLength);
        os.write(chunkTypeBytes);
        if (data != null) {
            os.write(data);
        }
        writeInt(os, 0); // crc
    }

    public enum PngColorType {

        GREYSCALE(0, true, false, 1, new int[] { 1, 2, 4, 8, 16 }), TRUE_COLOR(2, false, false, 3, new int[] { 8, 16 }),
        INDEXED_COLOR(3, false, false, 1, new int[] { 1, 2, 4, 8 }), GREYSCALE_WITH_ALPHA(4, true, true, 2, new int[] { 8, 16 }),
        TRUE_COLOR_WITH_ALPHA(6, false, true, 4, new int[] { 8, 16 });

        private final int value;
        private final boolean greyscale;
        private final boolean alpha;
        private final int samplesPerPixel;
        private final int[] allowedBitDepths;

        PngColorType(final int value, final boolean greyscale, final boolean alpha, final int samplesPerPixel, final int[] allowedBitDepths) {
            this.value = value;
            this.greyscale = greyscale;
            this.alpha = alpha;
            this.samplesPerPixel = samplesPerPixel;
            this.allowedBitDepths = allowedBitDepths;
        }

        int getSamplesPerPixel() {
            return samplesPerPixel;
        }

        int getValue() {
            return value;
        }

        boolean hasAlpha() {
            return alpha;
        }

        boolean isBitDepthAllowed(final int bitDepth) {
            return Arrays.binarySearch(allowedBitDepths, bitDepth) >= 0;
        }

        boolean isGreyscale() {
            return greyscale;
        }
    }

    enum FilterType {
        NONE, SUB, UP, AVERAGE, PAETH
    }
}
