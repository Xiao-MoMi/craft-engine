package net.momirealms.craftengine.bedrock.text;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

public final class FontData {
    private static final int VERSION = 1;
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

    public JsonObject toJson() {
        JsonArray tables = new JsonArray();
        Map<WidthPage, Integer> ids = new HashMap<>();
        JsonObject definitions = new JsonObject();
        for (Map.Entry<String, Font> entry : this.fonts.entrySet()) {
            Font font = entry.getValue();
            JsonArray indices = new JsonArray();
            int[] keys = font.pages.keySet().toIntArray();
            IntArrays.quickSort(keys);
            for (int key : keys) {
                WidthPage page = font.pages.get(key);
                Integer id = ids.putIfAbsent(page, tables.size());
                if (id == null) {
                    id = tables.size();
                    tables.add(page.toJson());
                }
                indices.add(key);
                indices.add(id);
            }
            JsonObject rendering = new JsonObject();
            for (Int2ObjectMap.Entry<FontGlyph> glyph : font.rendering.int2ObjectEntrySet()) {
                int code = glyph.getIntKey();
                FontGlyph value = glyph.getValue();
                JsonObject record = new JsonObject();
                if (value.character() != code) {
                    record.addProperty("character", value.character());
                }
                if (value.renderedAdvance() >= 0) {
                    record.addProperty("rendered_advance", value.renderedAdvance());
                }
                if (value.bearing() != 0) {
                    record.addProperty("bearing", value.bearing());
                }
                rendering.add(Integer.toHexString(code), record);
            }
            JsonObject definition = new JsonObject();
            definition.add("pages", indices);
            definition.add("rendering", rendering);
            definitions.add(entry.getKey(), definition);
        }
        JsonObject root = new JsonObject();
        root.addProperty("version", VERSION);
        root.add("width_pages", tables);
        root.add("fonts", definitions);
        return root;
    }

    public static FontData fromJson(JsonObject root) {
        if (root == null || !root.has("version") || root.get("version").getAsInt() != VERSION) {
            throw new IllegalArgumentException("Missing or unsupported craftengine/fonts.json; regenerate the Bedrock pack");
        }
        JsonArray tables = root.getAsJsonArray("width_pages");
        WidthPage[] pages = new WidthPage[tables.size()];
        for (int i = 0; i < pages.length; i++) {
            pages[i] = WidthPage.fromJson(tables.get(i).getAsJsonArray());
        }
        Map<String, Font> fonts = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("fonts").entrySet()) {
            JsonObject definition = entry.getValue().getAsJsonObject();
            JsonArray indices = definition.getAsJsonArray("pages");
            Int2ObjectMap<FontGlyph> rendering = new Int2ObjectOpenHashMap<>();
            Font font = new Font(rendering);
            for (int i = 0; i < indices.size(); i += 2) {
                font.pages.put(indices.get(i).getAsInt(), pages[indices.get(i + 1).getAsInt()]);
            }
            for (Map.Entry<String, JsonElement> glyph : definition.getAsJsonObject("rendering").entrySet()) {
                int code = Integer.parseInt(glyph.getKey(), 16);
                JsonObject record = glyph.getValue().getAsJsonObject();
                rendering.put(code, new FontGlyph(record.has("character") ? record.get("character").getAsInt() : code,
                        record.has("rendered_advance") ? record.get("rendered_advance").getAsInt() : -1,
                        record.has("bearing") ? record.get("bearing").getAsInt() : 0));
            }
            fonts.put(entry.getKey(), font);
        }
        return new FontData(fonts);
    }

    public static final class Font {
        private static final Font MISSING = new Font(Int2ObjectMaps.emptyMap());
        private final Int2ObjectMap<WidthPage> pages = new Int2ObjectOpenHashMap<>();
        private final Int2ObjectMap<FontGlyph> rendering;

        private Font(Int2ObjectMap<FontGlyph> rendering) {
            this.rendering = Int2ObjectMaps.unmodifiable(rendering);
        }

        public static <T> Font of(Int2ObjectMap<T> glyphs, ToDoubleFunction<T> advance, ToDoubleFunction<T> boldOffset,
                                  Int2ObjectMap<FontGlyph> rendering) {
            Font font = new Font(rendering);
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
    }

    private static final class WidthPage {
        private final float[] advances = new float[256];
        private final float[] boldOffsets = new float[256];

        private WidthPage() {
            Arrays.fill(this.advances, MISSING_ADVANCE);
            Arrays.fill(this.boldOffsets, DEFAULT_BOLD_OFFSET);
        }

        private JsonArray toJson() {
            JsonArray ranges = new JsonArray();
            int first = 0;
            while (first < 256) {
                int end = first + 1;
                while (end < 256 && this.advances[end] == this.advances[first] && this.boldOffsets[end] == this.boldOffsets[first]) {
                    end++;
                }
                if (this.advances[first] != MISSING_ADVANCE || this.boldOffsets[first] != DEFAULT_BOLD_OFFSET) {
                    ranges.add(first);
                    ranges.add(end - 1);
                    ranges.add(this.advances[first]);
                    ranges.add(this.boldOffsets[first]);
                }
                first = end;
            }
            return ranges;
        }

        private static WidthPage fromJson(JsonArray ranges) {
            WidthPage page = new WidthPage();
            for (int i = 0; i < ranges.size(); i += 4) {
                int start = ranges.get(i).getAsInt();
                int end = ranges.get(i + 1).getAsInt() + 1;
                Arrays.fill(page.advances, start, end, ranges.get(i + 2).getAsFloat());
                Arrays.fill(page.boldOffsets, start, end, ranges.get(i + 3).getAsFloat());
            }
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
