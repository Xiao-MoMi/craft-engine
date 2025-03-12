package net.momirealms.craftengine.core.plugin.gui;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public abstract class AbstractGui implements Gui {
    protected Component title;
    protected final int width;
    protected final int height;
    protected final GuiElement[] guiElements;
    protected final Consumer<Click> inventoryClickConsumer;
    protected final Inventory inventory;

    public AbstractGui(GuiLayout layout, Consumer<Click> inventoryClickConsumer) {
        this.width = layout.width();
        this.height = layout.height();
        this.guiElements = layout.createElements(this);
        this.inventoryClickConsumer = inventoryClickConsumer;
        this.title = Component.empty();
        this.inventory = CraftEngine.instance().guiManager().createInventory(this, size());
    }

    @Override
    public Gui refresh() {
        int i = 0;
        for (GuiElement guiElement : this.guiElements) {
            this.inventory.setItem(i++, guiElement.item());
        }
        return this;
    }

    public void handleGuiClick(Click click) {
        GuiElement element = this.guiElements[click.slot()];
        if (element != null) {
            element.handleClick(click);
        } else {
            click.cancel();
        }
    }

    public void handleInventoryClick(Click click) {
        this.inventoryClickConsumer.accept(click);
    }

    @Override
    public Inventory inventory() {
        return inventory;
    }

    @Override
    public Component title() {
        return title;
    }

    @Override
    public AbstractGui title(Component title) {
        this.title = title;
        return this;
    }

    @Override
    public void open(Player player) {
        this.inventory.open(player, this.title);
    }

    @Override
    public void onTimer() {
        for (GuiElement guiElement : this.guiElements) {
            guiElement.onTimer();
        }
    }

    @Override
    public int height() {
        return this.height;
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public void setElement(int index, @Nullable GuiElement element) {
        this.guiElements[index] = element;
    }

    @Override
    public boolean hasElement(int index) {
        return this.guiElements[index] != null;
    }

    @Override
    public void removeElement(int index) {
        this.guiElements[index] = null;
    }
}
