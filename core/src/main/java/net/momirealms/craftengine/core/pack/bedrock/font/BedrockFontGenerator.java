package net.momirealms.craftengine.core.pack.bedrock.font;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.font.JavaFontResolver.Glyph;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.Key;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class BedrockFontGenerator {
    private final JavaFontResolver fonts;
    private final JsonArray diagnostics = new JsonArray();

    public BedrockFontGenerator(JavaResourcePack resources) throws IOException {
        this(new JavaFontResolver(resources));
    }

    public BedrockFontGenerator(JavaFontResolver fonts) {
        this.fonts = fonts;
    }

    public void write(Path root) throws IOException {
        JsonObject mappings = new JsonObject();
        JsonObject layoutFonts = new JsonObject();
        Map<Glyph, Integer> allocated = new HashMap<>();
        Map<Glyph, Metrics> metrics = new HashMap<>();
        Map<Glyph, String> unsupported = new HashMap<>();
        List<Page> pages = new ArrayList<>();
        for (Key font : this.fonts.fonts()) {
            JsonObject characters = new JsonObject();
            JsonObject layout = new JsonObject();
            Int2ObjectMap<Glyph> glyphs = this.fonts.resolve(font);
            int[] codepoints = glyphs.keySet().toIntArray();
            IntArrays.quickSort(codepoints);
            Map<String, Integer> skipped = new LinkedHashMap<>();
            Set<Glyph> pending = new LinkedHashSet<>();
            for (int codepoint : codepoints) {
                Glyph glyph = glyphs.get(codepoint);
                // 无需新增图片字形空间，后续仍会导出有效字形的映射或度量
                if (glyph == null || glyph.nativeText() || glyph.advanceOnly() || allocated.containsKey(glyph)) {
                    continue;
                }
                if (!metrics.containsKey(glyph) && !unsupported.containsKey(glyph)) {
                    try {
                        metrics.put(glyph, this.measure(glyph));
                    } catch (UnsupportedOperationException exception) {
                        unsupported.put(glyph, exception.getMessage());
                    }
                }
                String reason = unsupported.get(glyph);
                if (reason != null) {
                    skipped.merge(reason, 1, Integer::sum);
                } else {
                    pending.add(glyph);
                }
            }
            skipped.forEach((reason, count) -> this.diagnostics.add(font + ": " + reason + " (" + count + " glyphs)"));
            // Reserve a whole font before writing anything, so a large text font cannot be partially remapped.
            if (!this.fits(pending, metrics, pages)) {
                this.diagnostics.add(font + ": font exceeds remaining E3..F8 page capacity; entire font skipped");
                continue;
            }
            for (int codepoint : codepoints) {
                Glyph glyph = glyphs.get(codepoint);
                if (glyph == null) {
                    continue;
                }
                String key = Integer.toHexString(codepoint);
                if (glyph.nativeText()) {
                    // 原版 unihex 仅有 Java 度量，不能作为基岩实际字宽。
                    int renderedAdvance = glyph.image() == null ? -1 : (int) glyph.advance();
                    layout.add(key, this.layoutGlyph(codepoint, glyph.advance(), renderedAdvance, 0));
                    continue;
                }
                if (glyph.advanceOnly()) {
                    if (codepoint == ' ' && glyph.advance() == 4) {
                        layout.add(key, this.layoutGlyph(' ', 4, 4, 0));
                    } else {
                        layout.add(key, this.layoutGlyph(-1, glyph.advance(), 0, 0));
                    }
                    continue;
                }
                if (unsupported.containsKey(glyph)) {
                    continue;
                }
                Integer target = allocated.get(glyph);
                if (target == null) {
                    target = this.allocate(glyph, metrics.get(glyph), pages);
                    allocated.put(glyph, target);
                }
                characters.addProperty(key, target);
                layout.add(key, this.layoutGlyph(target, glyph.advance(), metrics.get(glyph).advance, 1));
            }
            if (!characters.isEmpty()) {
                mappings.add(font.asString(), characters);
            }
            if (!layout.isEmpty()) {
                layoutFonts.add(font.asString(), layout);
            }
        }
        Path directory = root.resolve("font");
        Files.createDirectories(directory);
        for (Page page : pages) {
            Path file = directory.resolve("glyph_" + Integer.toHexString(page.index).toUpperCase(Locale.ROOT) + ".png");
            ImageUtils.writePng(page.image, file);
        }
        // E2 is reserved for invisible positive advances; negative advances are consumed by the title layout.
        int spacerSize = 32;
        BufferedImage spacers = new BufferedImage(spacerSize * 16, spacerSize * 16, BufferedImage.TYPE_INT_ARGB);
        for (int advance = 2; advance <= ContainerTitleLayout.MAX_SPACER; advance++) {
            int slot = advance - 2;
            int x = slot % 16 * spacerSize + (spacerSize - (advance - 1)) / 2;
            int y = slot / 16 * spacerSize + spacerSize / 2;
            for (int column = 0; column < advance - 1; column++) {
                spacers.setRGB(x + column, y, 0x01000000);
            }
        }
        ImageUtils.writePng(spacers, directory.resolve("glyph_E2.png"));
        JsonObject result = new JsonObject();
        result.addProperty(BedrockPackLayout.FORMAT_VERSION, 1);
        result.add("fonts", mappings);
        result.add("metrics", layoutFonts);
        this.diagnostics.addAll(this.fonts.diagnostics());
        result.add(BedrockPackLayout.DIAGNOSTICS, this.diagnostics);
        GsonHelper.writeJsonFile(result, root.resolve(BedrockPackLayout.GLYPHS));
    }

    private JsonObject layoutGlyph(int character, float advance, int renderedAdvance, int bearing) {
        JsonObject glyph = new JsonObject();
        glyph.addProperty("character", character);
        glyph.addProperty("advance", advance);
        if (renderedAdvance >= 0) {
            glyph.addProperty("rendered_advance", renderedAdvance);
        }
        glyph.addProperty("bearing", bearing);
        return glyph;
    }

    private Metrics measure(Glyph glyph) {
        if (glyph.height() <= 0 || glyph.ascent() > glyph.height()) {
            throw new UnsupportedOperationException("bitmap height/ascent is only supported as GUI spacing");
        }
        int actualWidth = glyph.actualWidth();
        int advance = (int) glyph.advance();
        if (actualWidth == 0 || advance <= 1) {
            throw new UnsupportedOperationException("empty or subpixel bitmap spacing is not supported");
        }
        int width = advance - 1;
        long required = Math.max(width, Math.max(2L * (glyph.ascent() - 3L), 2L * (glyph.height() + 3L - glyph.ascent())));
        if (required > 512) {
            throw new UnsupportedOperationException("bitmap exceeds a 512-pixel font cell after baseline compensation");
        }
        int size = 8;
        while (size < required) {
            size *= 2;
        }
        return new Metrics(width, advance, size);
    }

    private boolean fits(Set<Glyph> glyphs, Map<Glyph, Metrics> metrics, List<Page> pages) {
        Map<Integer, Integer> available = new HashMap<>();
        for (Page page : pages) {
            available.merge(page.size, 256 - page.count, Integer::sum);
        }
        int count = pages.size();
        for (Glyph glyph : glyphs) {
            int size = metrics.get(glyph).size;
            int remaining = available.getOrDefault(size, 0);
            if (remaining == 0) {
                if (++count > 22) {
                    return false;
                }
                remaining = 256;
            }
            available.put(size, remaining - 1);
        }
        return true;
    }

    private int allocate(Glyph glyph, Metrics metrics, List<Page> pages) {
        int width = metrics.width;
        int advance = metrics.advance;
        int size = metrics.size;
        Page page = null;
        for (Page candidate : pages) {
            if (candidate.size == size && candidate.count < 256) {
                page = candidate;
                break;
            }
        }
        if (page == null) {
            page = new Page(0xE3 + pages.size(), size);
            pages.add(page);
        }
        int slot = page.count++;
        int originX = slot % 16 * size + (size - width) / 2;
        // Measured Bedrock cell baseline: cell/2 + 3; Java's bitmap top is determined by ascent.
        int originY = slot / 16 * size + size / 2 + 3 - glyph.ascent();
        int markerY = -1;
        float scale = (float) glyph.height() / glyph.sourceHeight();
        for (int y = 0; y < glyph.height(); y++) {
            int sourceY = Math.min(glyph.sourceHeight() - 1, (int) ((y + 0.5) * glyph.sourceHeight() / glyph.height()));
            for (int x = 0; x < width; x++) {
                int sourceX = Math.min(glyph.width() - 1, (int) ((x + 0.5) / scale));
                int pixel = glyph.image().getRGB(glyph.x() + sourceX, glyph.y() + sourceY);
                if ((pixel >>> 24) != 0 && markerY < 0) {
                    markerY = y;
                }
                page.image.setRGB(originX + x, originY + y, pixel);
            }
        }
        if (markerY < 0) {
            markerY = 0;
        }
        // A minimal-alpha edge survives Bedrock's horizontal trim and retains Java's bearing/advance.
        for (int x : new int[]{0, advance - 2}) {
            if ((page.image.getRGB(originX + x, originY + markerY) >>> 24) == 0) {
                page.image.setRGB(originX + x, originY + markerY, 0x01000000);
            }
        }
        return page.index * 256 + slot;
    }

    private record Metrics(int width, int advance, int size) {
    }

    private static final class Page {
        private final int index;
        private final int size;
        private final BufferedImage image;
        private int count;

        private Page(int index, int size) {
            this.index = index;
            this.size = size;
            this.image = new BufferedImage(size * 16, size * 16, BufferedImage.TYPE_INT_ARGB);
        }
    }
}
