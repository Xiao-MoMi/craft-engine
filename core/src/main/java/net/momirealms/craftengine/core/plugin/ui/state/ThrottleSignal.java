package net.momirealms.craftengine.core.plugin.ui.state;

final class ThrottleSignal<T> extends PacedSignal<T> {
    private boolean trailing;

    ThrottleSignal(AbstractSignal<T> source, long delay, Delayer delayer) {
        super(source, delay, delayer);
    }

    @Override
    boolean onSourceDirtyLocked() {
        if (this.waitingLocked()) {
            this.trailing = true;
            return false;
        }
        return this.emitAndOpenWindowLocked();
    }

    @Override
    boolean onFireLocked() {
        if (!this.trailing) return false;
        this.trailing = false;
        return this.emitAndOpenWindowLocked();
    }

    @Override
    void onInactiveLocked() {
        this.trailing = false;
    }

    // 先排窗口到期任务再拍快照. 调度失败时快照不动, 下一次失效可以重试.
    private boolean emitAndOpenWindowLocked() {
        if (!this.sourceChangedLocked()) return false;
        this.scheduleLocked();
        this.captureLocked();
        return true;
    }
}
