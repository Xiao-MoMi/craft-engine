package net.momirealms.craftengine.core.plugin.ui.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.ui.ObservableDispatcher;
import net.momirealms.craftengine.core.plugin.ui.Observer;
import net.momirealms.craftengine.core.plugin.ui.item.provider.ImmediateItemProvider;
import net.momirealms.craftengine.core.plugin.ui.item.provider.ItemProvider;
import net.momirealms.craftengine.core.plugin.ui.item.provider.RenderContext;
import net.momirealms.craftengine.core.plugin.ui.state.KeyedSignal;
import net.momirealms.craftengine.core.plugin.ui.state.PlayerKeyedSignal;
import net.momirealms.craftengine.core.plugin.ui.state.Signal;
import net.momirealms.craftengine.core.plugin.ui.window.Window;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public abstract class AbstractItem implements ObservableItem {
    private final ItemProvider itemProvider;
    private final ImmediateItemProvider placeholder;
    private final ObservableDispatcher<Item> observers = new ObservableDispatcher<>();
    private final CopyOnWriteArrayList<Function<Player, Signal<?>>> dependencies = new CopyOnWriteArrayList<>();

    protected AbstractItem() {
        this.itemProvider = this::render;
        this.placeholder = ItemProvider.sync(this::placeholder);
    }

    /**
     * 声明渲染依赖, 只应在子类构造器中调用.
     *
     * @param signals 渲染依赖的数据源
     */
    protected final void dependsOn(@NotNull Signal<?>... signals) {
        for (int index = 0; index < signals.length; index++) {
            Signal<?> signal = signals[index];
            this.dependencies.add(ignoredViewer -> signal);
        }
    }

    /**
     * 声明按查看者 UUID 取值的渲染依赖, 只应在子类构造器中调用.
     *
     * @param signal 玩家分区数据源
     */
    protected final void dependsOn(@NotNull PlayerKeyedSignal<?> signal) {
        this.dependencies.add(signal::at);
    }

    /**
     * 声明通过查看者计算分区 key 的渲染依赖, 只应在子类构造器中调用.
     *
     * @param <K> 分区 key 类型
     * @param signal 分区数据源
     * @param keyOf 从查看者取得分区 key 的函数
     */
    protected final <K> void dependsOn(@NotNull KeyedSignal<K, ?> signal, @NotNull Function<Player, K> keyOf) {
        this.dependencies.add(viewer -> signal.at(keyOf.apply(viewer)));
    }

    /**
     * 为当前显示位置计算物品内容.
     *
     * @param context 渲染上下文
     * @return 本次显示结果
     */
    @NotNull
    protected abstract CompletableFuture<net.momirealms.craftengine.core.item.Item> render(@NotNull RenderContext context);

    /**
     * 返回首次渲染成功前使用的占位物品.
     *
     * @param context 渲染上下文
     * @return 占位物品
     */
    @NotNull
    protected net.momirealms.craftengine.core.item.Item placeholder(@NotNull RenderContext context) {
        return net.momirealms.craftengine.core.item.Item.empty();
    }

    @Override
    @NotNull
    public final ItemProvider getItemProvider() {
        return this.itemProvider;
    }

    @Override
    @NotNull
    public final ImmediateItemProvider getPlaceholder() {
        return this.placeholder;
    }

    @Override
    public ItemAttachment attach(@NotNull Window window, @NotNull Observer<? super Item> observer) {
        ItemAttachment.Tracking attachment = ItemAttachment.tracking(this, observer);
        // 观察者与依赖必须同时建立, 中途失败时撤销整次挂载.
        try {
            attachment.track(this.observers.subscribe(observer));
            attachment.subscribeDependencies(this.dependencies, window.viewer());
            return attachment;
        } catch (RuntimeException | Error throwable) {
            try {
                attachment.close();
            } catch (RuntimeException | Error closeFailure) {
                // 原始挂载异常仍是主异常, 清理失败作为补充信息.
                throwable.addSuppressed(closeFailure);
            }
            throw throwable;
        }
    }

    @Override
    public final void notifyWindows() {
        this.observers.publish(this);
    }
}
