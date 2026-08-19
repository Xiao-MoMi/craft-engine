package net.momirealms.craftengine.core.plugin.ui.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.ui.Observer;
import net.momirealms.craftengine.core.plugin.ui.Subscription;
import net.momirealms.craftengine.core.plugin.ui.signal.Signal;
import net.momirealms.craftengine.core.util.ThrowableUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Item 与一个最终显示槽位之间的挂载关系.
 * Window 在替换显示路径或关闭时必须调用 {@link #close()}.
 */
public interface ItemAttachment extends AutoCloseable {
    ItemAttachment PASSIVE = () -> {};

    @Override
    void close();

    @NotNull
    static Tracking tracking(@NotNull Item item, @NotNull Observer<? super Item> observer) {
        return new Tracking(item, observer);
    }

    /**
     * 持有本次挂载取得的全部订阅, 并把依赖失效转成对这一条显示路径的标脏.
     */
    final class Tracking implements ItemAttachment {
        private final Item item;
        private final Observer<? super Item> observer;
        private final CopyOnWriteArrayList<Subscription> subscriptions = new CopyOnWriteArrayList<>(); // 挂载线程写入, 关闭线程读取.
        private final AtomicBoolean closed = new AtomicBoolean(); // 一次性关闭哨兵, 关闭后依赖失效不再转发

        private Tracking(Item item, Observer<? super Item> observer) {
            this.item = item;
            this.observer = observer;
        }

        //  把一条订阅交给本次挂载管理, 关闭挂载时一并关闭.
        void track(@NotNull Subscription subscription) {
            this.subscriptions.add(subscription);
        }

        void subscribeDependencies(
                @NotNull List<? extends Function<? super Player, ? extends Signal<?>>> dependencies,
                @NotNull Player viewer
        ) {
            for (int index = 0; index < dependencies.size(); index++) {
                Signal<?> signal = dependencies.get(index).apply(viewer);
                this.track(signal.onDirty(this::dirty));
            }
        }

        private void dirty() {
            if (this.closed.get()) return;
            this.observer.onUpdate(this.item);
        }

        @Override
        public void close() {
            if (!this.closed.compareAndSet(false, true)) return;
            RuntimeException failure = null;
            for (Subscription subscription : this.subscriptions) {
                try {
                    subscription.close();
                } catch (RuntimeException exception) {
                    failure = ThrowableUtils.combine(failure, exception);
                }
            }
            if (failure != null) {
                throw failure;
            }
        }
    }
}
