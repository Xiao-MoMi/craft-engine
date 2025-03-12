package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.shared.block.BlockBehavior;

import java.util.Map;

public class StrippableBlockBehavior extends BlockBehavior {
    public static final Factory FACTORY = new Factory();
    private final Key stripped;

    public StrippableBlockBehavior(Key stripped) {
        this.stripped = stripped;
    }

    public Key stripped() {
        return stripped;
    }

    public static class Factory implements BlockBehaviorFactory {

        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            String stripped = (String) arguments.get("stripped");
            if (stripped == null) {
                throw new IllegalArgumentException("stripped is null");
            }
            return new StrippableBlockBehavior(Key.of(stripped));
        }
    }
}
