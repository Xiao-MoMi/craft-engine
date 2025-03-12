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

import net.momirealms.craftengine.core.util.Key;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class Font {
    private final Key key;
    private final HashMap<Integer, BitmapImage> idToCodepoint = new LinkedHashMap<>();

    public Font(Key key) {
        this.key = key;
    }

    public boolean isCodepointInUse(int codepoint) {
        if (codepoint == 0) return false;
        return this.idToCodepoint.containsKey(codepoint);
    }

    public BitmapImage getImageByCodepoint(int codepoint) {
        return this.idToCodepoint.get(codepoint);
    }

    public void registerCodepoint(int codepoint, BitmapImage image) {
        this.idToCodepoint.put(codepoint, image);
    }

    public Key key() {
        return key;
    }

    public Collection<BitmapImage> bitmapImages() {
        return idToCodepoint.values().stream().distinct().toList();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Font font = (Font) object;
        return key.equals(font.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
