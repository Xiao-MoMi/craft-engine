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

package net.momirealms.craftengine.core.util;

import java.util.stream.IntStream;

public class CharacterUtils {

    private CharacterUtils() {}

    public static char[] decodeUnicodeToChars(String unicodeString) {
        String processedString = unicodeString.replace("\\u", "");
        int length = processedString.length() / 4;
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            int codePoint = Integer.parseInt(processedString.substring(i * 4, i * 4 + 4), 16);
            if (Character.isSupplementaryCodePoint(codePoint)) {
                chars[i] = Character.highSurrogate(codePoint);
                chars[++i] = Character.lowSurrogate(codePoint);
            } else {
                chars[i] = (char) codePoint;
            }
        }
        return chars;
    }

    public static int charsToCodePoint(char[] chars) {
        if (chars.length == 1) {
            return chars[0];
        } else if (chars.length == 2) {
            if (Character.isHighSurrogate(chars[0]) && Character.isLowSurrogate(chars[1])) {
                return Character.toCodePoint(chars[0], chars[1]);
            } else {
                throw new IllegalArgumentException("Invalid surrogate pair: not a valid high and low surrogate combination.");
            }
        } else {
            throw new IllegalArgumentException("The given chars array must contain either 1 or 2 characters.");
        }
    }

    public static int[] charsToCodePoints(char[] chars) {
        return IntStream.range(0, chars.length)
                .filter(i -> !Character.isLowSurrogate(chars[i]))
                .map(i -> {
                    char c1 = chars[i];
                    if (Character.isHighSurrogate(c1)) {
                        if (i + 1 < chars.length && Character.isLowSurrogate(chars[i + 1])) {
                            char c2 = chars[++i];
                            return Character.toCodePoint(c1, c2);
                        } else {
                            throw new IllegalArgumentException("Illegal surrogate pair: High surrogate without matching low surrogate at index " + i);
                        }
                    } else {
                        return c1;
                    }
                }).toArray();
    }

    public static String encodeCharToUnicode(char c) {
        return String.format("\\u%04x", (int) c);
    }

    public static String encodeCharsToUnicode(char[] chars) {
        StringBuilder builder = new StringBuilder();
        for (char value : chars) {
            builder.append(encodeCharToUnicode(value));
        }
        return builder.toString();
    }
}
