package net.momirealms.craftengine.core.pack.bedrock.font;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.momirealms.craftengine.core.pack.bedrock.font.JavaFontResolver.Glyph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class JavaUnihexFont {
    private final Int2IntMap dimensions;

    JavaUnihexFont(InputStream input) throws IOException {
        this.dimensions = new Int2IntOpenHashMap();
        try (ZipInputStream archive = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().endsWith(".hex")) {
                    continue;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(archive, StandardCharsets.US_ASCII));
                String line;
                while ((line = reader.readLine()) != null) {
                    int separator = line.indexOf(':');
                    int length = line.length() - separator - 1;
                    if (separator < 4 || separator > 6 || (length != 32 && length != 64 && length != 96 && length != 128)) {
                        throw new IOException("Invalid unihex glyph in " + entry.getName());
                    }
                    try {
                        int codepoint = Integer.parseInt(line, 0, separator, 16);
                        int rowLength = length / 16;
                        int bitWidth = rowLength * 4;
                        int mask = 0;
                        for (int row = 0; row < 16; row++) {
                            int start = separator + 1 + row * rowLength;
                            mask |= Integer.parseUnsignedInt(line, start, start + rowLength, 16) << (32 - bitWidth);
                        }
                        int left = mask == 0 ? 0 : Integer.numberOfLeadingZeros(mask);
                        int right = mask == 0 ? bitWidth : 31 - Integer.numberOfTrailingZeros(mask);
                        this.dimensions.put(codepoint, (left << 8) | right);
                    } catch (NumberFormatException exception) {
                        throw new IOException("Invalid unihex hex digits in " + entry.getName(), exception);
                    }
                }
            }
        }
    }

    Int2ObjectMap<Glyph> glyphs(JsonArray sizes) {
        Override[] overrides = new Override[sizes == null ? 0 : sizes.size()];
        for (int i = 0; i < overrides.length; i++) {
            JsonObject size = sizes.get(i).getAsJsonObject();
            overrides[i] = new Override(
                    size.get("from").getAsString().codePointAt(0),
                    size.get("to").getAsString().codePointAt(0),
                    size.get("left").getAsInt(),
                    size.get("right").getAsInt()
            );
        }
        Int2ObjectMap<Glyph> glyphs = new Int2ObjectOpenHashMap<>(this.dimensions.size());
        Int2IntMaps.fastForEach(this.dimensions, entry -> {
            int codepoint = entry.getIntKey();
            int left = entry.getIntValue() >>> 8;
            int right = entry.getIntValue() & 255;
            for (Override override : overrides) {
                if (codepoint >= override.from() && codepoint <= override.to()) {
                    left = override.left();
                    right = override.right();
                    break;
                }
            }
            int width = right - left + 1;
            glyphs.put(codepoint, new Glyph(null, left, 0, width, 16, 8, 7, width / 2 + 1, true));
        });
        return glyphs;
    }

    private record Override(int from, int to, int left, int right) {
    }
}
