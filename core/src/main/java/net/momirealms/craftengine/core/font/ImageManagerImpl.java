package net.momirealms.craftengine.core.font;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.CharacterUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.PreConditions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;

public class ImageManagerImpl implements ImageManager {
    private final CraftEngine plugin;
                // namespace:font font
    private final HashMap<Key, Font> fonts = new HashMap<>();
                // namespace:id image
    private final HashMap<Key, BitmapImage> images = new HashMap<>();

    private OffsetFont offsetFont;

    public ImageManagerImpl(CraftEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void load() {
        this.offsetFont = Optional.ofNullable(plugin.configManager().settings().getSection("offset-characters"))
                .map(OffsetFont::new)
                .orElse(null);
    }

    @Override
    public void unload() {
        this.fonts.clear();
        this.images.clear();
    }

    @Override
    public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
        int height = MiscUtils.getAsInt(section.get("height"));
        int ascent = MiscUtils.getAsInt(section.get("ascent"));
        if (PreConditions.runIfTrue(height < ascent,
                () -> this.plugin.logger().warn(path, "Illegal ascent found at " + id + ". Height should be no lower than ascent"))) return;

        String file = (String) section.get("file");
        if (PreConditions.isNull(file,
                () -> this.plugin.logger().warn(path, "`file` option is not set in image " + id))) return;

        String fontName = (String) section.getOrDefault("font", "minecraft:default");
        if (PreConditions.isNull(fontName,
                () -> this.plugin.logger().warn(path, "`font` option is not set in image " + id))) return;

        Key fontKey = Key.withDefaultNamespace(fontName, id.namespace());
        // get the font
        Font font = this.getOrCreateFont(fontKey);
        List<char[]> chars;
        if (section.containsKey("chars")) {
            chars = MiscUtils.getAsStringList(section.get("chars")).stream().map(it -> {
                if (it.startsWith("\\u")) {
                    return CharacterUtils.decodeUnicodeToChars(it);
                } else {
                    return it.toCharArray();
                }
            }).toList();
        } else {
            String character = (String) section.get("char");
            if (PreConditions.isNull(character,
                    () -> this.plugin.logger().warn(path, "`char` option is not set in image " + id))) return;
            if (character.length() == 1) {
                chars = List.of(character.toCharArray());
            } else {
                chars = List.of(CharacterUtils.decodeUnicodeToChars(character));
            }
        }

        int size = -1;
        int[][] codepointGrid = new int[chars.size()][];
        for (int i = 0; i < chars.size(); ++i) {
            int[] codepoints = CharacterUtils.charsToCodePoints(chars.get(i));
            for (int codepoint : codepoints) {
                if (PreConditions.runIfTrue(font.isCodepointInUse(codepoint),
                        () -> this.plugin.logger().warn(path, String.format("Codepoint [%s] is already used in font [%s]", CharacterUtils.encodeCharsToUnicode(Character.toChars(codepoint)), font.key().toString())))) return;
            }
            codepointGrid[i] = codepoints;
            if (size == -1) size = codepoints.length;
            if (PreConditions.runIfTrue(size != codepoints.length,
                    () -> this.plugin.logger().warn(path, "Illegal chars format found at " + id))) return;
        }

        if (PreConditions.runIfTrue(size == -1,
                () -> this.plugin.logger().warn(path, "Illegal chars format found at " + id))) return;

        if (!file.endsWith(".png")) file += ".png";
        file = file.replace("\\", "/");
        Key namespacedPath = Key.of(file);
        Path targetImageFile = pack.resourcePackFolder()
                .resolve("assets")
                .resolve(namespacedPath.namespace())
                .resolve("textures")
                .resolve(namespacedPath.value());

        if (PreConditions.runIfTrue(!Files.exists(targetImageFile),
                () -> this.plugin.logger().warn(targetImageFile, "PNG file not found for image " + id))) return;

        BitmapImage bitmapImage = new BitmapImage(id, fontKey, height, ascent, file, codepointGrid);
        for (int[] y : codepointGrid) {
            for (int x : y) {
                font.registerCodepoint(x, bitmapImage);
            }
        }

        this.images.put(id, bitmapImage);
    }

    @Override
    public Collection<Font> fontsInUse() {
        return new ArrayList<>(this.fonts.values());
    }

    @Override
    public Optional<BitmapImage> bitmapImageByCodepoint(Key font, int codepoint) {
        return getFontInUse(font).map(f -> f.getImageByCodepoint(codepoint));
    }

    @Override
    public Optional<BitmapImage> bitmapImageByImageId(Key id) {
        return Optional.ofNullable(this.images.get(id));
    }

    @Override
    public int codepointByImageId(Key key, int x, int y) {
        BitmapImage image = this.images.get(key);
        if (image == null) return -1;
        return image.codepointAt(x, y);
    }

    @Override
    public String createOffsets(int offset, BiFunction<String, String, String> tagFormatter) {
        return Optional.ofNullable(this.offsetFont).map(it -> it.createOffset(offset, tagFormatter)).orElse("");
    }

    @Override
    public Optional<Font> getFontInUse(Key key) {
        return Optional.ofNullable(fonts.get(key));
    }

    private Font getOrCreateFont(Key key) {
        return this.fonts.computeIfAbsent(key, Font::new);
    }
}
