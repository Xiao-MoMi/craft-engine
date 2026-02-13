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
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;

public final class MythicItemDrop extends ItemDrop implements IItemDrop {
    private static final Constructor<?> constructor$BukkitItemStack = ReflectionUtils.getConstructor(BukkitItemStack.class, ItemStack.class);
    private static final boolean useReflection = constructor$BukkitItemStack != null;
    private final LazyReference<CustomItem<ItemStack>> customItem;
    private final String itemId;

    public MythicItemDrop(String line, MythicLineConfig config, LazyReference<CustomItem<ItemStack>> customItem, String itemId) {
        super(line, config);
        this.customItem = customItem;
        this.itemId = itemId;
    }

    @Override
    public AbstractItemStack getDrop(DropMetadata dropMetadata, double amount) {
        ItemBuildContext context = ItemBuildContext.empty();
        SkillCaster caster = dropMetadata.getCaster();
        if (caster != null && caster.getEntity() instanceof AbstractPlayer abstractPlayer) {
            Entity bukkitEntity = abstractPlayer.getBukkitEntity();
            if (bukkitEntity instanceof Player bukkitPlayer) {
                var player = BukkitAdaptors.adapt(bukkitPlayer);
                context = ItemBuildContext.of(player);
            }
        }
        int amountInt = MiscUtils.floor(amount + 0.5F);
        CustomItem<ItemStack> customItem = this.customItem.get();
        if (customItem == null) {
            throw new IllegalArgumentException("Cannot find CraftEngine item " + this.itemId);
        } else {
            ItemStack itemStack = customItem.buildItemStack(context, amountInt);
            return adapt(itemStack).amount(amountInt);
        }
    }

    private static AbstractItemStack adapt(ItemStack itemStack) {
        if (!useReflection) {
            return BukkitAdapter.adapt(itemStack);
        }
        try {
            return (AbstractItemStack) constructor$BukkitItemStack.newInstance(itemStack);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("adapt(ItemStack itemStack) error: " + e.getMessage());
            return null;
        }
    }
}
