package net.momirealms.craftengine.bukkit.item.listener;

import com.destroystokyo.paper.event.player.PlayerReadyArrowEvent;
import io.papermc.paper.event.block.CompostItemEvent;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.Set;

public final class PaperItemEventListener implements Listener {

    public PaperItemEventListener() {
    }

    @EventHandler(ignoreCancelled = true)
    public void onReadyArrow(PlayerReadyArrowEvent event) {
        BukkitItem bowItem = BukkitAdaptor.adapt(event.getBow());
        Optional<ItemDefinition> bowItemDefinition = bowItem.getDefinition();
        if (bowItemDefinition.isPresent()) {
            ItemDefinition itemDefinition = bowItemDefinition.get();
            Set<Key> ammo = itemDefinition.settings().allowedProjectiles();
            if (!ammo.isEmpty() && !ammo.contains(BukkitAdaptor.adapt(event.getArrow()).id())) {
                event.setCancelled(true);
            }
        }
    }

    // 自定义堆肥改了
    @EventHandler(ignoreCancelled = true)
    public void onCompost(CompostItemEvent event) {
        ItemStack itemToCompost = event.getItem();
        Item wrapped = BukkitAdaptor.adapt(itemToCompost);
        Optional<ItemDefinition> optionalCustomItem = wrapped.getDefinition();
        if (optionalCustomItem.isEmpty()) return;
        event.setWillRaiseLevel(RandomUtils.generateRandomFloat(0, 1) < optionalCustomItem.get().settings().compostProbability());
    }
}
