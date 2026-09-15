package net.momirealms.craftengine.core.plugin.ui.state;

final class DebounceSignal<T> extends PacedSignal<T> {

    DebounceSignal(AbstractSignal<T> source, long delay, Delayer delayer) {
        super(source, delay, delayer);
    }

    @Override
    boolean onSourceDirtyLocked() {
        this.scheduleLocked();
        return false;
    }

    @Override
    boolean onFireLocked() {
        // 上游版本没有越过快照时不补发订阅前已经并入基线的写入.
        if (!this.sourceChangedLocked()) return false;
        this.captureLocked();
        return true;
    }
}
