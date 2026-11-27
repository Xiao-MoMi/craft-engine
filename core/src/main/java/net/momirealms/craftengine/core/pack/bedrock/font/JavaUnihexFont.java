package net.momirealms.craftengine.core.pack.bedrock.font;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.CharBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class JavaUnihexFont {
    private final byte[][] glyphWidths = new byte[(Character.MAX_CODE_POINT >>> 8) + 1][];
    private int glyphCount;

    JavaUnihexFont(InputStream input) throws IOException {
        // 最长一行是 6 位码点、冒号和 128 位十六进制数据，多留一字节识别超长输入。
        byte[] lineBytes = new byte[6 + 1 + 128 + 1];
        char[] characters = new char[lineBytes.length];
        CharBuffer line = CharBuffer.wrap(characters);
        try (ZipInputStream archive = new ZipInputStream(input)) {
            boolean foundHex = false;
            ZipEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().endsWith(".hex")) {
                    continue;
                }
                foundHex = true;
                FastBufferedInputStream reader = new FastBufferedInputStream(archive);
                int length;
                while ((length = reader.readLine(lineBytes)) >= 0) {
                    for (int i = 0; i < length; i++) {
                        characters[i] = (char) (lineBytes[i] & 255);
                    }
                    this.readGlyph(line, length, entry.getName());
                }
            }
            if (!foundHex) {
                throw new IOException("Missing .hex entry");
            }
        }
    }

    private void readGlyph(CharBuffer line, int length, String filename) throws IOException {
        int separator = 0;
        while (separator < length && line.charAt(separator) != ':') {
            separator++;
        }
        int hexLength = length - separator - 1;
        if (separator < 4 || separator > 6 || (hexLength != 32 && hexLength != 64 && hexLength != 96 && hexLength != 128)) {
            throw new IOException("Invalid unihex entry " + filename);
        }
        try {
            int codepoint = Integer.parseInt(line, 0, separator, 16);
            if (!Character.isValidCodePoint(codepoint)) {
                throw new IOException("Invalid codepoint in " + filename);
            }
            int rowLength = hexLength / 16;
            int bitWidth = rowLength * 4;
            int mask = 0;
            for (int start = separator + 1; start < length; start += rowLength) {
                mask |= Integer.parseUnsignedInt(line, start, start + rowLength, 16) << (32 - bitWidth);
            }
            int left = mask == 0 ? 0 : Integer.numberOfLeadingZeros(mask);
            int right = mask == 0 ? bitWidth : 31 - Integer.numberOfTrailingZeros(mask);
            int pageIndex = codepoint >>> 8;
            byte[] page = this.glyphWidths[pageIndex];
            if (page == null) {
                page = new byte[256];
                this.glyphWidths[pageIndex] = page;
            }
            int slot = codepoint & 255;
            if (page[slot] == 0) {
                this.glyphCount++;
            }
            // 原始字宽在 1–33 之间，0 留给缺字；后出现的同码点定义优先。
            page[slot] = (byte) (right - left + 1);
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid unihex entry " + filename, exception);
        }
    }

    void mergeInto(Int2ObjectOpenHashMap<Glyph> glyphs, JsonArray sizes) {
        WidthOverride[] overrides = new WidthOverride[sizes == null ? 0 : sizes.size()];
        for (int i = 0; i < overrides.length; i++) {
            JsonObject size = sizes.get(i).getAsJsonObject();
            overrides[i] = new WidthOverride(
                    size.get("from").getAsString().codePointAt(0),
                    size.get("to").getAsString().codePointAt(0),
                    size.get("left").getAsInt(),
                    size.get("right").getAsInt()
            );
        }
        glyphs.ensureCapacity(this.glyphCount);
        Int2ObjectMap<Glyph> widths = new Int2ObjectOpenHashMap<>();
        for (int pageIndex = 0; pageIndex < this.glyphWidths.length; pageIndex++) {
            byte[] page = this.glyphWidths[pageIndex];
            if (page == null) {
                continue;
            }
            for (int slot = 0; slot < page.length; slot++) {
                int width = page[slot];
                if (width == 0) {
                    continue;
                }
                int codepoint = (pageIndex << 8) | slot;
                for (WidthOverride override : overrides) {
                    if (codepoint >= override.from() && codepoint <= override.to()) {
                        width = override.right() - override.left() + 1;
                        break;
                    }
                }
                glyphs.putIfAbsent(codepoint, widths.computeIfAbsent(width, Glyph::unihex));
            }
        }
    }

    private record WidthOverride(int from, int to, int left, int right) {
    }
}
