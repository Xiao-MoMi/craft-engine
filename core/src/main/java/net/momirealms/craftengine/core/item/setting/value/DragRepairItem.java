package net.momirealms.craftengine.core.item.setting.value;

import net.momirealms.craftengine.core.item.ItemSelector;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.ItemUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record DragRepairItem(ItemSelector[] targets, NumberProvider amount, NumberProvider percent, @Nullable SoundData sound) {

    public DragRepairItem(List<String> targets, NumberProvider amount, NumberProvider percent, @Nullable SoundData sound) {
        this(ItemUtils.parseItemOrTag(targets), amount, percent, sound);
    }

    public int durabilityPerItem(int maxDamage, @Nullable Context context) {
        if (context != null) {
            return (int) (this.amount.getInt(context) + this.percent.getDouble(context) * maxDamage);
        }
        return (int) (this.amount.getInt() + this.percent.getDouble() * maxDamage);
    }
}
