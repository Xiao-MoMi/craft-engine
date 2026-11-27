package net.momirealms.craftengine.core.pack.bedrock.font;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.CharacterUtils;
import net.momirealms.craftengine.core.util.CollectingConfigReader;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public record GuiGlyphSelector(int first, int last, boolean range) {
    private static final Pattern HEX = Pattern.compile("(?:[Uu]\\+|0[xX])[0-9a-fA-F]{1,6}");
    private static final Pattern ESCAPES = Pattern.compile("(?:\\\\u[0-9a-fA-F]{4}){1,2}");

    private GuiGlyphSelector(int code) {
        this(code, code, false);
    }

    public static Map<Key, List<GuiGlyphSelector>> read(CollectingConfigReader config, String path) {
        Map<Key, List<GuiGlyphSelector>> fonts = new HashMap<>();
        for (Map.Entry<String, Object> entry : config.getSection(path).entrySet()) {
            String source = path + "." + entry.getKey();
            Key font = config.read(ConfigValue.of(source, entry.getKey()), CollectingConfigReader::identifier, null);
            if (font == null) {
                continue;
            }
            List<GuiGlyphSelector> selectors = new ArrayList<>();
            for (ConfigValue value : ConfigValue.of(source, entry.getValue()).getAsValueList()) {
                GuiGlyphSelector selector = config.read(value, GuiGlyphSelector::parse, null);
                if (selector != null) {
                    selectors.add(selector);
                }
            }
            if (!selectors.isEmpty()) {
                fonts.put(font, List.copyOf(selectors));
            }
        }
        return Map.copyOf(fonts);
    }

    static GuiGlyphSelector parse(ConfigValue value) {
        if (value.value() instanceof Number number) {
            return new GuiGlyphSelector(scalar(Integer.parseInt(number.toString())));
        }
        String text = CollectingConfigReader.string(value);
        int separator = text.indexOf("..");
        if (separator < 0) {
            return new GuiGlyphSelector(codepoint(text));
        }
        int first = codepoint(text.substring(0, separator).strip());
        int last = codepoint(text.substring(separator + 2).strip());
        if (first > last) {
            throw new IllegalArgumentException("Reversed GUI glyph range " + text);
        }
        return new GuiGlyphSelector(first, last, true);
    }

    private static int codepoint(String text) {
        if (!text.isEmpty() && Character.charCount(text.codePointAt(0)) == text.length()) {
            return scalar(text.codePointAt(0));
        }
        if (HEX.matcher(text).matches()) {
            return scalar(Integer.parseInt(text, 2, text.length(), 16));
        }
        if (ESCAPES.matcher(text).matches()) {
            return scalar(CharacterUtils.charsToCodePoint(CharacterUtils.decodeUnicodeToChars(text)));
        }
        try {
            return scalar(Integer.parseInt(text));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid GUI glyph " + text + " (expected a code point, one character, or an inclusive range)");
        }
    }

    private static int scalar(int code) {
        if (!Character.isValidCodePoint(code) || (code >= Character.MIN_SURROGATE && code <= Character.MAX_SURROGATE)) {
            throw new IllegalArgumentException("Invalid Unicode scalar " + code);
        }
        return code;
    }

    static Set<Integer> resolve(JavaFontResolver fonts, Key font, List<GuiGlyphSelector> selectors) throws IOException {
        Set<Integer> selected = new TreeSet<>();
        BitSet ranges = new BitSet(0);
        for (GuiGlyphSelector selector : selectors) {
            if (selector.range) {
                ranges.set(selector.first, selector.last + 1);
            } else {
                selected.add(selector.first);
            }
        }
        if (!ranges.isEmpty()) {
            // 区间只与已解析的可见位图求交，重叠区间和未定义码点都不产生字形对象。
            ranges.clear(Character.MIN_SURROGATE, Character.MAX_SURROGATE + 1);
            for (Int2ObjectMap.Entry<Glyph> entry : Int2ObjectMaps.fastIterable(fonts.resolve(font))) {
                int code = entry.getIntKey();
                if (ranges.get(code) && entry.getValue().isGuiBitmap()) {
                    selected.add(code);
                }
            }
        }
        return selected;
    }
}
