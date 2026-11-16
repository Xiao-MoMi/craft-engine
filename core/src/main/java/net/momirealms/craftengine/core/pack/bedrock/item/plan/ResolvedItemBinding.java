package net.momirealms.craftengine.core.pack.bedrock.item.plan;

import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

public record ResolvedItemBinding(Key itemId,
                                  Key serverItemType,
                                  Key clientItemType,
                                  Key itemModel,
                                  ItemModelDefinitionBinding definition, // 可能为空，但是空的不会参与生成
                                  ItemBehaviorDescription clientBehavior,
                                  ItemRegistrationProperties clientProperties) {

    public Key bedrockId() {
        return Key.ce(this.itemId.namespace() + '/' + this.itemId.value());
    }

    public ItemBehaviorDescription.Use useBehavior() {
        return this.clientBehavior.use();
    }

    @Nullable
    public ItemBehaviorDescription.Ticks chargeDuration() {
        return this.useBehavior().chargeDuration();
    }
}
