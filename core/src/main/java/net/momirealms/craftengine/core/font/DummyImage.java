package net.momirealms.craftengine.core.font;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.Key;

public final class DummyImage implements Image {
    public static final Key INVALID_ID = Key.ce("__invalid__");
    public static final DummyImage INSTANCE = new DummyImage();
    private DummyImage() {}

    @Override
    public String miniMessageAt(int row, int col) {
        return "INVALID";
    }

    @Override
    public String mineDownAt(int row, int col) {
        return "INVALID";
    }

    @Override
    public int codepointAt(int row, int column) {
        return 0;
    }

    @Override
    public Component componentAt(int row, int column) {
        return Component.text("INVALID");
    }

    @Override
    public Key id() {
        return INVALID_ID;
    }
}
