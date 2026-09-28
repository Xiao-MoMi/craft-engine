package net.momirealms.craftengine.bukkit.item.listener;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.EquipmentSlotUtils;
import net.momirealms.craftengine.bukkit.util.PlayerUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;


public final class PreventBreakListener implements Listener {
    private final BukkitItemManager itemManager;
    private boolean registered = false;

    public PreventBreakListener(BukkitItemManager itemManager) {
        this.itemManager = itemManager;
    }

    public void register(Plugin plugin) {
        if (this.registered) return;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.registered = true;
    }

    public void unregister() {
        if (!this.registered) return;
        HandlerList.unregisterAll(this);
        this.registered = false;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onItemDamage(PlayerItemDamageEvent event) {
        ItemStack itemStack = event.getItem();
        Item wrapped = this.itemManager.wrap(itemStack);
        ItemDefinition customItem = wrapped.getDefinitionOrNull();
        if (customItem == null) return;
        if (!customItem.settings().preventBreak()) return;
        int maxDamage = wrapped.maxDamage();
        if (maxDamage <= 0) return;
        int damage = wrapped.damage().orElse(0);
        // 本次损失会导致耐久归零，钳制损伤使物品保留最后一点耐久
        if (damage + event.getDamage() < maxDamage) return;
        // 穿在身上的装备耐久耗尽时自动脱下（损伤在脱下时已手动应用）
        if (tryUnequipBrokenArmor(event.getPlayer(), wrapped, maxDamage)) {
            event.setCancelled(true);
        } else {
            int allowedDamage = maxDamage - 1 - damage;
            if (allowedDamage <= 0) {
                event.setCancelled(true);
            } else {
                event.setDamage(allowedDamage);
            }
        }
    }

    private boolean tryUnequipBrokenArmor(Player player, Item wrapped, int maxDamage) {
        PlayerInventory inventory = player.getInventory();
        Object damagedStack = wrapped.minecraftItem();
        for (EquipmentSlot slot : EquipmentSlotUtils.ARMOR_SLOTS) {
            BukkitItem armor = this.itemManager.wrap(inventory.getItem(slot));
            if (armor.isEmpty()) continue;
            // 通过 NMS 栈引用相等定位正在被损伤的那件装备（Bukkit ItemStack 是镜像，equals 可能误伤内容相同的另一件）
            if (armor.minecraftItem() != damagedStack) continue;
            BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
            if (serverPlayer != null) {
                // 手动把耐久压到最后一点
                wrapped.damage(maxDamage - 1);
                inventory.setItem(slot, null);
                PlayerUtils.giveItem(serverPlayer, armor.count(), armor, false);
            }
            return true;
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onAttackWithBrokenItem(EntityDamageByEntityEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (this.itemManager.isBrokenItem(player.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onBreakBlockWithBrokenItem(BlockBreakEvent event) {
        if (this.itemManager.isBrokenItem(event.getPlayer().getInventory().getItemInMainHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onShootBowWithBrokenItem(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (this.itemManager.isBrokenItem(event.getBow())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onDispenseBrokenArmor(BlockDispenseArmorEvent event) {
        if (this.itemManager.isBrokenItem(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteractWithBrokenItem(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action == Action.PHYSICAL) return;
        ItemStack itemStack = event.getItem();
        if (!this.itemManager.isBrokenItem(itemStack)) return;
        if (VersionHelper.hasPaperPatch) {
            event.setUseItemInHand(Event.Result.DENY);
        } else {
            event.setCancelled(true);
        }
    }

    // TODO 物品栏点击装备
}
