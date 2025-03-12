package net.momirealms.craftengine.core.block.behavior;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.shared.block.BlockBehavior;

import java.util.Map;

public interface BlockBehaviorFactory {

    BlockBehavior create(CustomBlock block, Map<String, Object> arguments);
}
