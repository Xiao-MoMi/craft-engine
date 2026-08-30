package net.momirealms.craftengine.bukkit.entity.listener;

import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import net.momirealms.craftengine.bukkit.entity.BukkitEntityManager;
import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.bukkit.plugin.listener.AbstractListener;
import net.momirealms.craftengine.bukkit.util.EquipmentSlotUtils;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.attribute.equipment.EquipmentSetSlot;
import net.momirealms.craftengine.core.entity.LivingEntityHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;

/** Loaded only on Paper-compatible 1.21.4+ servers. */
public final class PaperEquipmentListener extends AbstractListener {
    private final BukkitEntityManager manager;

    public PaperEquipmentListener(BukkitEntityManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onEquipmentChange(EntityEquipmentChangedEvent event) {
        LivingEntityHolder holder = this.manager.getEntityHolder(event.getEntity().getUniqueId());
        if (holder == null) return;
        Map<EquipmentSlot, EntityEquipmentChangedEvent.EquipmentChange> changes = event.getEquipmentChanges();
        Map<EquipmentSetSlot, BukkitItem> replacements = new HashMap<>(changes.size());
        for (Map.Entry<EquipmentSlot, EntityEquipmentChangedEvent.EquipmentChange> entry : changes.entrySet()) {
            replacements.put(
                    EquipmentSlotUtils.toEquipmentSetSlot(entry.getKey()),
                    ItemStackUtils.wrap(entry.getValue().newItem())
            );
        }
        boolean setPiecesChanged = holder.applyEquipmentChangesAndReportSetChange(replacements);
        if (setPiecesChanged && event.getEntity() instanceof Player player) {
            EquipmentSetClientSlotUpdater.updateAfterEquipmentChange(player);
        }
    }
}
