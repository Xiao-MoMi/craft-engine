package net.momirealms.craftengine.core.font;

import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.plugin.Reloadable;
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;
import net.momirealms.craftengine.core.util.CharacterUtils;
import net.momirealms.craftengine.core.util.FormatUtils;
import net.momirealms.craftengine.core.util.Key;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;

public interface ImageManager extends Reloadable, ConfigSectionParser {
    String CONFIG_SECTION_NAME = "images";

    default String sectionId() {
        return CONFIG_SECTION_NAME;
    }

    Collection<Font> fontsInUse();

    Optional<BitmapImage> bitmapImageByCodepoint(Key font, int codepoint);

    default Optional<BitmapImage> getBitmapImageByChars(Key font, char[] chars) {
        return bitmapImageByCodepoint(font, CharacterUtils.charsToCodePoint(chars));
    }

    Optional<BitmapImage> bitmapImageByImageId(Key imageId);

    Optional<Font> getFontInUse(Key font);

    int codepointByImageId(Key imageId, int x, int y);

    default int codepointByImageId(Key imageId) {
        return this.codepointByImageId(imageId, 0, 0);
    }

    default char[] getCharsByImageId(Key imageId) {
        return getCharsByImageId(imageId, 0, 0);
    }

    default char[] getCharsByImageId(Key imageId, int x, int y) {
        return Character.toChars(this.codepointByImageId(imageId, x, y));
    }

    String createOffsets(int offset, BiFunction<String, String, String> tagFormatter);

    default String createMiniMessageOffsets(int offset) {
        return createOffsets(offset, FormatUtils::miniMessageFont);
    }

    default String createMineDownOffsets(int offset) {
        return createOffsets(offset, FormatUtils::mineDownFont);
    }

    default String createRawOffsets(int offset) {
        return createOffsets(offset, (raw, font) -> raw);
    }

    default int loadingSequence() {
        return LoadingSequence.FONT;
    }
}
