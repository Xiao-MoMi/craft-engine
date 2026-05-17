package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.PendingConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.core.world.context.UseOnContext;

import java.nio.file.Path;
import java.util.Map;

public final class CeilingBlockItemBehavior extends BlockItemBehavior {
    public static final ItemBehaviorFactory<CeilingBlockItemBehavior> FACTORY = new Factory();

    private CeilingBlockItemBehavior(Key ceilingBlockId) {
        super(ceilingBlockId);
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        return this.place(new BlockPlaceContext(context));
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (context.getClickedFace() != Direction.DOWN) {
            return InteractionResult.PASS;
        }
        return super.place(context);
    }

    private static class Factory implements ItemBehaviorFactory<CeilingBlockItemBehavior> {
        @Override
        public CeilingBlockItemBehavior create(Pack pack, Path path, Key key, ConfigSection section) {
            ConfigValue blockValue = section.getNonNullValue("block", ConfigConstants.ARGUMENT_SECTION);
            if (blockValue.is(Map.class)) {
                BukkitBlockManager.instance().blockParser().addPendingConfigSection(new PendingConfigSection(pack, path, key, blockValue.getAsSection()));
                return new CeilingBlockItemBehavior(key);
            } else {
                return new CeilingBlockItemBehavior(blockValue.getAsIdentifier());
            }
        }
    }
}
