package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.core.world.context.UseOnContext;

import java.nio.file.Path;
import java.util.Map;

public final class GroundBlockItemBehavior extends BlockItemBehavior {
    public static final ItemBehaviorFactory<GroundBlockItemBehavior> FACTORY = new Factory();

    private GroundBlockItemBehavior(Key ceilingBlockId) {
        super(ceilingBlockId);
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        return this.place(new BlockPlaceContext(context));
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        return super.place(context);
    }

    private static class Factory implements ItemBehaviorFactory<GroundBlockItemBehavior> {
        @Override
        public GroundBlockItemBehavior create(Pack pack, Path path, String node, Key key, Map<String, Object> arguments) {
            Object id = arguments.get("block");
            if (id == null) {
                throw new LocalizedResourceConfigException("warning.config.item.behavior.ground_block.missing_block", new IllegalArgumentException("Missing required parameter 'block' for ground_block_item behavior"));
            }
            if (id instanceof Map<?, ?> map) {
                addPendingSection(pack, path, node, key, map);
                return new GroundBlockItemBehavior(key);
            } else {
                return new GroundBlockItemBehavior(Key.of(id.toString()));
            }
        }
    }
}
