package net.momirealms.craftengine.core.plugin.ui.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.ui.Observer;
import net.momirealms.craftengine.core.plugin.ui.Subscription;
import net.momirealms.craftengine.core.plugin.ui.state.Signal;
import net.momirealms.craftengine.core.util.ThrowableUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public interface ItemAttachment extends AutoCloseable {
    ItemAttachment PASSIVE = () -> {};

    /**
     * 解除显示关系并关闭关联订阅, 重复关闭不产生额外效果.
     */
    @Override
    void close();

    /**
     * 创建持有观察者与依赖订阅的 attachment.
     *
     * @param item 被挂载的 Item
     * @param observer 本次挂载的失效观察者
     * @return 新 attachment
     */
    @NotNull
    static Tracking tracking(@NotNull Item item, @NotNull Observer<? super Item> observer) {
        return new Tracking(item, observer);
    }

    /**
     * 持有本次挂载建立的全部订阅.
     */
    final class Tracking implements ItemAttachment {
        private final Item item;
        private final Observer<? super Item> observer;
        private final CopyOnWriteArrayList<Subscription> subscriptions = new CopyOnWriteArrayList<>();
        private final AtomicBoolean closed = new AtomicBoolean();

        private Tracking(Item item, Observer<? super Item> observer) {
            this.item = item;
            this.observer = observer;
        }

        void track(@NotNull Subscription subscription) {
            this.subscriptions.add(subscription);
        }

        void subscribeDependencies(
                @NotNull List<Function<Player, Signal<?>>> dependencies,
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
            if (!this.closed.compareAndSet(false, true)) {
                return;
            }
            // 单个订阅关闭失败不阻断其余清理.
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
