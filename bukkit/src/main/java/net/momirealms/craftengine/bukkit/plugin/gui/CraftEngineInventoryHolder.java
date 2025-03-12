package net.momirealms.craftengine.bukkit.plugin.gui;

import net.momirealms.craftengine.core.plugin.gui.Gui;
import net.momirealms.craftengine.shared.ObjectHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class CraftEngineInventoryHolder implements InventoryHolder {
    private final ObjectHolder<Inventory> inventory;
    private final Gui gui;

    public CraftEngineInventoryHolder(Gui gui) {
        this.inventory = new ObjectHolder<>();
        this.gui = gui;
    }

    public ObjectHolder<Inventory> holder() {
        return inventory;
    }

    public Gui gui() {
        return gui;
    }

    public Inventory inventory() {
        return inventory.value();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory();
    }
}
