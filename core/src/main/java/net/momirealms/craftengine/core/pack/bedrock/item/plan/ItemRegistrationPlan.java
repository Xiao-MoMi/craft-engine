package net.momirealms.craftengine.core.pack.bedrock.item.plan;

import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ItemRegistrationPlan(List<Entry> entries) {

    public ItemRegistrationPlan {
        entries = List.copyOf(entries);
    }

    public record Entry(Key bedrockId,  // craftengine:原始命名空间/物品路径
                        Key javaItemType,
                        Key definitionId,
                        ItemDefinitionBinding definition,
                        ItemRegistrationProperties properties,
                        ResolvedItemBinding source) {

        public ItemBehaviorDescription clientBehavior() {
            return this.source.clientBehavior();
        }

        public ItemBehaviorDescription.Use useBehavior() {
            return this.clientBehavior().use();
        }

        @Nullable
        public ItemBehaviorDescription.Ticks chargeDuration() {
            return this.useBehavior().chargeDuration();
        }
    }
}
