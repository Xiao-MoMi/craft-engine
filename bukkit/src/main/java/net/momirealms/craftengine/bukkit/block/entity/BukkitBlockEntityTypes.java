package net.momirealms.craftengine.bukkit.block.entity;

import net.momirealms.craftengine.core.block.entity.BlockEntityType;
import net.momirealms.craftengine.core.block.entity.BlockEntityTypeKeys;
import net.momirealms.craftengine.core.block.entity.BlockEntityTypes;

public class BukkitBlockEntityTypes extends BlockEntityTypes {
    public static final BlockEntityType<SimpleStorageBlockEntity> SIMPLE_STORAGE = register(BlockEntityTypeKeys.SIMPLE_STORAGE);
    public static final BlockEntityType<SimpleParticleBlockEntity> SIMPLE_PARTICLE = register(BlockEntityTypeKeys.SIMPLE_PARTICLE);
    public static final BlockEntityType<WallTorchParticleBlockEntity> WALL_TORCH_PARTICLE = register(BlockEntityTypeKeys.WALL_TORCH_PARTICLE);
    public static final BlockEntityType<SeatBlockEntity> SEAT = register(BlockEntityTypeKeys.SEAT);
    public static final BlockEntityType<ItemFrameBlockEntity> ITEM_FRAME = register(BlockEntityTypeKeys.ITEM_FRAME);

    private BukkitBlockEntityTypes() {}
}