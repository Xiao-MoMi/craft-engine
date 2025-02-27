package net.momirealms.craftengine.core.plugin.gui;

import java.util.function.Consumer;

public class BasicGuiImpl extends AbstractGui implements BasicGui {

    public BasicGuiImpl(GuiLayout layout, Consumer<Click> inventoryClickConsumer) {
        super(layout, inventoryClickConsumer);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private GuiLayout layout;
        private Consumer<Click> inventoryClickConsumer = Click::cancel;

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

        public BasicGui build() {
            return new BasicGuiImpl(layout, inventoryClickConsumer);
        }
    }
}
