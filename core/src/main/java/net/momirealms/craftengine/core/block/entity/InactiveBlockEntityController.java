package net.momirealms.craftengine.core.block.entity;

import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Map;

public final class InactiveBlockEntityController extends BlockEntityController {
    private final CompoundTag tag;

    public InactiveBlockEntityController(BlockEntity blockEntity, CompoundTag tag) {
        super(blockEntity);
        this.tag = tag;
    }

    @Override
    public void saveCustomData(CompoundTag data) {
        for (Map.Entry<String, Tag> entry : this.tag.entrySet()) {
            data.put(entry.getKey(), entry.getValue());
        }
    }
}
