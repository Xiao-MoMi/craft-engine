package net.momirealms.craftengine.core.font;

import net.momirealms.craftengine.core.util.Key;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Font {
    private final Key key;
    private final Map<Integer, BitmapImage> idToCodepoint = new LinkedHashMap<>();

    public Font(Key key) {
        this.key = key;
    }

    public boolean isCodepointInUse(int codepoint) {
        if (codepoint == 0) return false;
        return this.idToCodepoint.containsKey(codepoint);
    }

    public Collection<Integer> codepointsInUse() {
        return Collections.unmodifiableCollection(this.idToCodepoint.keySet());
    }

    public BitmapImage bitmapImageByCodepoint(int codepoint) {
        return this.idToCodepoint.get(codepoint);
    }

    public synchronized void addBitmapImage(int codepoint, BitmapImage image) {
        this.idToCodepoint.put(codepoint, image);
    }

    public Key key() {
        return key;
    }

    public Collection<BitmapImage> bitmapImages() {
        return this.idToCodepoint.values().stream().distinct().toList();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Font font = (Font) object;
        return this.key.equals(font.key);
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }
}
