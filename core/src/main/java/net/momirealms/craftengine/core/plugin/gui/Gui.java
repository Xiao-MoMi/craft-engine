package net.momirealms.craftengine.core.plugin.gui;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public interface Gui {

    Gui refresh();

    default int size() {
        return width() * height();
    }

    int height();

    int width();

    default int coordinateToIndex(int x, int y) {
        return y * width() + x;
    }

    void setElement(int index, @Nullable GuiElement element);

    default void setElement(int x, int y, @Nullable GuiElement element) {
        setElement(coordinateToIndex(x, y), element);
    }

    boolean hasElement(int index);

    default boolean hasElement(int x, int y) {
        return hasElement(coordinateToIndex(x, y));
    }

    void removeElement(int index);

    default void removeElement(int x, int y) {
        removeElement(coordinateToIndex(x, y));
    }

    Inventory inventory();

    Component title();

    Gui title(Component title);

    void open(Player player);

    void onTimer();
}
