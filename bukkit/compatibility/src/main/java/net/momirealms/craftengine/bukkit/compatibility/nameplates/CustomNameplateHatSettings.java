package net.momirealms.craftengine.bukkit.compatibility.nameplates;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.CustomItemSettingType;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemSettings;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.customnameplates.api.CNPlayer;
import net.momirealms.customnameplates.api.CustomNameplates;
import net.momirealms.customnameplates.api.CustomNameplatesAPI;
import net.momirealms.customnameplates.api.feature.tag.TagRenderer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.Optional;

public final class CustomNameplateHatSettings implements Listener {
    public static final CustomItemSettingType<Double> HAT_HEIGHT = CustomItemSettingType.simple();

    public void register() {
        ItemSettings.Modifiers.registerFactory("hat-height", height -> {
            double heightD = ResourceConfigUtils.getAsDouble(height, "hat-height");
            return settings -> settings.addCustomData(HAT_HEIGHT, heightD);
        });
        Bukkit.getPluginManager().registerEvents(this, BukkitCraftEngine.instance().javaPlugin());
    }

    @SuppressWarnings("deprecation")
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerArmorChange(PlayerArmorChangeEvent event) {
        if (VersionHelper.isOrAbove1_21_4()) {
            if (event.getSlot() != EquipmentSlot.HEAD) {
                return;
            }
        } else if (event.getSlotType() != PlayerArmorChangeEvent.SlotType.HEAD) {
            return;
        }
        ItemStack newItem = event.getNewItem();
        updateHatHeight(event.getPlayer(), newItem);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // 稍微延迟一下，可以等待背包同步插件的处理
        if (VersionHelper.isFolia()) {
            player.getScheduler().runDelayed(BukkitCraftEngine.instance().javaPlugin(), t1 -> {
                if (player.isOnline()) {
                    updateHatHeight(player, player.getInventory().getItem(EquipmentSlot.HEAD));
                }
            }, null, 10);
        } else {
            CraftEngine.instance().scheduler().sync().runLater(() -> {
                if (player.isOnline()) {
                    updateHatHeight(player, player.getInventory().getItem(EquipmentSlot.HEAD));
                }
            }, 10);
        }
    }

    public void updateHatHeight(Player player, ItemStack newItem) {
        CNPlayer cnPlayer = CustomNameplatesAPI.getInstance().getPlayer(player.getUniqueId());
        if (cnPlayer == null) return;
        TagRenderer tagRender = CustomNameplates.getInstance().getUnlimitedTagManager().getTagRender(cnPlayer);
        if (tagRender == null) return;
        Item<ItemStack> wrapped = BukkitItemManager.instance().wrap(newItem);
        Optional<CustomItem<ItemStack>> optionalCustomItem = wrapped.getCustomItem();
        if (optionalCustomItem.isEmpty()) {
            tagRender.hatOffset(0d);
            return;
        }
        Double customHeight = optionalCustomItem.get().settings().getCustomData(HAT_HEIGHT);
        tagRender.hatOffset(Objects.requireNonNullElse(customHeight, 0d));
    }
}
