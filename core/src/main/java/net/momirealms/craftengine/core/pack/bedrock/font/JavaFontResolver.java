package net.momirealms.craftengine.core.pack.bedrock.font;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.util.ClientPngReader;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public final class JavaFontResolver {
    private final JavaResourcePack resources;
    private final Map<String, BufferedImage> images = new HashMap<>();
    private final Map<String, JavaUnihexFont> unihexFonts = new HashMap<>();
    private final Map<Key, Int2ObjectMap<Glyph>> resolved = new HashMap<>();
    private final Set<Key> resolving = new HashSet<>();
    private final JsonArray diagnostics = new JsonArray();
    private final Map<Key, List<JavaResourcePack.FontResource>> fonts;

    public JavaFontResolver(JavaResourcePack resources) throws IOException {
        this.resources = resources;
        this.fonts = resources.fonts();
    }

    public Set<Key> fonts() {
        return this.fonts.keySet();
    }

    public JsonArray diagnostics() {
        return this.diagnostics;
    }

    public Int2ObjectMap<Glyph> resolve(Key font) throws IOException {
        Int2ObjectMap<Glyph> cached = this.resolved.get(font);
        if (cached != null) {
            return cached;
        }
        if (!this.resolving.add(font)) {
            throw new IOException("Circular font reference " + font);
        }
        Int2ObjectMap<Glyph> glyphs = new Int2ObjectOpenHashMap<>();
        List<JavaResourcePack.FontResource> definitions = this.fonts.get(font);
        if (definitions == null) {
            throw new IOException("Missing font " + font + " at assets/" + font.namespace() + "/font/" + font.value() + ".json");
        }
        // Higher resource packs win; within each file the first provider containing the character wins.
        boolean stop = false;
        for (JavaResourcePack.FontResource resource : definitions) {
            for (JsonElement value : resource.definition().getAsJsonArray("providers")) {
                JsonObject provider = value.getAsJsonObject();
                if (provider.has("filter") && provider.getAsJsonObject("filter").entrySet().stream().anyMatch(entry -> entry.getValue().getAsBoolean())) {
                    continue;
                }
                Key type = Key.of(provider.get("type").getAsString());
                if (!type.isMinecraftNamespace()) {
                    continue;
                }
                switch (type.value) {
                    case "bitmap" -> {
                        Int2ObjectMap<Glyph> bitmap = this.bitmap(font, provider, resource.custom());
                        Int2ObjectMaps.fastForEach(bitmap, entry -> glyphs.putIfAbsent(entry.getIntKey(), entry.getValue()));
                    }
                    case "reference" -> Int2ObjectMaps.fastForEach(this.resolve(Key.of(provider.get("id").getAsString())), entry -> glyphs.putIfAbsent(entry.getIntKey(), entry.getValue()));
                    case "space" -> {
                        for (String character : provider.getAsJsonObject("advances").keySet()) {
                            if (!glyphs.containsKey(character.codePointAt(0))) {
                                glyphs.put(character.codePointAt(0), new Glyph(null, 0, 0, 0, 0, 0, 0, provider.getAsJsonObject("advances").get(character).getAsFloat()));
                            }
                        }
                    }
                    case "unihex" -> {
                        Key file = Key.of(provider.get("hex_file").getAsString());
                        String path = "assets/" + file.namespace() + "/" + file.value();
                        if (resource.custom() || this.resources.hasCustomResource(path)) {
                            this.diagnostics.add(font + ": custom unihex glyph conversion is not supported; lower-priority providers are not converted");
                            stop = true;
                            break;
                        }
                        JavaUnihexFont unihex = this.unihexFonts.get(path);
                        if (unihex == null) {
                            InputStream input = this.resources.openResource(path);
                            if (input == null) {
                                throw new IOException("Missing unihex archive " + path + " in font " + font);
                            }
                            unihex = new JavaUnihexFont(input);
                            this.unihexFonts.put(path, unihex);
                        }
                        Int2ObjectMaps.fastForEach(unihex.glyphs(provider.getAsJsonArray("size_overrides")), entry -> glyphs.putIfAbsent(entry.getIntKey(), entry.getValue()));
                    }
                    default -> {
                        // Unknown coverage must not expose a lower-priority bitmap as an effective glyph.
                        if (resource.custom()) {
                            this.diagnostics.add(font + ": unsupported provider " + type + "; lower-priority providers are not converted");
                        }
                        stop = true;
                    }
                }
                if (stop) {
                    break;
                }
            }
            if (stop) {
                break;
            }
        }
        this.resolving.remove(font);
        this.resolved.put(font, glyphs);
        return glyphs;
    }

    private Int2ObjectMap<Glyph> bitmap(Key font, JsonObject provider, boolean custom) throws IOException {
        Key texture = Key.of(provider.get("file").getAsString());
        String path = "assets/" + texture.namespace() + "/textures/" + texture.value();
        JsonArray rows = provider.getAsJsonArray("chars");
        int[][] characters = new int[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            characters[i] = rows.get(i).getAsString().codePoints().toArray();
        }
        Int2ObjectMap<Glyph> glyphs = new Int2ObjectOpenHashMap<>();
        boolean convert = custom || this.resources.hasCustomResource(path);
        int height = provider.has("height") ? provider.get("height").getAsInt() : 8;
        int ascent = provider.get("ascent").getAsInt();
        BufferedImage image = this.images.get(path);
        if (image == null) {
            try (InputStream input = this.resources.openResource(path)) {
                if (input == null || (image = ClientPngReader.read(input)) == null) {
                    throw new IOException("Missing or invalid bitmap " + path + " in font " + font);
                }
            }
            this.images.put(path, image);
        }
        int width = image.getWidth() / characters[0].length;
        int cellHeight = image.getHeight() / characters.length;
        if (width == 0 || cellHeight == 0) {
            throw new IOException("Empty bitmap cell " + path + " in font " + font);
        }
        for (int y = 0; y < characters.length; y++) {
            if (characters[y].length != characters[0].length) {
                throw new IOException("Unequal bitmap rows in font " + font + " [" + path + "]");
            }
            for (int x = 0; x < characters[y].length; x++) {
                int codepoint = characters[y][x];
                if (codepoint != 0) {
                    glyphs.put(codepoint, new Glyph(image, x * width, y * cellHeight, width, cellHeight, height, ascent, Float.NaN, !convert));
                }
            }
        }
        return glyphs;
    }

    public record Glyph(@Nullable BufferedImage image,
                        int x,
                        int y,
                        int width,
                        int sourceHeight,
                        int height,
                        int ascent,
                        float spacing,
                        boolean nativeText) { // 未覆写的原版字形，仅导出度量，保留字符交给基岩原生字体绘制

        public Glyph(BufferedImage image, int x, int y, int width, int sourceHeight, int height, int ascent, float spacing) {
            this(image, x, y, width, sourceHeight, height, ascent, spacing, false);
        }

        public boolean advanceOnly() {
            return this.image == null || this.height <= 0 || this.ascent < -2048 || this.actualWidth() == 0;
        }

        public int actualWidth() {
            for (int column = this.width - 1; column >= 0; column--) {
                for (int row = 0; row < this.sourceHeight; row++) {
                    if ((this.image.getRGB(this.x + column, this.y + row) >>> 24) != 0) {
                        return column + 1;
                    }
                }
            }
            return 0;
        }

        public float advance() {
            if (this.image == null) {
                return this.spacing;
            }
            return (int) (0.5 + this.actualWidth() * ((float) this.height / this.sourceHeight)) + 1;
        }
    }
}
