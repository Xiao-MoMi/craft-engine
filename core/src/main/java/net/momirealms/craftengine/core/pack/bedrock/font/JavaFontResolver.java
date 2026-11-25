package net.momirealms.craftengine.core.pack.bedrock.font;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.momirealms.craftengine.bedrock.GuiTitlePart;
import net.momirealms.craftengine.bedrock.text.FontData;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public final class JavaFontResolver {
    public record FontOptions(boolean uniform, boolean japaneseVariants) {
        public static final FontOptions DEFAULT = new FontOptions(false, false);
    }

    private record Lookup(Key font, int inheritedFilters) {
    }

    private static final int UNIFORM = 1;
    private static final int JAPANESE_VARIANTS = 2;
    private static final Key DEFAULT_FONT = Key.of("minecraft:default");
    private static final Key UNIFORM_FONT = Key.of("minecraft:uniform");
    private static final Glyph MISSING = Glyph.space(FontData.MISSING_ADVANCE);
    private final FontOptions options;
    private final JavaFontSources sources;
    private final List<Pattern> excludedFonts;
    private final Map<Lookup, Int2ObjectMap<Glyph>> resolved = new HashMap<>();
    private final Set<Key> resolving = new HashSet<>();
    private int circularReferences;

    public JavaFontResolver(JavaResourcePack resources, List<Pattern> excludedFonts) throws IOException {
        this(resources, excludedFonts, FontOptions.DEFAULT);
    }

    public JavaFontResolver(JavaResourcePack resources, List<Pattern> excludedFonts, FontOptions options) throws IOException {
        this(new JavaFontSources(resources), excludedFonts, options);
    }

    private JavaFontResolver(JavaFontSources sources, List<Pattern> excludedFonts, FontOptions options) {
        this.sources = sources;
        this.options = options;
        this.excludedFonts = excludedFonts;
    }

    JavaFontResolver excluding(List<Pattern> excludedFonts) {
        return excludedFonts.isEmpty() ? this : new JavaFontResolver(this.sources, excludedFonts, this.options);
    }

    public float advance(Key font, int codepoint, boolean bold) throws IOException {
        return this.resolve(font).getOrDefault(codepoint, MISSING).advance(bold);
    }

    public int width(List<GuiTitlePart> parts) throws IOException {
        float width = 0;
        for (GuiTitlePart part : parts) {
            Int2ObjectMap<Glyph> glyphs = this.resolve(Key.of(part.font()));
            boolean initialBold = (part.decorations() & 2) != 0;
            boolean bold = initialBold;
            String text = part.text();
            for (int i = 0; i < text.length();) {
                int code = text.codePointAt(i);
                i += Character.charCount(code);
                if (code == '§') {
                    if (i < text.length()) {
                        char format = Character.toLowerCase(text.charAt(i++));
                        if (format == 'l') {
                            bold = true;
                        } else if (format == 'r') {
                            bold = initialBold;
                        } else if ("0123456789abcdef".indexOf(format) >= 0) {
                            bold = false;
                        }
                    }
                    continue;
                }
                if (code >= Character.MIN_SURROGATE && code <= Character.MAX_SURROGATE) {
                    code = 0xfffd;
                }
                width += glyphs.getOrDefault(code, MISSING).advance(bold);
            }
        }
        return (int) Math.ceil(width);
    }

    public Set<Key> fonts() {
        return this.sources.fonts.keySet();
    }

    private boolean isExcluded(Key font) {
        String id = font.asString();
        for (Pattern pattern : this.excludedFonts) {
            if (pattern.matcher(id).matches()) {
                return true;
            }
        }
        return false;
    }

    public Int2ObjectMap<Glyph> resolve(Key font) throws IOException {
        if (this.options.uniform() && !this.sources.pack.version().isAtOrAbove(MinecraftVersion.V1_20_5) && DEFAULT_FONT.equals(font)) {
            font = UNIFORM_FONT;
        }
        return this.resolveDefinition(font, 0);
    }

    private Int2ObjectMap<Glyph> resolveDefinition(Key font, int inheritedFilters) throws IOException {
        if (this.isExcluded(font)) {
            return Int2ObjectMaps.emptyMap();
        }
        Lookup lookup = new Lookup(font, inheritedFilters);
        Int2ObjectMap<Glyph> cached = this.resolved.get(lookup);
        if (cached != null) {
            return cached;
        }
        List<JavaResourcePack.FontResource> definitions = this.sources.fonts.get(font);
        if (definitions == null) {
            return Int2ObjectMaps.emptyMap();
        }
        this.resolving.add(font);
        int cyclesBefore = this.circularReferences;
        try {
            Int2ObjectOpenHashMap<Glyph> glyphs = new Int2ObjectOpenHashMap<>();
            // 高优先级包先提供字形，缺少的字符继续向低优先级包查找
            for (JavaResourcePack.FontResource resource : definitions) {
                JsonArray providers = resource.definition().getAsJsonArray("providers");
                for (int index = 0; index < providers.size(); index++) {
                    try {
                        this.mergeProvider(glyphs, providers.get(index).getAsJsonObject(), resource.custom(), font, index, inheritedFilters);
                    } catch (IOException | RuntimeException exception) {
                        String source = resource.custom() ? "resource_pack" : "vanilla";
                        TranslationManager.warn(
                                "resource_pack.bedrock.font.provider",
                                font.asString(),
                                source,
                                Integer.toString(index),
                                Objects.toString(exception.getLocalizedMessage(), exception.getClass().getSimpleName())
                        );
                    }
                }
            }
            // 跳过循环引用后，结果依赖本次引用路径，不能复用到其他字体。
            if (this.circularReferences == cyclesBefore) {
                this.resolved.put(lookup, glyphs);
            }
            return glyphs;
        } finally {
            this.resolving.remove(font);
        }
    }

    private void mergeProvider(Int2ObjectOpenHashMap<Glyph> destination, JsonObject provider, boolean custom, Key font, int index, int inheritedFilters) throws IOException {
        JsonObject filter = provider.getAsJsonObject("filter");
        if (filter != null && this.sources.pack.version().isAtOrAbove(MinecraftVersion.V1_20_5)) {
            // 引用者的同名条件优先；已在外层匹配的条件不再用子字体的值重复筛选。
            for (Map.Entry<String, JsonElement> entry : filter.entrySet()) {
                int option = switch (entry.getKey()) {
                    case "uniform" -> UNIFORM;
                    case "jp" -> JAPANESE_VARIANTS;
                    default -> throw new IOException("Unknown font option " + entry.getKey());
                };
                boolean enabled = option == UNIFORM ? this.options.uniform() : this.options.japaneseVariants();
                if ((inheritedFilters & option) == 0 && entry.getValue().getAsBoolean() != enabled) {
                    return;
                }
                inheritedFilters |= option;
            }
        }
        Key type = Key.of(provider.get("type").getAsString());
        if (!type.isMinecraftNamespace()) {
            return;
        }
        switch (type.value) {
            case "bitmap" -> mergeGlyphs(destination, this.bitmap(provider, custom));
            case "reference" -> {
                Key target = Key.of(provider.get("id").getAsString());
                if (this.isExcluded(target)) {
                    return;
                }
                if (!this.sources.fonts.containsKey(target)) {
                    throw new IOException(TranslationManager.message("resource_pack.bedrock.font.missing_ref", target.asString()));
                }
                if (this.resolving.contains(target)) {
                    this.circularReferences++;
                    throw new IOException(TranslationManager.message("resource_pack.bedrock.font.cycle", target.asString()));
                }
                mergeGlyphs(destination, this.resolveDefinition(target, inheritedFilters));
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
                mergeGlyphs(destination, spaces);
            }
            case "unihex" -> {
                Key file = Key.of(provider.get("hex_file").getAsString());
                String path = "assets/" + file.namespace() + "/" + file.value();
                JavaUnihexFont source = this.sources.unihex(path);
                JsonArray sizes = provider.getAsJsonArray("size_overrides");
                if (sizes == null && !this.sources.pack.version().isAtOrAbove(MinecraftVersion.V1_21_6)) {
                    throw new IOException("Missing size_overrides in " + path);
                }
                source.mergeInto(destination, sizes);
            }
            default -> {
                if (custom) {
                    TranslationManager.warn("resource_pack.bedrock.font.skipped_provider", font.asString(), Integer.toString(index), type.asString());
                }
            }
        }
    }

    private static void mergeGlyphs(Int2ObjectOpenHashMap<Glyph> destination, Int2ObjectMap<Glyph> source) {
        destination.ensureCapacity(source.size());
        Int2ObjectMaps.fastForEach(source, entry -> destination.putIfAbsent(entry.getIntKey(), entry.getValue()));
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
        boolean convert = custom || this.sources.pack.hasCustomResource(path);
        BufferedImage image = this.sources.image(path);
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
