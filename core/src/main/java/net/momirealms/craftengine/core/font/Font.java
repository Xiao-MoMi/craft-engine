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
