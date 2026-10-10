package net.momirealms.craftengine.core.util;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.zopfli.Options;
import net.momirealms.craftengine.core.util.zopfli.ZopfliOutputStream;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
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
    private final int zopfliIterations;

    public PngOptimizer(BufferedImage src) {
        this(src, Config.optimizeTexture() ? Config.zopfliIterations() : 0);
    }

    public PngOptimizer(BufferedImage src, int zopfliIterations) {
        this.src = src;
        this.zopfliIterations = zopfliIterations;
    }

    public static BufferedImage readPng(InputStream input) throws IOException {
        // Pack images are already in memory; avoid ImageIO's temporary-file cache.
        try (ImageInputStream stream = new MemoryCacheImageInputStream(input)) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) return null;
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, false);
                BufferedImage image = reader.read(0);
                if (isRawGrayscale(image) && image.getColorModel().hasAlpha()) {
                    restorePackedGrayTransparency(image, reader.getImageMetadata(0));
                }
                return image;
            } finally {
                reader.dispose();
            }
        }
    }

    private static void restorePackedGrayTransparency(BufferedImage image, IIOMetadata metadata) {
        String format = "javax_imageio_png_1.0";
        if (!format.equals(metadata.getNativeMetadataFormatName())) return;
        Node root = metadata.getAsTree(format);
        Node header = child(root, "IHDR");
        Node transparency = child(root, "tRNS");
        if (header == null || transparency == null) return;
        Node gray = child(transparency, "tRNS_Grayscale");
        if (gray == null) return;
        int bits = Integer.parseInt(header.getAttributes().getNamedItem("bitDepth").getNodeValue());
        if (bits >= 8) return;

        // ImageIO expands packed gray samples to 8 bits, but compares tRNS against
        // the unscaled key. Rebuild alpha using the key in the expanded range.
        int key = Integer.parseInt(gray.getAttributes().getNamedItem("gray").getNodeValue());
        int expandedKey = key * 255 / ((1 << bits) - 1);
        var raster = image.getRaster();
        int[] grayRow = new int[image.getWidth()];
        int[] alphaRow = new int[image.getWidth()];
        for (int y = 0; y < image.getHeight(); y++) {
            raster.getSamples(0, y, image.getWidth(), 1, 0, grayRow);
            for (int x = 0; x < image.getWidth(); x++) {
                alphaRow[x] = grayRow[x] == expandedKey ? 0 : 255;
            }
            raster.setSamples(0, y, image.getWidth(), 1, 1, alphaRow);
        }
    }

    private static Node child(Node parent, String name) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (name.equals(node.getNodeName())) return node;
        }
        return null;
    }

    public void write(OutputStream os) throws IOException {
        BufferedImage src = this.src;
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
        Int2IntOpenHashMap ope = new Int2IntOpenHashMap();
        Int2IntOpenHashMap tra = new Int2IntOpenHashMap();
        boolean hasAlpha = false;
        boolean hasPalette = true;
        int[] pixels = readPixels(src);

        for (int argb : pixels) {
            int alpha = (argb >> 24) & 0xFF;
            hasAlpha |= alpha != 255;
            if (!hasPalette) continue;
            if (alpha == 255) {
                ope.addTo(argb, 1);
            } else {
                tra.addTo(argb, 1);
            }
            if (ope.size() + tra.size() > 256) {
                // More than 256 colors cannot be represented by a PNG palette.
                hasPalette = false;
                ope.clear();
                tra.clear();
            }
        }

        return new ImageColorInfo(pixels, ope, tra, hasAlpha, hasPalette);
    }

    /** Supports standard ImageIO image types and non-premultiplied 8/16-bit grayscale PNGs. */
    public static boolean canOptimize(BufferedImage image) {
        return image.getType() != BufferedImage.TYPE_CUSTOM || isRawGrayscale(image);
    }

    private static boolean isRawGrayscale(BufferedImage image) {
        ColorModel model = image.getColorModel();
        if (!(model instanceof ComponentColorModel) || model.getColorSpace().getType() != ColorSpace.TYPE_GRAY
                || model.isAlphaPremultiplied()) {
            return false;
        }
        int bits = model.getComponentSize(0);
        return ((model.getTransferType() == DataBuffer.TYPE_BYTE && bits == 8)
                || (model.getTransferType() == DataBuffer.TYPE_USHORT && bits == 16))
                && (!model.hasAlpha() || model.getComponentSize(1) == bits);
    }

    private static int[] readPixels(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[Math.multiplyExact(width, height)];
        if (!isRawGrayscale(image)) {
            image.getRGB(0, 0, width, height, pixels, 0, width);
            return pixels;
        }

        // ImageIO labels PNG gray samples as linear CS_GRAY. getRGB()/Graphics2D can
        // therefore change their brightness; texture decoding must preserve the samples.
        Raster raster = image.getRaster();
        boolean hasAlpha = image.getColorModel().hasAlpha();
        int shift = image.getColorModel().getComponentSize(0) - 8;
        int[] grayRow = new int[width];
        int[] alphaRow = hasAlpha ? new int[width] : null;
        int offset = 0;
        for (int y = 0; y < height; y++) {
            raster.getSamples(0, y, width, 1, 0, grayRow);
            if (hasAlpha) raster.getSamples(0, y, width, 1, 1, alphaRow);
            for (int x = 0; x < width; x++) {
                // Match STB's 16-to-8 conversion by retaining the high byte.
                int gray = grayRow[x] >>> shift;
                int alpha = hasAlpha ? alphaRow[x] >>> shift : 255;
                pixels[offset++] = (alpha << 24) | (gray << 16) | (gray << 8) | gray;
            }
        }
        return pixels;
    }

    private ImageData findBestFileStructure(BufferedImage src, ImageColorInfo info) throws IOException {
        byte[] normalSize = tryNormal(info.pixels(), src.getWidth(), src.getHeight(), info.hasAlpha());
        // 可以考虑使用调色盘
        if (info.hasPalette()) {
            Pair<Palette, byte[]> palettePair = tryPalette(src.getWidth(), src.getHeight(), info);
            byte[] paletteSize = palettePair.right();
            if (normalSize.length > paletteSize.length) {
                return new ImageData(PngColorType.INDEXED_COLOR, (byte) palettePair.left().calculateBitDepth(), paletteSize);
            }
        }
        // RGB(A) and palette PNGs keep the same interpretation in ImageIO and STB.
        // Single-channel PNGs would reintroduce ImageIO's linear-gray conversion.
        return new ImageData(info.hasAlpha() ? PngColorType.TRUE_COLOR_WITH_ALPHA : PngColorType.TRUE_COLOR, (byte) 8, normalSize);
    }

    private byte[] tryNormal(int[] pixels, int width, int height, boolean hasAlpha) throws IOException {
        byte[] bytes = generatePngData(pixels, width, height, hasAlpha);
        int zopfli = this.zopfliIterations;
        return zopfli > 0 ? compressImageZopfli(bytes, zopfli) : compressImageStandard(bytes);
    }

    private byte[] generatePngData(int[] pixels, int width, int height, boolean hasAlpha) {
        int channels = hasAlpha ? 4 : 3;
        byte[] data = new byte[Math.multiplyExact(height, Math.addExact(1, Math.multiplyExact(width, channels)))];
        int offset = 0;
        int sourceIndex = 0;
        for (int y = 0; y < height; y++) {
            data[offset++] = (byte) FilterType.NONE.ordinal();
            for (int x = 0; x < width; x++) {
                final int argb = pixels[sourceIndex++];
                final int alpha = 0xff & argb >> 24;
                final int red = 0xff & argb >> 16;
                final int green = 0xff & argb >> 8;
                final int blue = 0xff & argb >> 0;
                data[offset++] = (byte) red;
                data[offset++] = (byte) green;
                data[offset++] = (byte) blue;
                if (hasAlpha) {
                    data[offset++] = (byte) alpha;
                }
            }
        }
        return data;
    }

    private Pair<Palette, byte[]> tryPalette(int width, int height, ImageColorInfo info) throws IOException {
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
        byte[] bytes = generatePaletteData(info.pixels(), width, height, palette);
        int zopfli = this.zopfliIterations;
        paletteOs.write(zopfli > 0 ? compressImageZopfli(bytes, zopfli) : compressImageStandard(bytes));
        return Pair.of(palette, paletteOs.toByteArray());
    }

    private byte[] generatePaletteData(int[] pixels, int width, int height, Palette palette) {
        int bitsPerIndex = palette.calculateBitDepth();
        int rowBytes = Math.toIntExact(((long) width * bitsPerIndex + 7) / 8);
        byte[] data = new byte[Math.multiplyExact(height, Math.addExact(rowBytes, 1))];
        int offset = 0;

        for (int y = 0; y < height; y++) {
            int rowOffset = y * width;
            data[offset++] = (byte) FilterType.NONE.ordinal();

            // 根据位深度选择相应的处理方法
            switch (bitsPerIndex) {
                case 4 -> process4Bit(pixels, rowOffset, width, data, offset, palette);
                case 2 -> process2Bit(pixels, rowOffset, width, data, offset, palette);
                case 1 -> process1Bit(pixels, rowOffset, width, data, offset, palette);
                default -> process8Bit(pixels, rowOffset, width, data, offset, palette);
            }
            offset += rowBytes;
        }
        return data;
    }

    // 处理8位深度：每个索引占1字节
    private void process8Bit(int[] pixels, int rowOffset, int width, byte[] data, int offset, Palette palette) {
        for (int x = 0; x < width; x++) {
            final int argb = pixels[rowOffset + x];
            final int index = palette.getPaletteIndex(argb);
            data[offset++] = (byte) index;
        }
    }

    // 处理4位深度：每2个索引打包到1字节中
    private void process4Bit(int[] pixels, int rowOffset, int width, byte[] data, int offset, Palette palette) {
        for (int x = 0; x < width; x += 2) {
            final int argb1 = pixels[rowOffset + x];
            final int index1 = palette.getPaletteIndex(argb1);

            if (x + 1 < width) {
                final int argb2 = pixels[rowOffset + x + 1];
                final int index2 = palette.getPaletteIndex(argb2);
                // 将两个4位索引打包到一个字节中
                byte packed = (byte) ((index1 << 4) | index2);
                data[offset++] = packed;
            } else {
                // 如果是奇数宽度，最后一个像素单独处理
                byte packed = (byte) (index1 << 4);
                data[offset++] = packed;
            }
        }
    }

    // 处理2位深度：每4个索引打包到1字节中
    private void process2Bit(int[] pixels, int rowOffset, int width, byte[] data, int offset, Palette palette) {
        for (int x = 0; x < width; x += 4) {
            int packed = 0;
            for (int i = 0; i < 4; i++) {
                if (x + i < width) {
                    final int argb = pixels[rowOffset + x + i];
                    final int index = palette.getPaletteIndex(argb);
                    packed |= (index << (6 - i * 2)) & 0xFF;
                }
            }
            data[offset++] = (byte) packed;
        }
    }

    // 处理1位深度：每8个索引打包到1字节中
    private void process1Bit(int[] pixels, int rowOffset, int width, byte[] data, int offset, Palette palette) {
        for (int x = 0; x < width; x += 8) {
            int packed = 0;
            for (int i = 0; i < 8; i++) {
                if (x + i < width) {
                    final int argb = pixels[rowOffset + x + i];
                    final int index = palette.getPaletteIndex(argb);
                    packed |= (index << (7 - i));
                }
            }
            data[offset++] = (byte) packed;
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
            int length = Math.min(chunkSize, compressedData.length - index);
            writeChunkIDAT(output, compressedData, index, length);
        }
        return output.toByteArray();
    }

    private byte[] compressImageStandard(byte[] uncompressed) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);

        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
             final DeflaterOutputStream dos = new DeflaterOutputStream(
                     baos, deflater)) {

            dos.write(uncompressed);
            dos.finish();

            final byte[] compressed = baos.toByteArray();

            final int chunkSize = 32 * 1024;
            for (int index = 0; index < compressed.length; index += chunkSize) {
                final int length = Math.min(chunkSize, compressed.length - index);
                writeChunkIDAT(output, compressed, index, length);
            }
        } finally {
            deflater.end();
        }

        return output.toByteArray();
    }

    private void writeChunkIDAT(final OutputStream os, final byte[] bytes, int offset, int length) throws IOException {
        writeChunk(os, IDAT, bytes, offset, length);
    }

    private void writeChunkIEND(final OutputStream os) throws IOException {
        writeChunk(os, IEND, null);
    }

    private void writeChunkTRNS(final OutputStream os, final Palette palette) throws IOException {
        int transparentColors = 0;
        while (transparentColors < palette.length() && (palette.getEntry(transparentColors) >>> 24) < 255) {
            transparentColors++;
        }

        if (transparentColors == 0) {
            return;
        }

        final byte[] bytes = new byte[transparentColors];
        for (int i = 0; i < transparentColors; i++) {
            bytes[i] = (byte) (palette.getEntry(i) >>> 24);
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
        writeChunk(os, chunkType, data, 0, dataLength);
    }

    private void writeChunk(final OutputStream os, final byte[] chunkType, final byte[] data, int offset, int length) throws IOException {
        writeInt(os, length);
        os.write(chunkType);
        if (length > 0) {
            os.write(data, offset, length);
        }
        writeInt(os, calculateCRC(chunkType, data, offset, length)); // crc
    }

    public static int calculateCRC(byte[] chunkType, byte[] data) {
        return calculateCRC(chunkType, data, 0, data == null ? 0 : data.length);
    }

    private static int calculateCRC(byte[] chunkType, byte[] data, int offset, int length) {
        CRC32 crc = new CRC32();
        crc.update(chunkType, 0, chunkType.length);
        if (length > 0) {
            crc.update(data, offset, length);
        }
        return (int) crc.getValue();
    }

    enum PngColorType {
        TRUE_COLOR(2), INDEXED_COLOR(3),
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

    private static int[] sortColors(Int2IntMap colorFrequency) {
        IntArrayList colors = new IntArrayList(colorFrequency.size());
        // Match the previous stream traversal order before sorting equal-frequency colors.
        colorFrequency.keySet().spliterator().forEachRemaining((int color) -> colors.add(color));
        int[] palette = colors.elements();
        IntArrays.stableSort(palette, (a, b) -> Integer.compare(colorFrequency.get(b), colorFrequency.get(a)));
        return palette;
    }

    static class ExactOpaquePalette implements Palette {
        private final int[] palette;                      // 频次排序的颜色数组
        private final Int2IntOpenHashMap colorToIndex;     // 颜色到索引的映射

        public ExactOpaquePalette(final Int2IntMap colorFrequency) {
            this.palette = sortColors(colorFrequency);
            this.colorToIndex = new Int2IntOpenHashMap(palette.length);
            this.colorToIndex.defaultReturnValue(-1);
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
            int index = colorToIndex.get(rgb);
            if (index < 0) {
                throw new IllegalArgumentException("Color not found in palette: 0x" + Integer.toHexString(rgb));
            }
            return index;
        }

        @Override
        public int length() {
            return palette.length;
        }
    }

    static class ExactTransparentPalette implements Palette {
        private final int[] palette;                      // 透明色在前，不透明色在后
        private final Int2IntOpenHashMap colorToIndex;     // 颜色到索引的映射

        public ExactTransparentPalette(final Int2IntMap opaque, final Int2IntMap transparent) {
            // 分别处理透明色和不透明色
            int[] transparentColors = sortColors(transparent);
            int[] opaqueColors = sortColors(opaque);

            // 合并：透明色在前，不透明色在后
            this.palette = new int[transparentColors.length + opaqueColors.length];
            System.arraycopy(transparentColors, 0, this.palette, 0, transparentColors.length);
            System.arraycopy(opaqueColors, 0, this.palette, transparentColors.length, opaqueColors.length);

            this.colorToIndex = new Int2IntOpenHashMap(palette.length);
            this.colorToIndex.defaultReturnValue(-1);
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
            int index = colorToIndex.get(rgb);
            if (index < 0) {
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

    record ImageColorInfo(int[] pixels, Int2IntMap opaque, Int2IntMap transparent, boolean hasAlpha, boolean hasPalette) {
    }
}
