package net.momirealms.craftengine.bedrock.text;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

import java.io.IOException;
import java.util.*;

final class FontDataJson extends TypeAdapter<FontData> {
    static final FontDataJson INSTANCE = new FontDataJson();
    private static final int VERSION = 1;

    @Override
    public void write(JsonWriter writer, FontData data) throws IOException {
        writer.beginObject();
        writer.name("version").value(VERSION);
        writer.name("width_pages");
        Map<String, int[]> references = writeWidthPages(writer, data.fonts());
        writer.name("fonts").beginObject();
        for (Map.Entry<String, FontData.Font> entry : data.fonts().entrySet()) {
            writer.name(entry.getKey()).beginObject();
            writer.name("pages").beginArray();
            for (int number : references.get(entry.getKey())) {
                writer.value(number);
            }
            writer.endArray();
            if (entry.getValue().nativeFallback) {
                writer.name("native_fallback").value(true);
            }
            BitSet unicode = entry.getValue().hudUnicode;
            if (unicode != null) {
                writer.name("hud_unicode_ranges").beginArray();
                for (int start = unicode.nextSetBit(0); start >= 0;) {
                    int end = unicode.nextClearBit(start);
                    writer.value(start).value(end);
                    start = unicode.nextSetBit(end);
                }
                writer.endArray();
            }
            writer.name("rendering");
            writeRendering(writer, entry.getValue().rendering());
            writer.endObject();
        }
        writer.endObject();
        writer.endObject();
    }

    private static Map<String, int[]> writeWidthPages(JsonWriter writer, Map<String, FontData.Font> fonts) throws IOException {
        writer.beginArray();
        Object2IntOpenHashMap<FontData.WidthPage> ids = new Object2IntOpenHashMap<>();
        ids.defaultReturnValue(-1);
        Map<String, int[]> references = new LinkedHashMap<>();
        for (Map.Entry<String, FontData.Font> entry : fonts.entrySet()) {
            Int2ObjectMap<FontData.WidthPage> pages = entry.getValue().pages;
            int[] keys = pages.keySet().toIntArray();
            IntArrays.quickSort(keys);
            int[] pairs = new int[keys.length * 2];
            for (int i = 0; i < keys.length; i++) {
                FontData.WidthPage page = pages.get(keys[i]);
                int next = ids.size();
                int id = ids.putIfAbsent(page, next);
                if (id == -1) {
                    id = next;
                    page.write(writer);
                }
                pairs[i * 2] = keys[i];
                pairs[i * 2 + 1] = id;
            }
            references.put(entry.getKey(), pairs);
        }
        writer.endArray();
        return references;
    }

    private static void writeRendering(JsonWriter writer, Int2ObjectMap<FontGlyph> glyphs) throws IOException {
        writer.beginObject();
        for (Int2ObjectMap.Entry<FontGlyph> glyph : Int2ObjectMaps.fastIterable(glyphs)) {
            int code = glyph.getIntKey();
            FontGlyph value = glyph.getValue();
            writer.name(Integer.toHexString(code)).beginObject();
            if (value.character() != code) {
                writer.name("character").value(value.character());
            }
            if (value.renderedAdvance() >= 0 || value.renderedAdvance() == FontGlyph.NATIVE_FALLBACK) {
                writer.name("rendered_advance").value(value.renderedAdvance());
            }
            if (value.bearing() != 0) {
                writer.name("bearing").value(value.bearing());
            }
            writer.endObject();
        }
        writer.endObject();
    }

    @Override
    public FontData read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            throw new IllegalArgumentException("Missing craftengine/fonts.json; regenerate the Bedrock pack");
        }
        int version = -1;
        List<FontData.WidthPage> pages = null;
        Map<String, Definition> definitions = null;
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "version" -> version = reader.nextInt();
                case "width_pages" -> {
                    pages = new ArrayList<>();
                    ObjectOpenCustomHashSet<float[]> sharedBoldOffsets = new ObjectOpenCustomHashSet<>(FloatArrays.HASH_STRATEGY);
                    reader.beginArray();
                    while (reader.hasNext()) {
                        pages.add(FontData.WidthPage.read(reader, sharedBoldOffsets));
                    }
                    reader.endArray();
                }
                case "fonts" -> {
                    definitions = new LinkedHashMap<>();
                    reader.beginObject();
                    while (reader.hasNext()) {
                        definitions.put(reader.nextName(), readDefinition(reader));
                    }
                    reader.endObject();
                }
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        if (version != VERSION || pages == null || definitions == null) {
            throw new IllegalArgumentException("Missing or unsupported craftengine/fonts.json; regenerate the Bedrock pack");
        }

        // fonts 和 width_pages 可以任意顺序出现。所有定义读取完成后再绑定共享的字宽页。
        Map<String, FontData.Font> fonts = new LinkedHashMap<>();
        for (Map.Entry<String, Definition> entry : definitions.entrySet()) {
            Definition definition = entry.getValue();
            FontData.Font font = new FontData.Font(definition.rendering, definition.hudUnicode, definition.nativeFallback);
            IntArrayList pairs = definition.pages;
            for (int i = 0; i < pairs.size(); i += 2) {
                font.pages.put(pairs.getInt(i), pages.get(pairs.getInt(i + 1)));
            }
            fonts.put(entry.getKey(), font);
        }
        return new FontData(fonts);
    }

    private static Definition readDefinition(JsonReader reader) throws IOException {
        IntArrayList pages = null;
        BitSet hudUnicode = null;
        Int2ObjectMap<FontGlyph> rendering = null;
        boolean nativeFallback = false;
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "native_fallback" -> nativeFallback = reader.nextBoolean();
                case "hud_unicode_ranges" -> {
                    hudUnicode = new BitSet();
                    reader.beginArray();
                    while (reader.hasNext()) {
                        int start = reader.nextInt();
                        int end = reader.nextInt();
                        if (start < 0 || end < start || end > Character.MAX_CODE_POINT + 1) {
                            throw new IllegalArgumentException("Invalid HUD font range");
                        }
                        hudUnicode.set(start, end);
                    }
                    reader.endArray();
                }
                case "pages" -> {
                    pages = new IntArrayList();
                    reader.beginArray();
                    while (reader.hasNext()) {
                        pages.add(reader.nextInt());
                    }
                    reader.endArray();
                }
                case "rendering" -> {
                    rendering = new Int2ObjectOpenHashMap<>();
                    reader.beginObject();
                    while (reader.hasNext()) {
                        int code = Integer.parseInt(reader.nextName(), 16);
                        rendering.put(code, readGlyph(reader, code));
                    }
                    reader.endObject();
                }
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        if (pages == null || rendering == null) {
            throw new IllegalArgumentException("Incomplete font definition in craftengine/fonts.json");
        }
        return new Definition(pages, rendering, hudUnicode, nativeFallback);
    }

    private static FontGlyph readGlyph(JsonReader reader, int code) throws IOException {
        int character = code;
        int advance = -1;
        int bearing = 0;
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "character" -> character = reader.nextInt();
                case "rendered_advance" -> advance = reader.nextInt();
                case "bearing" -> bearing = reader.nextInt();
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        return new FontGlyph(character, advance, bearing);
    }

    private record Definition(IntArrayList pages, Int2ObjectMap<FontGlyph> rendering, BitSet hudUnicode, boolean nativeFallback) {
    }
}
