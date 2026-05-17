package net.momirealms.craftengine.bukkit.world;

import net.momirealms.craftengine.core.world.Container;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;

import javax.annotation.Nullable;
import java.util.List;

public interface BukkitContainer extends Container {

    void onOpen(HumanEntity player);

    void onClose(HumanEntity player);

    List<HumanEntity> getViewers();

    @Nullable
    InventoryHolder getOwner();
}
