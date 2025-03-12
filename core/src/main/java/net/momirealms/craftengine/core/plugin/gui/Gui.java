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
