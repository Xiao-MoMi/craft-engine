package net.momirealms.craftengine.bukkit.block;

import com.sk89q.worldedit.world.block.BlockType;
import net.momirealms.craftengine.core.util.Key;

public class WorldEditHook {

    public static void register(Key id) {
        BlockType.REGISTRY.register(id.toString(), new BlockType(id.toString(), blockState -> blockState));
    }
}
