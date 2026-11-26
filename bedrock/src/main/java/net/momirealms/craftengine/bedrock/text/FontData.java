package net.momirealms.craftengine.bedrock.text;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

import java.io.IOException;
import java.util.*;
import java.util.function.ToDoubleFunction;

public final class FontData {
    public static final float MISSING_ADVANCE = 6;
    private static final float DEFAULT_BOLD_OFFSET = 1;
    public static final FontData EMPTY = new FontData(Map.of());
    private final Map<String, Font> fonts;

    public FontData(Map<String, Font> fonts) {
        this.fonts = Collections.unmodifiableMap(new LinkedHashMap<>(fonts));
    }

    public Font font(String id) {
        return this.fonts.getOrDefault(id, Font.MISSING);
    }

    public Map<String, Font> fonts() {
        return this.fonts;
    }

    public void write(JsonWriter writer) throws IOException {
        FontDataJson.INSTANCE.write(writer, this);
    }

    public static FontData read(JsonReader reader) throws IOException {
        return FontDataJson.INSTANCE.read(reader);
    }

    public JsonObject toJson() {
        return FontDataJson.INSTANCE.toJsonTree(this).getAsJsonObject();
    }

    public static FontData fromJson(JsonObject root) {
        return FontDataJson.INSTANCE.fromJsonTree(root);
    }

    public static final class Font {
        private static final Font MISSING = new Font(Int2ObjectMaps.emptyMap());
        final Int2ObjectMap<WidthPage> pages = new Int2ObjectOpenHashMap<>();
        final BitSet hudUnicode;
        private final Int2ObjectMap<FontGlyph> rendering;

        Font(Int2ObjectMap<FontGlyph> rendering) {
            this(rendering, null);
        }

        Font(Int2ObjectMap<FontGlyph> rendering, BitSet hudUnicode) {
            this.rendering = Int2ObjectMaps.unmodifiable(rendering);
            this.hudUnicode = hudUnicode;
        }

        public static <T> Font of(Int2ObjectMap<T> glyphs, ToDoubleFunction<T> advance, ToDoubleFunction<T> boldOffset,
                                  Int2ObjectMap<FontGlyph> rendering) {
            return of(glyphs, advance, boldOffset, rendering, null);
        }

        public static <T> Font of(Int2ObjectMap<T> glyphs, ToDoubleFunction<T> advance, ToDoubleFunction<T> boldOffset,
                                  Int2ObjectMap<FontGlyph> rendering, BitSet hudUnicode) {
            Font font = new Font(rendering, hudUnicode);
            Int2ObjectMaps.fastForEach(glyphs, entry -> {
                int code = entry.getIntKey();
                WidthPage page = font.pages.computeIfAbsent(code >>> 8, (int key) -> new WidthPage());
                page.advances[code & 255] = (float) advance.applyAsDouble(entry.getValue());
                page.boldOffsets[code & 255] = (float) boldOffset.applyAsDouble(entry.getValue());
            });
            return font;
        }

        public float advance(int code, boolean bold) {
            WidthPage page = this.pages.get(code >>> 8);
            if (page == null) {
                return bold ? MISSING_ADVANCE + DEFAULT_BOLD_OFFSET : MISSING_ADVANCE;
            }
            int index = code & 255;
            return page.advances[index] + (bold ? page.boldOffsets[index] : 0);
        }

        public Int2ObjectMap<FontGlyph> rendering() {
            return this.rendering;
        }

        public boolean hudUnicode(int codepoint) {
            return this.hudUnicode != null && this.hudUnicode.get(codepoint);
        }
    }

    static final class WidthPage {
        private final float[] advances = new float[256];
        private float[] boldOffsets = new float[256];

        private WidthPage() {
            Arrays.fill(this.advances, MISSING_ADVANCE);
            Arrays.fill(this.boldOffsets, DEFAULT_BOLD_OFFSET);
        }

        void write(JsonWriter writer) throws IOException {
            writer.beginArray();
            int first = 0;
            while (first < 256) {
                int end = first + 1;
                while (end < 256 && this.advances[end] == this.advances[first] && this.boldOffsets[end] == this.boldOffsets[first]) {
                    end++;
                }
                if (this.advances[first] != MISSING_ADVANCE || this.boldOffsets[first] != DEFAULT_BOLD_OFFSET) {
                    writer.value(first);
                    writer.value(end - 1);
                    writer.value(this.advances[first]);
                    writer.value(this.boldOffsets[first]);
                }
                first = end;
            }
            writer.endArray();
        }

        static WidthPage read(JsonReader reader, ObjectOpenCustomHashSet<float[]> sharedBoldOffsets) throws IOException {
            WidthPage page = new WidthPage();
            reader.beginArray();
            while (reader.hasNext()) {
                int start = reader.nextInt();
                int end = reader.nextInt() + 1;
                Arrays.fill(page.advances, start, end, Float.parseFloat(reader.nextString()));
                Arrays.fill(page.boldOffsets, start, end, Float.parseFloat(reader.nextString()));
            }
            reader.endArray();
            // 不同字宽页经常有完全相同的粗体增量。加载结束后数组只读，查询仍是直接下标访问。
            page.boldOffsets = sharedBoldOffsets.addOrGet(page.boldOffsets);
            return page;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof WidthPage page && Arrays.equals(this.advances, page.advances)
                    && Arrays.equals(this.boldOffsets, page.boldOffsets);
        }

        @Override
        public int hashCode() {
            return 31 * Arrays.hashCode(this.advances) + Arrays.hashCode(this.boldOffsets);
        }
    }
}
