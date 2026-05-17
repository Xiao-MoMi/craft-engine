package net.momirealms.craftengine.core.util;

import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.zopfli.Options;
import net.momirealms.craftengine.core.util.zopfli.ZopfliOutputStream;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public final class PngOptimizer {
    private static final byte[] PNG_SIGNATURE = new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };
    private static final byte[] IDAT = "IDAT".getBytes(StandardCharsets.UTF_8);
    private static final byte[] IEND = "IEND".getBytes(StandardCharsets.UTF_8);
    private static final byte[] tRNS = "tRNS".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PLTE = "PLTE".getBytes(StandardCharsets.UTF_8);
    private static final byte[] IHDR = "IHDR".getBytes(StandardCharsets.UTF_8);

    private final BufferedImage src;

    public PngOptimizer(BufferedImage src) {
        this.src = src;
    }

    private boolean isGrayscale(final BufferedImage src) {
        final int width = src.getWidth();
        final int height = src.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int argb = src.getRGB(x, y);
                final int red = 0xff & argb >> 16;
                final int green = 0xff & argb >> 8;
                final int blue = 0xff & argb >> 0;
                if (red != green || red != blue) {
                    return false;
                }
            }
        }
        return true;
    }

    public void write(OutputStream os) throws IOException {
        BufferedImage src = convertTo8BitRGB(this.src);
        final int width = src.getWidth();
        final int height = src.getHeight();

        ImageColorInfo info = createColorInfo(src);
        ImageData bestChoice = findBestFileStructure(src, info);
        {
            os.write(PNG_SIGNATURE);
        }
        {
            final byte compressionMethod = 0;
            final byte filterMethod = 0;
            final InterlaceMethod interlaceMethod = InterlaceMethod.NONE;
            final ImageHeader imageHeader = new ImageHeader(width, height, bestChoice.bitDepth, bestChoice.colorType, compressionMethod, filterMethod, interlaceMethod);
            writeChunkIHDR(os, imageHeader);
        }

        os.write(bestChoice.data);
        writeChunkIEND(os);
        os.close();
    }

    private ImageColorInfo createColorInfo(final BufferedImage src) {
        final int width = src.getWidth();
        final int height = src.getHeight();
        boolean isGrayscale = isGrayscale(src);

        Map<Integer, Integer> ope = new HashMap<>();
        Map<Integer, Integer> tra = new HashMap<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = src.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha == 255) {
                    ope.put(argb, ope.getOrDefault(argb, 0) + 1);
                } else {
                    tra.put(argb, ope.getOrDefault(argb, 0) + 1);
                }
            }
        }

        return new ImageColorInfo(ope, tra, isGrayscale);
    }

    private BufferedImage convertTo8BitRGB(BufferedImage src) {
        int type = src.getType();
        if (type == BufferedImage.TYPE_BYTE_GRAY || type == BufferedImage.TYPE_USHORT_GRAY) {
            BufferedImage eightBitImage = new BufferedImage(
                    src.getWidth(),
                    src.getHeight(),
                    BufferedImage.TYPE_4BYTE_ABGR
            );
            Graphics2D g2d = eightBitImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
            g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2d.drawImage(src, 0, 0, null);
            g2d.dispose();
            return eightBitImage;
        }
        return src;
    }

    private ImageData findBestFileStructure(BufferedImage src, ImageColorInfo info) throws IOException {
        byte[] normalSize = tryNormal(src, info.hasAlpha(), info.isGrayscale());
        // 可以考虑使用调色盘
        if (info.uniqueColorCount() <= 256) {
            Pair<Palette, byte[]> palettePair = tryPalette(src, info);
            byte[] paletteSize = palettePair.right();
            if (normalSize.length > paletteSize.length) {
                return new ImageData(PngColorType.INDEXED_COLOR, (byte) palettePair.left().calculateBitDepth(), paletteSize);
            }
        }
        if (info.isGrayscale()) {
            return new ImageData(info.hasAlpha() ? PngColorType.GREYSCALE_WITH_ALPHA : PngColorType.GREYSCALE, (byte) 8, normalSize);
        } else {
            return new ImageData(info.hasAlpha() ? PngColorType.TRUE_COLOR_WITH_ALPHA : PngColorType.TRUE_COLOR, (byte) 8, normalSize);
        }
    }

    private byte[] tryNormal(BufferedImage src, boolean hasAlpha, boolean isGrayscale) throws IOException {
        byte[] bytes = generatePngData(src, hasAlpha, isGrayscale);
        int zopfli = Config.optimizeTexture() ? Config.zopfliIterations() : 0;
        return zopfli > 0 ? compressImageZopfli(bytes, zopfli) : compressImageStandard(bytes);
    }

    private byte[] generatePngData(BufferedImage src, boolean hasAlpha, boolean isGrayscale) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int width = src.getWidth();
        int height = src.getHeight();
        int[] row = new int[width];
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

    private Pair<Palette, byte[]> tryPalette(BufferedImage src, ImageColorInfo info) throws IOException {
        ByteArrayOutputStream paletteOs = new ByteArrayOutputStream();
        Palette palette;
        if (info.hasAlpha()) {
            palette = new ExactTransparentPalette(info.opaque, info.transparent);
            writeChunkPLTE(paletteOs, palette);
            writeChunkTRNS(paletteOs, palette);
        } else {
            palette = new ExactOpaquePalette(info.opaque);
            writeChunkPLTE(paletteOs, palette);
        }
        byte[] bytes = generatePaletteData(src, palette);
        int zopfli = Config.optimizeTexture() ? Config.zopfliIterations() : 0;
        paletteOs.write(zopfli > 0 ? compressImageZopfli(bytes, zopfli) : compressImageStandard(bytes));
        return Pair.of(palette, paletteOs.toByteArray());
    }

    private byte[] generatePaletteData(BufferedImage src, Palette palette) {
        int width = src.getWidth();
        int height = src.getHeight();
        int bitsPerIndex = palette.calculateBitDepth();
        final ByteArrayOutputStream dataOs = new ByteArrayOutputStream();
        final int[] row = new int[width];

        for (int y = 0; y < height; y++) {
            src.getRGB(0, y, width, 1, row, 0, width);
            dataOs.write(FilterType.NONE.ordinal());

            // 根据位深度选择相应的处理方法
            switch (bitsPerIndex) {
                case 4 -> process4Bit(row, width, dataOs, palette);
                case 2 -> process2Bit(row, width, dataOs, palette);
                case 1 -> process1Bit(row, width, dataOs, palette);
                default -> process8Bit(row, width, dataOs, palette);
            }
        }
        return dataOs.toByteArray();
    }

    // 处理8位深度：每个索引占1字节
    private void process8Bit(int[] row, int width, ByteArrayOutputStream dataOs, Palette palette) {
        for (int x = 0; x < width; x++) {
            final int argb = row[x];
            final int index = palette.getPaletteIndex(argb);
            dataOs.write(0xff & index);
        }
    }

    // 处理4位深度：每2个索引打包到1字节中
    private void process4Bit(int[] row, int width, ByteArrayOutputStream dataOs, Palette palette) {
        for (int x = 0; x < width; x += 2) {
            final int argb1 = row[x];
            final int index1 = palette.getPaletteIndex(argb1);

            if (x + 1 < width) {
                final int argb2 = row[x + 1];
                final int index2 = palette.getPaletteIndex(argb2);
                // 将两个4位索引打包到一个字节中
                byte packed = (byte) ((index1 << 4) | index2);
                dataOs.write(packed);
            } else {
                // 如果是奇数宽度，最后一个像素单独处理
                byte packed = (byte) (index1 << 4);
                dataOs.write(packed);
            }
        }
    }

    // 处理2位深度：每4个索引打包到1字节中
    private void process2Bit(int[] row, int width, ByteArrayOutputStream dataOs, Palette palette) {
        for (int x = 0; x < width; x += 4) {
            int packed = 0;
            for (int i = 0; i < 4; i++) {
                if (x + i < width) {
                    final int argb = row[x + i];
                    final int index = palette.getPaletteIndex(argb);
                    packed |= (index << (6 - i * 2)) & 0xFF;
                }
            }
            dataOs.write(packed);
        }
    }

    // 处理1位深度：每8个索引打包到1字节中
    private void process1Bit(int[] row, int width, ByteArrayOutputStream dataOs, Palette palette) {
        for (int x = 0; x < width; x += 8) {
            int packed = 0;
            for (int i = 0; i < 8; i++) {
                if (x + i < width) {
                    final int argb = row[x + i];
                    final int index = palette.getPaletteIndex(argb);
                    packed |= (index << (7 - i));
                }
            }
            dataOs.write(packed);
        }
    }

    private byte[] compressImageZopfli(byte[] uncompressed, int iterations) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZopfliOutputStream dos = new ZopfliOutputStream(baos, new Options(Options.OutputFormat.ZLIB, Options.BlockSplitting.FIRST, iterations))) {
            dos.write(uncompressed);
        } catch (IOException e) {
            throw new IOException("Compression failed", e);
        }
        byte[] compressedData = baos.toByteArray();
        int chunkSize = 32 * 1024;
        for (int index = 0; index < compressedData.length; index += chunkSize) {
            int end = Math.min(compressedData.length, index + chunkSize);
            byte[] chunk = Arrays.copyOfRange(compressedData, index, end);
            writeChunkIDAT(output, chunk);
        }
        return output.toByteArray();
    }

    private byte[] compressImageStandard(byte[] uncompressed) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
             final DeflaterOutputStream dos = new DeflaterOutputStream(
                     baos, new Deflater(Deflater.BEST_COMPRESSION))) {

            dos.write(uncompressed);
            dos.finish();

            final byte[] compressed = baos.toByteArray();

            final int chunkSize = 32 * 1024;
            for (int index = 0; index < compressed.length; index += chunkSize) {
                final int end = Math.min(compressed.length, index + chunkSize);
                final byte[] chunk = Arrays.copyOfRange(compressed, index, end);
                writeChunkIDAT(output, chunk);
            }
        }

        return output.toByteArray();
    }

    private void writeChunkIDAT(final OutputStream os, final byte[] bytes) throws IOException {
        writeChunk(os, IDAT, bytes);
    }

    private void writeChunkIEND(final OutputStream os) throws IOException {
        writeChunk(os, IEND, null);
    }

    private void writeChunkTRNS(final OutputStream os, final Palette palette) throws IOException {
        List<Byte> alphaValues = new ArrayList<>();
        boolean hasTransparency = false;

        for (int i = 0; i < palette.length(); i++) {
            int argb = palette.getEntry(i);
            int alpha = (argb >> 24) & 0xFF;

            if (alpha < 255) {
                hasTransparency = true;
                alphaValues.add((byte) alpha);
            } else {
                break;
            }
        }

        if (!hasTransparency) {
            return;
        }

        final byte[] bytes = new byte[alphaValues.size()];
        for (int i = 0; i < alphaValues.size(); i++) {
            bytes[i] = alphaValues.get(i);
        }

        writeChunk(os, tRNS, bytes);
    }

    private void writeChunkPLTE(final OutputStream os, final Palette palette) throws IOException {
        final int length = palette.length();
        final byte[] bytes = new byte[length * 3];
        for (int i = 0; i < length; i++) {
            final int rgb = palette.getEntry(i);
            final int index = i * 3;
            bytes[index + 0] = (byte) (0xff & rgb >> 16);
            bytes[index + 1] = (byte) (0xff & rgb >> 8);
            bytes[index + 2] = (byte) (0xff & rgb >> 0);
        }
        writeChunk(os, PLTE, bytes);
    }

    private void writeChunkIHDR(final OutputStream os, final ImageHeader value) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeInt(baos, value.width);
        writeInt(baos, value.height);
        baos.write(0xff & value.bitDepth);
        baos.write(0xff & value.pngColorType.value);
        baos.write(0xff & value.compressionMethod);
        baos.write(0xff & value.filterMethod);
        baos.write(0xff & value.interlaceMethod.ordinal());
        writeChunk(os, IHDR, baos.toByteArray());
    }

    private void writeInt(final OutputStream os, final int value) throws IOException {
        os.write(0xff & value >> 24);
        os.write(0xff & value >> 16);
        os.write(0xff & value >> 8);
        os.write(0xff & value >> 0);
    }

    private void writeChunk(final OutputStream os, final byte[] chunkType, final byte[] data) throws IOException {
        final int dataLength = data == null ? 0 : data.length;
        writeInt(os, dataLength);
        os.write(chunkType);
        if (data != null) {
            os.write(data);
        }
        writeInt(os, calculateCRC(chunkType, data)); // crc
    }

    public static int calculateCRC(byte[] chunkType, byte[] data) {
        CRC crc = new CRC();
        crc.update(chunkType, 0, chunkType.length);
        if (data != null && data.length > 0) {
            crc.update(data, 0, data.length);
        }
        return crc.getValue();
    }

    enum PngColorType {
        GREYSCALE(0), TRUE_COLOR(2),
        INDEXED_COLOR(3), GREYSCALE_WITH_ALPHA(4),
        TRUE_COLOR_WITH_ALPHA(6);

        private final int value;

        PngColorType(final int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    enum FilterType {
        NONE, SUB, UP, AVERAGE, PAETH
    }

    enum InterlaceMethod {
        NONE, ADAM7
    }

    interface Palette {

        int getEntry(int index);

        int getPaletteIndex(int rgb);

        int length();

        default int calculateBitDepth() {
            int colorCount = length();
            if (colorCount <= 2) return 1;
            if (colorCount <= 4) return 2;
            if (colorCount <= 16) return 4;
            return 8;
        }
    }

    static class ExactOpaquePalette implements Palette {
        private final int[] palette;                      // 频次排序的颜色数组
        private final Map<Integer, Integer> colorToIndex; // 颜色到索引的映射

        public ExactOpaquePalette(final Map<Integer, Integer> colorFrequency) {
            this.palette = colorFrequency.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .mapToInt(Map.Entry::getKey)
                    .toArray();
            this.colorToIndex = new HashMap<>();
            for (int i = 0; i < palette.length; i++) {
                this.colorToIndex.put(palette[i], i);
            }
        }

        @Override
        public int getEntry(int index) {
            if (index < 0 || index >= palette.length) {
                throw new IllegalArgumentException("Index out of bounds: " + index);
            }
            return palette[index];
        }

        @Override
        public int getPaletteIndex(int rgb) {
            return colorToIndex.get(rgb);
        }

        @Override
        public int length() {
            return palette.length;
        }
    }

    static class ExactTransparentPalette implements Palette {
        private final int[] palette;                      // 透明色在前，不透明色在后
        private final Map<Integer, Integer> colorToIndex; // 颜色到索引的映射

        public ExactTransparentPalette(final Map<Integer, Integer> opaque, final Map<Integer, Integer> transparent) {
            // 分别处理透明色和不透明色
            List<Integer> transparentList = transparent.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())) // 按频次降序
                    .map(Map.Entry::getKey)
                    .toList();

            List<Integer> opaqueList = opaque.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())) // 按频次降序
                    .map(Map.Entry::getKey)
                    .toList();

            // 合并：透明色在前，不透明色在后
            List<Integer> combinedList = new ArrayList<>();
            combinedList.addAll(transparentList);
            combinedList.addAll(opaqueList);

            this.palette = combinedList.stream().mapToInt(Integer::intValue).toArray();

            this.colorToIndex = new HashMap<>();
            for (int i = 0; i < palette.length; i++) {
                this.colorToIndex.put(palette[i], i);
            }
        }

        @Override
        public int getEntry(int index) {
            if (index < 0 || index >= palette.length) {
                throw new IllegalArgumentException("Index out of bounds: " + index);
            }
            return palette[index];
        }

        @Override
        public int getPaletteIndex(int rgb) {
            Integer index = colorToIndex.get(rgb);
            if (index == null) {
                throw new IllegalArgumentException("Color not found in palette: 0x" + Integer.toHexString(rgb));
            }
            return index;
        }

        @Override
        public int length() {
            return palette.length;
        }
    }

    record ImageData(PngColorType colorType, byte bitDepth, byte[] data) {
    }

    record ImageHeader(int width, int height, byte bitDepth, PngColorType pngColorType, byte compressionMethod, byte filterMethod, InterlaceMethod interlaceMethod) {
    }

    record ImageColorInfo(Map<Integer, Integer> opaque, Map<Integer, Integer> transparent, boolean isGrayscale) {

        public int uniqueColorCount() {
            return this.opaque.size() + this.transparent.size();
        }

        public boolean hasAlpha() {
            return !this.transparent.isEmpty();
        }
    }
}
