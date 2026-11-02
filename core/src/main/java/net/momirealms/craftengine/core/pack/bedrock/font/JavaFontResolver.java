package net.momirealms.craftengine.core.pack.bedrock.font;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.ClientPngReader;
import net.momirealms.craftengine.core.util.Key;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public final class JavaFontResolver {
    private final JavaResourcePack resources;
    private final Map<String, BufferedImage> images = new HashMap<>();
    private final Map<Key, Int2ObjectMap<Glyph>> resolved = new HashMap<>();
    private final Set<Key> resolving = new HashSet<>();
    private int circularReferences;
    private final Map<Key, List<JavaResourcePack.FontResource>> fonts;

    public JavaFontResolver(JavaResourcePack resources) throws IOException {
        this.resources = resources;
        this.fonts = resources.fonts();
    }

    public Set<Key> fonts() {
        return this.fonts.keySet();
    }

    public Int2ObjectMap<Glyph> resolve(Key font) throws IOException {
        Int2ObjectMap<Glyph> cached = this.resolved.get(font);
        if (cached != null) {
            return cached;
        }
        List<JavaResourcePack.FontResource> definitions = this.fonts.get(font);
        if (definitions == null) {
            return Int2ObjectMaps.emptyMap();
        }
        this.resolving.add(font);
        int cyclesBefore = this.circularReferences;
        try {
            Int2ObjectMap<Glyph> glyphs = new Int2ObjectOpenHashMap<>();
            // 高优先级包先提供字形，缺少的字符继续向低优先级包查找
            for (JavaResourcePack.FontResource resource : definitions) {
                JsonArray providers = resource.definition().getAsJsonArray("providers");
                for (int index = 0; index < providers.size(); index++) {
                    Int2ObjectMap<Glyph> loaded;
                    try {
                        loaded = this.loadProvider(providers.get(index).getAsJsonObject(), resource.custom(), font, index);
                    } catch (IOException | RuntimeException exception) {
                        String source = resource.custom() ? "resource_pack" : "vanilla";
                        TranslationManager.warn(
                                "resource_pack.bedrock.font.provider",
                                font.asString(),
                                source,
                                Integer.toString(index),
                                Objects.toString(exception.getLocalizedMessage(), exception.getClass().getSimpleName())
                        );
                        continue;
                    }
                    Int2ObjectMaps.fastForEach(loaded, entry -> glyphs.putIfAbsent(entry.getIntKey(), entry.getValue()));
                }
            }
            // 跳过循环引用后，结果依赖本次引用路径，不能复用到其他字体。
            if (this.circularReferences == cyclesBefore) {
                this.resolved.put(font, glyphs);
            }
            return glyphs;
        } finally {
            this.resolving.remove(font);
        }
    }

    private Int2ObjectMap<Glyph> loadProvider(JsonObject provider, boolean custom, Key font, int index) throws IOException {
        if (provider.has("filter") && provider.getAsJsonObject("filter").entrySet().stream().anyMatch(entry -> entry.getValue().getAsBoolean())) {
            return Int2ObjectMaps.emptyMap();
        }
        Key type = Key.of(provider.get("type").getAsString());
        if (!type.isMinecraftNamespace()) {
            return Int2ObjectMaps.emptyMap();
        }
        return switch (type.value) {
            case "bitmap" -> this.bitmap(provider, custom);
            case "reference" -> {
                Key target = Key.of(provider.get("id").getAsString());
                if (!this.fonts.containsKey(target)) {
                    throw new IOException(TranslationManager.message("resource_pack.bedrock.font.missing_ref", target.asString()));
                }
                if (this.resolving.contains(target)) {
                    this.circularReferences++;
                    throw new IOException(TranslationManager.message("resource_pack.bedrock.font.cycle", target.asString()));
                }
                yield this.resolve(target);
            }
            case "space" -> {
                Int2ObjectMap<Glyph> spaces = new Int2ObjectOpenHashMap<>();
                for (Map.Entry<String, JsonElement> entry : provider.getAsJsonObject("advances").entrySet()) {
                    float advance = entry.getValue().getAsFloat();
                    if (entry.getKey().isEmpty() || !Float.isFinite(advance)) {
                        throw new IOException(TranslationManager.message("resource_pack.bedrock.font.space", entry.getKey()));
                    }
                    spaces.putIfAbsent(entry.getKey().codePointAt(0), Glyph.space(advance));
                }
                yield spaces;
            }
            // TODO TTF/UNIHEX
            default -> {
                if (custom) {
                    TranslationManager.warn("resource_pack.bedrock.font.skipped_provider", font.asString(), Integer.toString(index), type.asString());
                }
                yield Int2ObjectMaps.emptyMap();
            }
        };
    }

    private Int2ObjectMap<Glyph> bitmap(JsonObject provider, boolean custom) throws IOException {
        int height = provider.has("height") ? provider.get("height").getAsInt() : 8;
        int ascent = provider.get("ascent").getAsInt();
        if (ascent > height) {
            throw new IOException(TranslationManager.message("resource.image.height_ascent_conflict", Integer.toString(height), Integer.toString(ascent)));
        }
        Key texture = Key.of(provider.get("file").getAsString());
        String path = "assets/" + texture.namespace() + "/textures/" + texture.value();
        JsonArray rows = provider.getAsJsonArray("chars");
        int[][] characters = new int[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            characters[i] = rows.get(i).getAsString().codePoints().toArray();
        }
        if (characters.length == 0 || characters[0].length == 0) {
            throw new IOException(TranslationManager.message("resource_pack.bedrock.font.grid", path));
        }
        Int2ObjectMap<Glyph> glyphs = new Int2ObjectOpenHashMap<>();
        boolean convert = custom || this.resources.hasCustomResource(path);
        BufferedImage image = this.images.get(path);
        if (image == null) {
            try (InputStream input = this.resources.openResource(path)) {
                if (input != null) {
                    image = ClientPngReader.read(input);
                }
            } catch (IOException exception) {
                throw new IOException(path + ": " + exception.getMessage(), exception);
            }
            if (image == null) {
                throw new IOException(TranslationManager.message("resource_pack.bedrock.font.bitmap", path));
            }
            this.images.put(path, image);
        }
        int width = image.getWidth() / characters[0].length;
        int cellHeight = image.getHeight() / characters.length;
        if (width == 0 || cellHeight == 0) {
            throw new IOException(TranslationManager.message("resource_pack.bedrock.font.cell", path));
        }
        for (int y = 0; y < characters.length; y++) {
            if (characters[y].length != characters[0].length) {
                throw new IOException(TranslationManager.message("resource_pack.bedrock.font.rows", path));
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
}
