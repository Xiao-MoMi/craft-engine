package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.core.world.context.UseOnContext;

import java.nio.file.Path;
import java.util.Map;

public final class WallBlockItemBehavior extends BlockItemBehavior {
    public static final ItemBehaviorFactory<WallBlockItemBehavior> FACTORY = new Factory();

    private WallBlockItemBehavior(Key wallBlockId) {
        super(wallBlockId);
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        return this.place(new BlockPlaceContext(context));
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (context.getClickedFace().stepY() != 0) {
            return InteractionResult.PASS;
        }
        return super.place(context);
    }

    private static class Factory implements ItemBehaviorFactory<WallBlockItemBehavior> {
        @Override
        public WallBlockItemBehavior create(Pack pack, Path path, String node, Key key, Map<String, Object> arguments) {
            Object id = arguments.get("block");
            if (id == null) {
                throw new LocalizedResourceConfigException("warning.config.item.behavior.wall_block.missing_block", new IllegalArgumentException("Missing required parameter 'block' for wall_block_item behavior"));
            }
            if (id instanceof Map<?, ?> map) {
                addPendingSection(pack, path, node, key, map);
                return new WallBlockItemBehavior(key);
            } else {
                return new WallBlockItemBehavior(Key.of(id.toString()));
            }
        }
    }
}
