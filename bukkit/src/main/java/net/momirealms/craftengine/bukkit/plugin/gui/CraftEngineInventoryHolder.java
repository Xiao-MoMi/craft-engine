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
