package net.momirealms.craftengine.bukkit.pack.bedrock.item;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.core.entity.LivingEntity;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemBehaviorDescription;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemBehaviorDescription.*;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemBehaviorReader;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.world.item.BowItemProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.CrossbowItemProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.component.SwingAnimationProxy;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BukkitItemBehaviorReader implements ItemBehaviorReader {
    private static final List<Key> COMPONENTS = List.of(
            DataComponentKeys.CONSUMABLE,
            DataComponentKeys.FOOD,
            DataComponentKeys.BLOCKS_ATTACK,
            DataComponentKeys.KINETIC_WEAPON,
            DataComponentKeys.PIERCING_WEAPON,
            DataComponentKeys.USE_EFFECTS,
            DataComponentKeys.USE_COOLDOWN,
            DataComponentKeys.INSTRUMENT,
            DataComponentKeys.ENCHANTMENTS,
            DataComponentKeys.SWING_ANIMATION,
            DataComponentKeys.ATTACK_ANIMATION,
            DataComponentKeys.INTERACT_ANIMATION
    );
    private final Map<Key, Object> componentTypes;

    public BukkitItemBehaviorReader() {
        Map<Key, Object> componentTypes = new LinkedHashMap<>();
        for (Key key : COMPONENTS) {
            Object type = DataComponentTypes.byId(key);
            if (type != null) {
                componentTypes.put(key, type);
            }
        }
        this.componentTypes = Map.copyOf(componentTypes);
    }

    @Override
    public ItemBehaviorDescription read(Item item, @Nullable LivingEntity user) {
        Object stack = item.minecraftItem();
        UseAnimation animation = UseAnimation.fromNative(((Enum<?>) ItemStackProxy.INSTANCE.getUseAnimation(stack)).name(), VersionHelper.MINECRAFT_VERSION);
        Duration maximumDuration;
        if (!VersionHelper.isOrAbove1_21) {
            maximumDuration = new Ticks(ItemStackProxy.INSTANCE.getUseDuration(stack));
        } else if (user != null) {
            maximumDuration = new Ticks(ItemStackProxy.INSTANCE.getUseDuration(stack, user.minecraftEntity()));
        } else {
            maximumDuration = RequiresEntity.INSTANCE;
        }
        Use use = new Use(animation, maximumDuration, chargeDuration(stack, user), ItemStackProxy.INSTANCE.useOnRelease(stack));
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
        Map<Key, Component> components = new LinkedHashMap<>();
        for (Map.Entry<Key, Object> entry : this.componentTypes.entrySet()) {
            JsonElement effective = item.getComponentAsJson(entry.getValue());
            JsonElement defaultValue = item.type().getJsonComponent(entry.getValue()).orElse(null);
            components.put(entry.getKey(), Component.of(effective, defaultValue));
        }
        return new ItemBehaviorDescription(use, attack, interact, components);
    }

    @Nullable
    private Duration chargeDuration(Object stack, @Nullable LivingEntity user) {
        Object itemType = ItemStackProxy.INSTANCE.getItem(stack);
        if (BowItemProxy.CLASS.isInstance(itemType)) {
            return new Ticks(BowItemProxy.INSTANCE.getMaxDrawDuration());
        }
        if (CrossbowItemProxy.CLASS.isInstance(itemType)) {
            if (!VersionHelper.isOrAbove1_21) {
                return new Ticks(CrossbowItemProxy.INSTANCE.getChargeDuration(stack));
            }
            if (user != null) {
                return new Ticks(CrossbowItemProxy.INSTANCE.getChargeDuration(stack, user.minecraftEntity()));
            }
            return RequiresEntity.INSTANCE;
        }
        return null;
    }

    private Swing readSwing(Object animation) {
        SwingAnimation type = SwingAnimation.valueOf(((Enum<?>) SwingAnimationProxy.INSTANCE.type(animation)).name());
        return new Swing(type, SwingAnimationProxy.INSTANCE.duration(animation));
    }
}
