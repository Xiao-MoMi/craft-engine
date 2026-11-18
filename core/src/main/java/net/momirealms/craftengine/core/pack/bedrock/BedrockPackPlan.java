package net.momirealms.craftengine.core.pack.bedrock;

import net.momirealms.craftengine.core.pack.bedrock.block.WorldBlockPlan;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRegistrationPlan;
import net.momirealms.craftengine.core.pack.bedrock.model.NativeModelSource;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Map;

public record BedrockPackPlan(ItemRegistrationPlan items,
                              WorldBlockPlan blocks,
                              List<NativeModelSource> nativeModels,
                              Map<Key, Key> soundReplacements) {
}
