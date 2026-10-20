package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.entity.LivingEntity;
import net.momirealms.craftengine.core.item.Item;
import org.jetbrains.annotations.Nullable;

public interface ItemBehaviorReader {

    ItemBehaviorDescription read(Item item, @Nullable LivingEntity user);
}
