/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
