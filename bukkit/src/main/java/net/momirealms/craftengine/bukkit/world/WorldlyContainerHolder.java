package net.momirealms.craftengine.bukkit.world;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class WorldlyContainerHolder implements InventoryHolder {
    private final Consumer<Player> onClose;
    private final Supplier<WorldPosition> pos;
    private Inventory inventory;

    public WorldlyContainerHolder(Consumer<Player> onClose, Supplier<WorldPosition> pos) {
        this.onClose = onClose;
        this.pos = pos;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    public void onClose(Player player) {
        this.onClose.accept(player);
    }

    public WorldPosition pos() {
        return this.pos.get();
    }
}
