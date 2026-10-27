package net.momirealms.craftengine.bukkit.compatibility.mythicmobs;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.ThreadSafetyLevel;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class TakeCEItemMechanic implements ITargetedEntitySkill {
    private final Key itemId;
    private final int amount;

    public TakeCEItemMechanic(MythicLineConfig config) {
        String item = config.getString(new String[]{"item", "i"}, "");
        if (item.regionMatches(true, 0, "craftengine:", 0, 12) && item.indexOf(':', 12) >= 0) {
            item = item.substring(12);
        }
        this.itemId = item.isEmpty() ? null : Key.of(item);
        this.amount = config.getInteger(new String[]{"amount", "a"}, 1);
    }

    @Override
    public ThreadSafetyLevel getThreadSafetyLevel() {
        return ThreadSafetyLevel.SYNC_ONLY;
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        if (!(target.getBukkitEntity() instanceof Player player)) {
            return SkillResult.INVALID_TARGET;
        }
        if (this.itemId == null || this.amount <= 0) {
            return SkillResult.INVALID_CONFIG;
        }
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getStorageContents();
        int remaining = this.amount;
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack item = contents[slot];
            if (!this.itemId.equals(CraftEngineItems.getCustomItemId(item))) {
                continue;
            }
            int removed = Math.min(remaining, item.getAmount());
            if (removed == item.getAmount()) {
                inventory.setItem(slot, null);
            } else {
                item.setAmount(item.getAmount() - removed);
                inventory.setItem(slot, item);
            }
            remaining -= removed;
        }
        return SkillResult.SUCCESS;
    }
}
