package net.momirealms.craftengine.bukkit.pack.bedrock.item;

import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemBehaviorDescription;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemBehaviorDescription.*;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemBehaviorReader;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.BowItemProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.CrossbowItemProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.InstrumentItemProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.InstrumentProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.component.SwingAnimationProxy;
import org.jetbrains.annotations.Nullable;

public final class BukkitItemBehaviorReader implements ItemBehaviorReader {
    private static final int CROSSBOW_BASE_CHARGE_TICKS = 25;

    @Override
    public ItemBehaviorDescription read(Item item) {
        Object stack = item.minecraftItem();
        Object itemType = ItemStackProxy.INSTANCE.getItem(stack);
        UseAnimation animation = UseAnimation.fromNative(((Enum<?>) ItemStackProxy.INSTANCE.getUseAnimation(stack)).name(), VersionHelper.MINECRAFT_VERSION);
        Use use = new Use(animation,
                new Ticks(this.maximumDuration(stack, itemType)),
                this.chargeDuration(itemType),
                ItemStackProxy.INSTANCE.useOnRelease(stack)
        );
        Swing attack;
        Swing interact;
        if (VersionHelper.isOrAbove26_3) {
            attack = readSwing(ItemStackProxy.INSTANCE.getAttackAnimation(stack));
            interact = readSwing(ItemStackProxy.INSTANCE.getInteractAnimation(stack));
        } else if (VersionHelper.isOrAbove1_21_11) {
            attack = readSwing(ItemStackProxy.INSTANCE.getSwingAnimation(stack));
            interact = attack;
        } else {
            attack = Swing.LEGACY;
            interact = attack;
        }
        return new ItemBehaviorDescription(use, attack, interact);
    }

    private int maximumDuration(Object stack, Object itemType) {
        // 1.21.5 之前弩的最大使用时长为蓄力时长加 3 tick；注册计划只记录不含附魔修正的基础值。
        if (!VersionHelper.isOrAbove1_21_5 && CrossbowItemProxy.CLASS.isInstance(itemType)) {
            return CROSSBOW_BASE_CHARGE_TICKS + 3;
        }
        if (!VersionHelper.isOrAbove1_21) {
            return ItemStackProxy.INSTANCE.getUseDuration(stack);
        }
        // 这些版本的乐器只借实体取得注册表，直接复用原版乐器解析即可避免实体依赖。
        if (VersionHelper.isOrAbove1_21_2 && !VersionHelper.isOrAbove26_1 && InstrumentItemProxy.CLASS.isInstance(itemType)) {
            Object holder = InstrumentItemProxy.INSTANCE.getInstrument(itemType, stack, RegistryUtils.getRegistryAccess()).orElse(null);
            if (holder == null) {
                return 0;
            }
            return (int) Math.floor(InstrumentProxy.INSTANCE.useDuration(HolderProxy.INSTANCE.value(holder)) * 20.0F);
        }
        // 其余已支持版本的原版实现只读取物品或固定值，不访问实体参数。
        return ItemStackProxy.INSTANCE.getUseDuration(stack, null);
    }

    @Nullable
    private Ticks chargeDuration(Object itemType) {
        if (BowItemProxy.CLASS.isInstance(itemType)) {
            return new Ticks(BowItemProxy.INSTANCE.getMaxDrawDuration());
        }
        if (CrossbowItemProxy.CLASS.isInstance(itemType)) {
            return new Ticks(CROSSBOW_BASE_CHARGE_TICKS);
        }
        return null;
    }

    private Swing readSwing(Object animation) {
        SwingAnimation type = SwingAnimation.valueOf(((Enum<?>) SwingAnimationProxy.INSTANCE.type(animation)).name());
        return new Swing(type, SwingAnimationProxy.INSTANCE.duration(animation));
    }
}
