package net.momirealms.craftengine.core.block.entity.tick;

import net.momirealms.craftengine.core.world.BlockPos;

public final class ReplaceableTickingBlockEntity implements TickingBlockEntity, BlockEntityTickingStateListener {
    static final int INDEX_UNREGISTERED = -1;
    static final int INDEX_PENDING_ADDITION = -2;

    private TickingBlockEntity target;
    private BlockEntityTickerScheduler owner;
    private int index = INDEX_UNREGISTERED;
    private boolean sleeping;

    public ReplaceableTickingBlockEntity(TickingBlockEntity target) {
        this.target = target;
        this.sleeping = target.isSleeping();
    }

    public TickingBlockEntity target() {
        return this.target;
    }

    public void setTicker(TickingBlockEntity target) {
        BlockEntityTickerScheduler owner = this.owner;
        if (owner == null) rebind(target);
        else owner.rebind(this, target);
    }

    void rebind(TickingBlockEntity target) {
        this.target.setTickingStateListener(null);
        this.target = target;
        if (this.owner != null) {
            bindTickerListener();
            this.owner.stateChanged(this);
        } else {
            this.sleeping = target.isSleeping();
        }
    }

    void attach(BlockEntityTickerScheduler owner) {
        if (this.owner != null) throw new IllegalStateException("Ticker already registered");
        this.owner = owner;
    }

    void bindTickerListener() {
        this.target.setTickingStateListener(this);
        this.sleeping = this.target.isSleeping();
    }

    @Override
    public void run() {
        this.sleeping = this.target.isSleeping();
        this.owner.stateChanged(this);
    }

    void detachTickerListener() {
        this.target.setTickingStateListener(null);
    }

    public int index() {
        return this.index;
    }

    void index(int index) {
        this.index = index;
    }

    @Override
    public boolean isSleeping() {
        return this.sleeping;
    }

    @Override
    public BlockPos pos() {
        return this.target.pos();
    }

    @Override
    public void tick() {
        if (!this.sleeping) {
            this.target.tick();
        }
    }

    @Override
    public boolean isValid() {
        return this.target.isValid();
    }
}
