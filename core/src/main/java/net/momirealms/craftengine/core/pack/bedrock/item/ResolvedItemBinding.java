package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

public record ResolvedItemBinding(Key itemId,
                                  Key serverItemType,
                                  Key clientItemType,
                                  @Nullable Key definitionId,
                                  @Nullable ItemDefinitionBinding definition,
                                  ItemBehaviorDescription clientBehavior,
                                  ItemRegistrationProperties clientProperties) {
}
