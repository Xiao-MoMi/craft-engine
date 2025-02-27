package net.momirealms.craftengine.core.plugin.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PagedGuiImpl extends AbstractGui implements PagedGui {
    private final List<ItemWithAction> itemsWithFunction;
    private final int maxPages;
    private final int elementsPerPage;
    private int currentPage;

    public PagedGuiImpl(GuiLayout layout, Consumer<Click> inventoryClickConsumer, List<ItemWithAction> itemsWithFunction) {
        super(layout, inventoryClickConsumer);
        this.itemsWithFunction = itemsWithFunction;
        int i = 0;
        for (GuiElement element : this.guiElements) {
            if (element instanceof GuiElement.PageOrderedGuiElement) {
                i++;
            }
        }
        this.elementsPerPage = i;
        this.maxPages = Math.max(1, (itemsWithFunction.size() - 1) / i + 1);
        this.currentPage = 1;
    }

    @Override
    public List<ItemWithAction> items() {
        return this.itemsWithFunction;
    }

    @Override
    public ItemWithAction itemAt(int index) {
        int rawIndex = (currentPage - 1) * elementsPerPage + index;
        if (rawIndex >= itemsWithFunction.size()) return ItemWithAction.EMPTY;
        return this.itemsWithFunction.get(rawIndex);
    }

    @Override
    public void setPage(int page) {
        this.currentPage = page;
    }

    @Override
    public int currentPage() {
        return this.currentPage;
    }

    @Override
    public int maxPages() {
        return this.maxPages;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private GuiLayout layout;
        private Consumer<Click> inventoryClickConsumer = Click::cancel;
        private final List<ItemWithAction> items = new ArrayList<>();

        public Builder() {
        }

        public Builder layout(GuiLayout layout) {
            this.layout = layout;
            return this;
        }

        public Builder inventoryClickConsumer(Consumer<Click> inventoryClickConsumer) {
            this.inventoryClickConsumer = inventoryClickConsumer;
            return this;
        }

        public Builder addIngredients(List<ItemWithAction> items) {
            this.items.addAll(items);
            return this;
        }

        public PagedGuiImpl build() {
            return new PagedGuiImpl(layout, inventoryClickConsumer, items);
        }
    }
}
