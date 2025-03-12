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

import java.util.List;

public interface PagedGui extends Gui {

    List<ItemWithAction> items();

    ItemWithAction itemAt(int index);

    void setPage(int page);

    int currentPage();

    int maxPages();

    default boolean hasNextPage() {
        return currentPage() < maxPages();
    }

    default boolean hasPreviousPage() {
        return currentPage() > 1;
    }

    default void goNextPage() {
        if (hasNextPage()) {
            setPage(currentPage() + 1);
        } else {
            setPage(1);
        }
    }

    default void goPreviousPage() {
        if (hasPreviousPage()) {
            setPage(currentPage() - 1);
        } else {
            setPage(maxPages());
        }
    }

    static PagedGuiImpl.Builder builder() {
        return new PagedGuiImpl.Builder();
    }
}
