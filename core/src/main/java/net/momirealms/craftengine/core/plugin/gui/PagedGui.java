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
