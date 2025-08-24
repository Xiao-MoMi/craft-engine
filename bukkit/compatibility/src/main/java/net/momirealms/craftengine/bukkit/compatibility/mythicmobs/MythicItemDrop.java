package net.momirealms.craftengine.bukkit.compatibility.mythicmobs;

import io.lumine.mythic.api.adapters.AbstractItemStack;
import io.lumine.mythic.api.adapters.AbstractPlayer;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.drops.DropMetadata;
import io.lumine.mythic.api.drops.IItemDrop;
import io.lumine.mythic.api.skills.SkillCaster;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.adapters.BukkitItemStack;
import io.lumine.mythic.core.drops.droppables.ItemDrop;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.MCUtils;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;

public class MythicItemDrop extends ItemDrop implements IItemDrop {
    private final CustomItem<ItemStack> customItem;
    private static final Constructor<?> constructor$BukkitItemStack = ReflectionUtils.getConstructor(BukkitItemStack.class, ItemStack.class);
    private static final boolean useReflection = constructor$BukkitItemStack != null;

    public MythicItemDrop(String line, MythicLineConfig config, CustomItem<ItemStack> customItem) {
        super(line, config);
        this.customItem = customItem;
    }

    @Override
    public AbstractItemStack getDrop(DropMetadata dropMetadata, double amount) {
        ItemBuildContext context = ItemBuildContext.EMPTY;
        SkillCaster caster = dropMetadata.getCaster();
        if (caster != null && caster.getEntity() instanceof AbstractPlayer abstractPlayer) {
            Entity bukkitEntity = abstractPlayer.getBukkitEntity();
            if (bukkitEntity instanceof Player bukkitPlayer) {
                var player = BukkitCraftEngine.instance().adapt(bukkitPlayer);
                context = ItemBuildContext.of(player);
            }
        }
        int amountInt = MCUtils.fastFloor(amount + 0.5F);
        ItemStack itemStack = this.customItem.buildItemStack(context, amountInt);
        return adapt(itemStack).amount(amountInt);
    }

    private static AbstractItemStack adapt(ItemStack itemStack) {
        if (useReflection) {
            try {
                return (AbstractItemStack) constructor$BukkitItemStack.newInstance(itemStack);
            } catch (Exception e) {
                CraftEngine.instance().logger().warn("adapt(ItemStack itemStack) error: " + e.getMessage());
                return null;
            }
        } else {
            return BukkitAdapter.adapt(itemStack);
        }
    }
}
