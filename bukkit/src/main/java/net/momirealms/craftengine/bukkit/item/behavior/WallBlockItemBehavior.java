package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.PendingConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
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
        public WallBlockItemBehavior create(Pack pack, Path path, Key key, ConfigSection section) {
            ConfigValue blockValue = section.getNonNullValue("block", ConfigConstants.ARGUMENT_SECTION);
            if (blockValue.is(Map.class)) {
                BukkitBlockManager.instance().blockParser().addPendingConfigSection(new PendingConfigSection(pack, path, key, blockValue.getAsSection()));
                return new WallBlockItemBehavior(key);
            } else {
                return new WallBlockItemBehavior(blockValue.getAsIdentifier());
            }
        }
    }
}
