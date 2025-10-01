package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.item.context.BlockPlaceContext;
import net.momirealms.craftengine.core.item.context.UseOnContext;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;
import java.util.Map;

public class WallBlockItemBehavior extends BlockItemBehavior {
    public static final Factory FACTORY = new Factory();

    public WallBlockItemBehavior(Key wallBlockId) {
        super(wallBlockId);
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        return this.place(new BlockPlaceContext(context));
    }

    public InteractionResult place(BlockPlaceContext context) {
        if (context.getClickedFace().stepY() != 0) {
            return InteractionResult.PASS;
        }
        return super.place(context);
    }

    public static class Factory implements ItemBehaviorFactory {
        @Override
        public ItemBehavior create(Pack pack, Path path, String node, Key key, Map<String, Object> arguments) {
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
