package net.momirealms.craftengine.core.font;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.Key;

public interface Image {

    String miniMessageAt(int row, int col);

    String mineDownAt(int row, int col);

    Key id();

    int codepointAt(int row, int column);

    Component componentAt(int row, int column);
}
