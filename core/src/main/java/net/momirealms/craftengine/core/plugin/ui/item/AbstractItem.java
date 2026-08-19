package net.momirealms.craftengine.core.plugin.ui.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.ui.Observer;
import net.momirealms.craftengine.core.plugin.ui.internal.ObservableDispatcher;
import net.momirealms.craftengine.core.plugin.ui.item.provider.ImmediateItemProvider;
import net.momirealms.craftengine.core.plugin.ui.item.provider.ItemProvider;
import net.momirealms.craftengine.core.plugin.ui.item.provider.RenderContext;
import net.momirealms.craftengine.core.plugin.ui.signal.Signal;
import net.momirealms.craftengine.core.plugin.ui.window.Window;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * 以声明式渲染为核心的 Item 骨架.
 *
 * <p>子类实现 {@link #render(RenderContext)} 决定显示内容, 并在构造器里用 dependsOn
 * 声明数据依赖; 依赖失效或调用 {@link #notifyWindows()} 时, 所有挂载它的窗口槽位重新渲染.
 */
public abstract class AbstractItem implements ObservableItem {
    private final ItemProvider itemProvider;
    private final ImmediateItemProvider placeholder;
    private final ObservableDispatcher<Item> observers = new ObservableDispatcher<>(); // 失效广播派发器, notifyWindows 经它送达所有观察者
    private final CopyOnWriteArrayList<Function<? super Player, ? extends Signal<?>>> dependencies = new CopyOnWriteArrayList<>(); // 渲染依赖声明.

    protected AbstractItem() {
        this.itemProvider = this::render;
        this.placeholder = ItemProvider.sync(this::placeholder);
    }

    /**
     * 声明渲染读取了哪些数据源, 它们失效时重新渲染这个 Item.
     * <p><strong>只应在子类构造器里调用.</strong>
     *
     * @param signals 渲染依赖的数据源
     */
    protected final void dependsOn(@NotNull Signal<?>... signals) {
        for (int index = 0; index < signals.length; index++) {
            Signal<?> signal = Objects.requireNonNull(signals[index], "signal");
            this.dependencies.add(ignoredViewer -> signal);
        }
    }

//    protected final void dependsOn(@NotNull PlayerKeyedSignal<?> signal) {
//        Objects.requireNonNull(signal, "signal");
//        this.dependencies.add(viewer -> signal.at(viewer.getUniqueId()));
//    }
//
//    protected final <K> void dependsOn(@NotNull KeyedSignal<K, ?> signal, @NotNull Function<? super Player, ? extends K> keyOf) {
//        Objects.requireNonNull(signal, "signal");
//        Objects.requireNonNull(keyOf, "keyOf");
//        this.dependencies.add(viewer -> signal.at(keyOf.apply(viewer)));
//    }

    /**
     * 根据当前显示上下文发起一次物品渲染.
     * <p>此方法遵守 {@link ItemProvider#provide(RenderContext)} 的渲染约束.</p>
     *
     * @param context 渲染上下文
     * @return 本次显示结果的 Future
     */
    @NotNull
    protected abstract CompletableFuture<? extends net.momirealms.craftengine.core.item.Item> render(RenderContext context);

    /**
     * 返回此显示位置尚无成功渲染结果时使用的占位物品.
     * <p>后续刷新尚未完成时继续显示最近一次成功结果, 不会重新退回占位物品.
     *
     * @param context 渲染上下文
     * @return 首次完成前显示的占位物品
     */
    @NotNull
    protected net.momirealms.craftengine.core.item.Item placeholder(@NotNull RenderContext context) {
        return net.momirealms.craftengine.core.item.Item.empty();
    }

    @NotNull
    @Override
    public final ItemProvider getItemProvider() {
        return this.itemProvider;
    }

    @NotNull
    @Override
    public final ImmediateItemProvider getPlaceholder() {
        return this.placeholder;
    }

    @Override
    public final ItemAttachment attach(@NotNull Window window, @NotNull Observer<? super Item> observer) {
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(observer, "observer");
        ItemAttachment.Tracking attachment = ItemAttachment.tracking(this, observer);
        // 登记观察者并订阅依赖, 任一步失败都关闭挂载回滚
        try {
            attachment.track(this.observers.subscribe(observer));
            attachment.subscribeDependencies(this.dependencies, window.viewer());
            return attachment;
        } catch (RuntimeException | Error throwable) {
            // 回滚失败不能盖掉挂载失败的原因
            try {
                attachment.close();
            } catch (RuntimeException | Error closeFailure) {
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
