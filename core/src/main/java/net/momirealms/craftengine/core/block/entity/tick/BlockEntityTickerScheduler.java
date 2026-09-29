package net.momirealms.craftengine.core.block.entity.tick;

import ca.spottedleaf.concurrentutil.collection.MultiThreadedQueue;

import java.util.Queue;

public final class BlockEntityTickerScheduler {
    private final ActiveBlockEntityTickersList tickers = new ActiveBlockEntityTickersList();
    private final boolean async;
    private final Queue<Runnable> pendingLifecycleChanges;

    public BlockEntityTickerScheduler() {
        this(false);
    }

    private BlockEntityTickerScheduler(boolean async) {
        this.async = async;
        this.pendingLifecycleChanges = async ? new MultiThreadedQueue<>() : null;
    }

    public static BlockEntityTickerScheduler async() {
        return new BlockEntityTickerScheduler(true);
    }

    public void add(TickingBlockEntity ticker) {
        ReplaceableTickingBlockEntity wrapper = ticker instanceof ReplaceableTickingBlockEntity replaceable ? replaceable : new ReplaceableTickingBlockEntity(ticker);
        wrapper.attach(this);
        if (this.async) this.pendingLifecycleChanges.add(() -> register(wrapper));
        else register(wrapper);
    }

    private void register(ReplaceableTickingBlockEntity ticker) {
        ticker.bindTickerListener();
        this.tickers.stateChanged(ticker);
    }

    void rebind(ReplaceableTickingBlockEntity ticker, TickingBlockEntity target) {
        if (this.async) this.pendingLifecycleChanges.add(() -> ticker.rebind(target));
        else ticker.rebind(target);
    }

    void stateChanged(ReplaceableTickingBlockEntity ticker) {
        this.tickers.stateChanged(ticker);
    }

    public int activeSize() {
        return this.tickers.activeSize();
    }

    public int size() {
        return this.tickers.size();
    }

    public void tick() {
        tick(true);
    }

    public void tick(boolean runTickers) {
        if (this.async) {
            Runnable change;
            while ((change = this.pendingLifecycleChanges.poll()) != null) change.run();
        }
        if (runTickers) this.tickers.tick();
    }
}
