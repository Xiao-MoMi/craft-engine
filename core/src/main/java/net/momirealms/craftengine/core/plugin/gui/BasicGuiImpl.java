package net.momirealms.craftengine.core.plugin.gui;

import java.util.function.Consumer;

public class BasicGuiImpl extends AbstractGui implements BasicGui {

    public BasicGuiImpl(GuiLayout layout, Consumer<Click> inventoryClickConsumer) {
        super(layout, inventoryClickConsumer);
    }
}
