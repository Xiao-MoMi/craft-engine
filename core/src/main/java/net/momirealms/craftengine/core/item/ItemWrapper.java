package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ItemWrapper {

    Object minecraftItem();

    default Object platformItem() {
        return minecraftItem();
    }

    int count();

    void count(int amount);

    ItemWrapper copy();

    ItemWrapper copyWithCount(int count);

    void shrink(int amount);

    void grow(int amount);

    void hurtAndBreak(int amount, @NotNull Player player, @Nullable EquipmentSlot slot);
}
