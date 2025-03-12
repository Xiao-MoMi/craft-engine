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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.momirealms.craftengine.core.util.CharacterUtils;

import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

public class OffsetFont {
    private final String font;

    private final String NEG_16;
    private final String NEG_24;
    private final String NEG_32;
    private final String NEG_48;
    private final String NEG_64;
    private final String NEG_128;
    private final String NEG_256;

    private final String POS_16;
    private final String POS_24;
    private final String POS_32;
    private final String POS_48;
    private final String POS_64;
    private final String POS_128;
    private final String POS_256;

    private final String[] negativeOffsets = new String[16];
    private final String[] positiveOffsets = new String[16];

    private final Cache<Integer, String> fastLookup = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(256)
            .build();

    public OffsetFont(Section section) {
        font = section.getString("font", "minecraft:default");

        NEG_16 = convertIfUnicode(section.getString("-16", ""));
        NEG_24 = convertIfUnicode(section.getString("-24", ""));
        NEG_32 = convertIfUnicode(section.getString("-32", ""));
        NEG_48 = convertIfUnicode(section.getString("-48", ""));
        NEG_64 = convertIfUnicode(section.getString("-64", ""));
        NEG_128 = convertIfUnicode(section.getString("-128", ""));
        NEG_256 = convertIfUnicode(section.getString("-256", ""));
        POS_16 = convertIfUnicode(section.getString("16", ""));
        POS_24 = convertIfUnicode(section.getString("24", ""));
        POS_32 = convertIfUnicode(section.getString("32", ""));
        POS_48 = convertIfUnicode(section.getString("48", ""));
        POS_64 = convertIfUnicode(section.getString("64", ""));
        POS_128 = convertIfUnicode(section.getString("128", ""));
        POS_256 = convertIfUnicode(section.getString("256", ""));

        for (int i = 1; i <= 15; i++) {
            negativeOffsets[i] = convertIfUnicode(section.getString("-" + i, ""));
            positiveOffsets[i] = convertIfUnicode(section.getString(String.valueOf(i), ""));
        }
    }

    public String createOffset(int offset, BiFunction<String, String, String> tagDecorator) {
        if (offset == 0) return "";
        String raw = fastLookup.get(offset, k -> {
            if (k > 0) {
                return createPos(k);
            } else {
                return createNeg(-k);
            }
        });
        return tagDecorator.apply(raw, font);
    }

    @SuppressWarnings("DuplicatedCode")
    private String createPos(int offset) {
        StringBuilder stringBuilder = new StringBuilder();
        while (offset >= 256) {
            stringBuilder.append(POS_256);
            offset -= 256;
        }
        if (offset >= 128) {
            stringBuilder.append(POS_128);
            offset -= 128;
        }
        if (offset >= 64) {
            stringBuilder.append(POS_64);
            offset -= 64;
        }
        if (offset >= 48) {
            stringBuilder.append(POS_48);
            offset -= 48;
        }
        if (offset >= 32) {
            stringBuilder.append(POS_32);
            offset -= 32;
        }
        if (offset >= 24) {
            stringBuilder.append(POS_24);
            offset -= 24;
        }
        if (offset >= 16) {
            stringBuilder.append(POS_16);
            offset -= 16;
        }
        if (offset == 0) return stringBuilder.toString();
        stringBuilder.append(positiveOffsets[offset]);
        return stringBuilder.toString();
    }

    @SuppressWarnings("DuplicatedCode")
    private String createNeg(int offset) {
        StringBuilder stringBuilder = new StringBuilder();
        while (offset >= 256) {
            stringBuilder.append(NEG_256);
            offset -= 256;
        }
        if (offset >= 128) {
            stringBuilder.append(NEG_128);
            offset -= 128;
        }
        if (offset >= 64) {
            stringBuilder.append(NEG_64);
            offset -= 64;
        }
        if (offset >= 48) {
            stringBuilder.append(NEG_48);
            offset -= 48;
        }
        if (offset >= 32) {
            stringBuilder.append(NEG_32);
            offset -= 32;
        }
        if (offset >= 24) {
            stringBuilder.append(NEG_24);
            offset -= 24;
        }
        if (offset >= 16) {
            stringBuilder.append(NEG_16);
            offset -= 16;
        }
        if (offset == 0) return stringBuilder.toString();
        stringBuilder.append(negativeOffsets[offset]);
        return stringBuilder.toString();
    }

    private String convertIfUnicode(String s) {
        if (s.startsWith("\\u")) {
            return new String(CharacterUtils.decodeUnicodeToChars(font));
        } else {
            return s;
        }
    }
}
