package net.momirealms.craftengine.core.block.entity.tick;

import net.momirealms.craftengine.core.world.BlockPos;

public final class ReplaceableTickingBlockEntity implements TickingBlockEntity {
    private TickingBlockEntity target;

    public ReplaceableTickingBlockEntity(TickingBlockEntity target) {
        this.target = target;
    }

    public TickingBlockEntity target() {
        return this.target;
    }

    public void setTicker(TickingBlockEntity target) {
        this.target = target;
    }

    @Override
    public BlockPos pos() {
        return this.target.pos();
    }

    @Override
    public void tick() {
        this.target.tick();
    }

    @Override
    public boolean isValid() {
        return this.target.isValid();
    }
}
