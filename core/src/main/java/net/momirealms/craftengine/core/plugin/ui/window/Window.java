package net.momirealms.craftengine.core.plugin.ui.window;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.ui.Subscription;
import net.momirealms.craftengine.core.plugin.ui.item.provider.ImmediateItemProvider;
import net.momirealms.craftengine.core.plugin.ui.item.provider.ItemProvider;
import net.momirealms.craftengine.core.plugin.ui.signal.Signal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 由一名玩家查看的 Pane 会话.
 * <p>所有变更方法都可以从任意线程调用. 命令会按玩家串行执行, 并只在玩家实体线程修改
 * Pane, 菜单和协议状态. 查询方法返回最近一次已应用状态的线程安全快照.
 */
public interface Window {

    /**
     * 创建普通窗口的 Builder.
     * 普通窗口由 {@code pane} 作为上半部分, 下半部分映射玩家原生物品栏.
     *
     * @param pane 上半部分 Pane
     * @return 可重复使用的 Builder
     */
    static @NotNull NormalWindow.Builder builder(@NotNull Pane pane) {
        return NormalWindow.builder().setUpperPane(pane);
    }

    /**
     * 创建上下分离窗口的 Builder.
     * 上下两个 Pane 分别控制容器和玩家物品栏区域.
     *
     * @param upperPane 上半部分 Pane
     * @param lowerPane 下半部分 9x4 Pane
     * @return 可重复使用的 Builder
     */
    static @NotNull NormalWindow.Builder splitBuilder(@NotNull Pane upperPane, @NotNull Pane lowerPane) {
        return NormalWindow.builder().setUpperPane(upperPane).setLowerPane(lowerPane);
    }

    /**
     * 创建合并窗口的 Builder.
     * 单个 Pane 同时覆盖容器和玩家物品栏区域.
     *
     * @param pane 合并后的 Pane
     * @return 可重复使用的 Builder
     */
    static @NotNull NormalWindow.Builder mergedBuilder(@NotNull Pane pane) {
        return NormalWindow.mergedBuilder(pane);
    }

    /**
     * 请求打开 Window, Stage 完成表示服务端打开流程已经执行, 不表示客户端已经显示.
     *
     * @return 打开请求的执行结果
     */
    @NotNull CompletableFuture<OpenResult> open();

    /**
     * 从本 Window 打开下一扇 Window, 本 Window 成为它的上一扇等着被返回.
     * <p>本 Window 已在某个会话中时新窗口加入那个会话, 不在时两者组成一段新会话;
     * 因此无论本 Window 原本是否属于会话, 新窗口都可以经 {@link #back()} 回到这里.
     *
     * @param next 要打开的下一扇 Window, 必须与本 Window 属于同一名玩家
     * @return 打开后的 next; 玩家不可用或所在会话已结束等打不开的情况以 null 完成
     */
    @NotNull CompletableFuture<Window> navigate(@NotNull Window next);

    /**
     * 以本 Window 的查看者创建下一扇 Window 并打开它, 语义同 {@link #navigate(Window)}.
     * <p>此方法先同步调用 {@link Builder#build(Player)}, 因而同样受其实体线程约束;
     * 想把构建放到别的线程, 自己 build 完再用 {@link #navigate(CompletionStage)}.
     *
     * @param next 下一扇 Window 的 Builder
     * @return 打开后的 Window; 打不开时以 null 完成
     */
    @NotNull
    default CompletableFuture<Window> navigate(@NotNull Builder<?, ?> next) {
        return this.navigate(next.build(this.viewer()));
    }

    /**
     * 等待一扇还在构建中的 Window 完成, 再从本 Window 打开它, 语义同 {@link #navigate(Window)}.
     * <p>构建在哪个线程完成由调用方决定, 方法只负责把打开送进玩家的实体线程.
     *
     * @param next 构建中的下一扇 Window
     * @return 打开后的 Window; 打不开时以 null 完成
     */
    @NotNull
    default CompletableFuture<Window> navigate(@NotNull CompletionStage<? extends Window> next) {
        return next.thenCompose(this::navigate).toCompletableFuture();
    }

    /**
     * 回到上一扇, 上一扇以原实例重新打开.
     * <p>只有本 Window 是某个会话的当前窗且有上一扇时才发生返回; 位于根窗或不属于任何会话时
     * 不做任何事, 本 Window 保持打开.
     *
     * @return 返回后的新当前窗; 没有发生返回时以 null 完成
     */
    @NotNull CompletableFuture<Window> back();

    /**
     * 回到上一扇, 没有上一扇可回时关闭本 Window.
     * <p>有上一扇时等同 {@link #back()}; 位于根窗或不属于任何会话时等同 {@link #close()},
     * 根窗的关闭会照常结束所在会话. 通用"返回/关闭"按钮用这个.
     *
     * @return 返回后的新当前窗; 走了关闭路径时以 null 完成
     */
    @NotNull CompletableFuture<Window> backOrClose();

    /**
     * 本 Window 所属的会话.
     * <p>{@code build()} 后尚未打开时为 null; 经 {@link #open()} 直接打开时成为新根窗并在此刻创建会话;
     * 经 {@link #navigate} 被打开时归属上一扇所在的会话; 离开会话(栈弹出丢弃, 被会话外 Window 顶替, 会话结束)后回到 null.
     *
     * @return 所属会话, 不属于任何会话时为 null
     */
    @Nullable
    WindowSession session();

    /**
     * 随本 Window 携带的用户对象, 通常是构建它的菜单对象.
     * <p>库只负责保管, 从不读取其中内容; 引用的寿命与本 Window 在会话中的留存一致,
     * 因此 STACK 弹出即弃, RETAINED_STACK 与 TREE 保留到会话结束.
     *
     * @return 携带的对象, 未设置时为 null
     */
    @Nullable
    Object data();

    /**
     * 以给定类型读取携带的用户对象.
     *
     * @param type 期望类型
     * @return 携带的对象, 未设置时为 null
     * @throws ClassCastException 携带对象不是该类型时
     */
    @Nullable
    default <T> T data(@NotNull Class<T> type) {
        return type.cast(this.data());
    }

    /**
     * 请求关闭 Window.
     * closeable 只限制玩家主动关闭, 不限制插件命令.
     *
     * @return 关闭请求的执行结果
     */
    @NotNull CompletableFuture<CloseResult> close();

    /**
     * 设置动态标题来源并请求刷新.
     * Supplier 会在玩家实体线程读取, 且不得返回 null.
     *
     * @param titleSupplier 标题来源
     */
    void setTitleSupplier(@NotNull Supplier<? extends Component> titleSupplier);

    /**
     * 设置固定标题并请求刷新.
     *
     * @param title 新标题
     */
    void setTitle(@NotNull Component title);

    /**
     * 使用纯文本组件设置固定标题.
     *
     * @param title 新标题
     */
    default void setTitle(@NotNull String title) {
        this.setTitle(Component.text(title));
    }

    /**
     * 请求重新读取当前标题 Supplier.
     * 多次请求会在玩家实体 tick 中合并.
     */
    void updateTitle();

    /**
     * 当前菜单标题, 也就是最近一次已应用的标题快照.
     *
     * @return 当前标题
     */
    @NotNull Component title();

    /**
     * 设置是否接受客户端主动关闭.
     * 此设置不阻止插件, 断线或 Bukkit 外部关闭.
     *
     * @param closeable 是否可由客户端主动关闭
     */
    void setCloseable(boolean closeable);

    /**
     * 返回指定协议槽位是否被本 Window 冻结.
     * 只反映 {@link #frozenAt(int, boolean)} 设置的窗口侧状态, 路径上 Pane 自身的冻结不计入.
     *
     * @param windowSlot 协议槽位(raw slot)
     * @return 该槽位被本 Window 冻结时返回 true
     */
    boolean frozenAt(int windowSlot);

    /**
     * 设置指定协议槽位是否被本 Window 冻结.
     * 冻结后该槽位与路径经过已冻结 Pane 同待遇: 不参与点击语义, 不派发任何事件, 也不分派 Item 点击,
     * 客户端预测会被纠正回来, 显示内容不受影响.
     * 与 {@link Pane#setFrozen} 相互独立, 任一生效该槽位即被冻结, 本方法的解冻只撤销窗口侧的这一份.
     *
     * @param windowSlot 协议槽位(raw slot)
     * @param frozen true 表示冻结
     */
    void frozenAt(int windowSlot, boolean frozen);

    /**
     * 返回本 Window 是否冻结玩家副手交互.
     * <p>副手不属于 Window 的协议槽位, 因此不会出现在 {@link #frozenAt(int)} 或 Pane 路径中.
     * 本状态只阻止玩家经当前 Window 发起的副手交换, 不阻止插件或其他服务端逻辑直接修改副手.
     *
     * @return 副手交互被冻结时返回 true
     */
    boolean offhandFrozen();

    /**
     * 设置本 Window 是否冻结玩家副手交互.
     * <p>冻结后, 玩家在当前 Window 内按下副手交换键时不会改变被点击槽位或副手,
     * 也不会派发 Bukkit, Sparrow Inventory 或 Item 点击事件.
     *
     * @param frozen true 表示冻结副手交互
     */
    void offhandFrozen(boolean frozen);

    /**
     * 返回玩家主动关闭本窗口时, 所在会话是否返回上一扇.
     *
     * @return 玩家主动关闭时是否返回上一扇
     */
    boolean backOnPlayerClose();

    /**
     * 设置玩家主动关闭本窗口时, 所在会话是否返回上一扇. 默认 false.
     * <p>仅当本窗口是某个会话的当前窗时有意义, true 且存在上一扇时返回上一扇,
     * 否则会话以 PLAYER 原因结束. 不在任何会话中的窗口忽略此开关, 玩家关闭就是关闭.
     * <p>本开关只作用于玩家主动关闭(reason == PLAYER), 不影响程序化导航.
     *
     * @param backOnPlayerClose true 表示返回上一扇
     */
    void backOnPlayerClose(boolean backOnPlayerClose);

    /**
     * 替换打开后依次执行的处理器列表.
     *
     * @param openHandlers 新处理器列表
     */
    void setOpenHandlers(@NotNull List<? extends Runnable> openHandlers);

    /**
     * 当前打开处理器列表的快照.
     *
     * @return 不可变的处理器列表
     */
    @Unmodifiable
    @NotNull List<Runnable> getOpenHandlers();

    /**
     * 在现有打开处理器末尾追加一个处理器.
     *
     * @param openHandler 打开处理器
     */
    void addOpenHandler(@NotNull Runnable openHandler);

    /**
     * 移除一个与给定对象相等的打开处理器.
     *
     * @param openHandler 要移除的打开处理器
     */
    void removeOpenHandler(@NotNull Runnable openHandler);

    /**
     * 替换关闭后依次执行的处理器列表.
     *
     * @param closeHandlers 新处理器列表
     */
    void setCloseHandlers(@NotNull List<? extends Consumer<? super InventoryCloseEvent.Reason>> closeHandlers);

    /**
     * 当前打开处理器列表的快照.
     *
     * @return 不可变的处理器列表
     */
    @Unmodifiable
    @NotNull List<Consumer<InventoryCloseEvent.Reason>> getCloseHandlers();

    /**
     * 在现有关闭处理器末尾追加一个处理器.
     *
     * @param closeHandler 关闭处理器, 参数为关闭原因
     */
    void addCloseHandler(@NotNull Consumer<? super InventoryCloseEvent.Reason> closeHandler);

    /**
     * 绑定到指定的 Signal, Signal 将会持有本类的弱引用.
     * 当 Signal 被标脏时, 会触发传入的回调函数.
     * <p>绑定不补发当前值, 第一次回调发生在下一次标脏.
     * <p>绑定由本对象持有, 本对象被回收时一并消失,{@code callback} 捕获的对象随本对象一起释放.
     *
     * @param signal 数据源
     * @param callback 失效回调
     * @return 订阅凭证, 可用于提前解绑.
     */
    @NotNull
    Subscription bind(@NotNull Signal<?> signal, @NotNull Consumer<? super Window> callback);

    /**
     * 移除一个与给定对象相等的关闭处理器.
     *
     * @param closeHandler 要移除的关闭处理器
     */
    void removeCloseHandler(@NotNull Consumer<? super InventoryCloseEvent.Reason> closeHandler);

    /**
     * 替换容器外点击处理器列表.
     * 处理器可以取消 {@link WindowOutsideClick} 以阻止该次点击.
     *
     * @param outsideClickHandlers 新处理器列表
     */
    void setOutsideClickHandlers(@NotNull List<? extends Consumer<? super WindowOutsideClick>> outsideClickHandlers);

    /**
     * 当前容器外点击处理器列表的快照.
     *
     * @return 不可变的处理器列表
     */
    @Unmodifiable
    @NotNull List<Consumer<WindowOutsideClick>> getOutsideClickHandlers();

    /**
     * 在现有容器外点击处理器末尾追加一个处理器.
     *
     * @param outsideClickHandler 容器外点击处理器
     */
    void addOutsideClickHandler(@NotNull Consumer<? super WindowOutsideClick> outsideClickHandler);

    /**
     * 移除一个与给定对象相等的容器外点击处理器.
     *
     * @param outsideClickHandler 要移除的容器外点击处理器
     */
    void removeOutsideClickHandler(@NotNull Consumer<? super WindowOutsideClick> outsideClickHandler);

    /**
     * 设置服务器窗口状态, 并在已打开时发送 Ping 等待客户端确认.
     *
     * @param windowState 新服务器窗口状态
     */
    void setWindowState(int windowState);

    /**
     * 将服务器窗口状态加一, 并在已打开时等待客户端确认.
     */
    void incrementWindowState();

    /**
     * 返回最近一次设置的服务器窗口状态.
     *
     * @return 服务器窗口状态
     */
    int getServerWindowState();

    /**
     * 返回最近一次收到 Pong 确认的客户端窗口状态.
     *
     * @return 客户端已确认窗口状态
     */
    int getClientWindowState();

    /**
     * 替换客户端 Pong 确认窗口状态时依次执行的处理器列表.
     *
     * @param handlers 新处理器列表
     */
    void setWindowStateChangeHandlers(@NotNull List<? extends Consumer<? super Integer>> handlers);

    /**
     * 当前客户端 Pong 确认窗口状态的处理器列表的快照.
     *
     * @return 不可变的处理器列表
     */
    @Unmodifiable
    @NotNull List<Consumer<Integer>> getWindowStateChangeHandlers();

    /**
     * 在现有客户端 Pong 确认窗口状态处理器末尾追加一个处理器.
     *
     * @param handler 状态确认处理器
     */
    void addWindowStateChangeHandler(@NotNull Consumer<? super Integer> handler);

    /**
     * 移除一个与给定对象相等的客户端 Pong 确认窗口状态处理器.
     *
     * @param handler 要移除的状态确认处理器
     */
    void removeWindowStateChangeHandler(@NotNull Consumer<? super Integer> handler);

    /**
     * 返回本 Window 的槽位视觉配置: 两层视觉映射.
     * <p>同一 Window 始终返回同一个对象; 配置只影响本 Window 的查看者,
     * 并盖在显示路径的最外层, 先于沿途 Pane 与路径终点求值.
     *
     * @return 槽位视觉配置
     */
    @NotNull
    WindowVisual visual();

    /**
     * 返回当前的全局视觉映射.
     *
     * @return 全局视觉映射; 没有设置过时为 {@code null}, 表示按路径终点显示
     */
    @Nullable
    default Function<@Nullable ItemStack, @Nullable ItemProvider> visualizerProvider() {
        return this.visual().visualizerProvider();
    }

    /**
     * 设置 Window 全局视觉映射. 映射盖在本 Window 每条显示路径的最外层, 命中时沿途 Pane 与路径终点不再参与显示;
     * 输入是路径终点的同步可读内容, 约定见 {@link WindowVisual}. 返回 {@code null} 表示放行, 交给下一层.
     * <p>映射只改变本 Window 中的展示结果, 不影响槽位元素, 事务与点击语义; 光标不归它管.
     *
     * @param visualizerProvider 新的全局视觉映射, {@code null} 表示不参与这一层
     */
    default void setVisualizerProvider(@Nullable Function<@Nullable ItemStack, @Nullable ItemProvider> visualizerProvider) {
        this.visual().setVisualizerProvider(visualizerProvider);
    }

    /**
     * 设置 Window 全局视觉映射, 并指定提供器给出结果前显示的占位.
     * <p>约定与 {@link #setVisualizerProvider(Function)} 相同; 提供器当场算得出结果时首帧就是真值, 用不到占位.
     *
     * @param visualizerProvider 新的全局视觉映射, {@code null} 表示不参与这一层
     * @param placeholder 首次成功结果前显示的占位, {@code null} 表示终点连接 Inventory 时显示该槽真实内容, 其余终点显示空
     */
    default void setVisualizerProvider(@Nullable Function<@Nullable Item, @Nullable ItemProvider> visualizerProvider, @Nullable ImmediateItemProvider placeholder) {
        this.visual().setVisualizerProvider(visualizerProvider, placeholder);
    }

    /**
     * 使用直接返回 ItemStack 的映射设置 Window 全局视觉映射.
     * <p>约定与 {@link #setVisualizerProvider(Function)} 相同.
     *
     * @param visualizer 新的全局物品映射, {@code null} 表示不参与这一层
     */
    default void setVisualizerItem(@Nullable Function<@Nullable Item, @Nullable Item> visualizer) {
        this.visual().setVisualizerItem(visualizer);
    }

    /**
     * 返回一个 Window 槽位的显式视觉映射; 不含回退到的全局映射.
     *
     * @param windowSlot Window 槽位
     * @return 该槽的逐槽视觉映射; 没有覆盖时为 {@code null}, 表示这个槽用的是全局映射
     * @throws IndexOutOfBoundsException 当槽号越界时
     */
    @Nullable
    default Function<@Nullable ItemStack, @Nullable ItemProvider> visualizerProvider(int windowSlot) {
        return this.visual().visualizerProvider(windowSlot);
    }

    /**
     * 替换一个 Window 槽位的逐槽视觉映射, 它是整条显示路径层级最高的一层:
     * 返回非 {@code null} 结果直接采用, 返回 {@code null} 表示放行, 继续询问全局映射.
     * 传入 {@code null} 会移除这一层, 使该槽直接从全局映射开始.
     * <p>映射的输入输出约定与 {@link #setVisualizerProvider(Function)} 相同.
     *
     * @param windowSlot Window 槽位
     * @param visualizerProvider 新的逐槽视觉映射, {@code null} 表示移除这一层
     * @throws IndexOutOfBoundsException 当槽号越界时
     */
    default void setVisualizerProvider(int windowSlot, @Nullable Function<@Nullable ItemStack, @Nullable ItemProvider> visualizerProvider) {
        this.visual().setVisualizerProvider(windowSlot, visualizerProvider);
    }

    /**
     * 替换一个 Window 槽位的逐槽视觉映射, 并指定提供器给出结果前显示的占位.
     * <p>约定与 {@link #setVisualizerProvider(int, Function)} 相同.
     *
     * @param windowSlot Window 槽位
     * @param visualizerProvider 新的逐槽视觉映射, {@code null} 表示移除这一层
     * @param placeholder 首次成功结果前显示的占位, {@code null} 表示终点连接 Inventory 时显示该槽真实内容, 其余终点显示空
     * @throws IndexOutOfBoundsException 当槽号越界时
     */
    default void setVisualizerProvider(int windowSlot, @Nullable Function<@Nullable ItemStack, @Nullable ItemProvider> visualizerProvider, @Nullable ImmediateItemProvider placeholder) {
        this.visual().setVisualizerProvider(windowSlot, visualizerProvider, placeholder);
    }

    /**
     * 使用直接返回 ItemStack 的映射替换一个 Window 槽位的逐槽视觉映射.
     * 映射返回 {@code null} 表示放行; 返回空 ItemStack 表示覆盖为空视觉.
     *
     * @param windowSlot Window 槽位
     * @param visualizer 新的逐槽物品映射, {@code null} 表示移除这一层
     * @throws IndexOutOfBoundsException 当槽号越界时
     */
    default void setVisualizerItem(int windowSlot, @Nullable Function<@Nullable ItemStack, @Nullable ItemStack> visualizer) {
        this.visual().setVisualizerItem(windowSlot, visualizer);
    }

    /**
     * 返回只控制客户端光标的视觉配置与失效范围.
     * <p>同一 Window 始终返回同一个对象; 标脏不会影响 Window 槽位或请求全量同步.
     *
     * @return 光标视觉配置与失效范围
     */
    @NotNull
    CursorVisual cursorVisual();

    /**
     * 返回当前的光标视觉映射.
     *
     * @return 光标视觉映射, 没有设置过时为 {@code null}, 表示按菜单实际光标显示
     */
    @Nullable
    Function<@Nullable ItemStack, @Nullable ItemProvider> cursorVisualizerProvider();

    /**
     * 设置光标视觉映射.
     * <p>参数为实际光标副本, 空光标以 null 表示; 返回 null 时保留实际光标显示.
     * 映射本身在渲染线程求值, 只挑出这次用哪个提供器, 重活放进返回的提供器里;
     * 提供器给出结果之前显示菜单实际光标, 光标内容变化会作废尚未完成的计算与已完成的结果.
     *
     * @param cursorVisualizerProvider 光标视觉映射, {@code null} 表示移除这一层
     */
    void setCursorVisualizerProvider(@Nullable Function<@Nullable ItemStack, @Nullable ItemProvider> cursorVisualizerProvider);

    /**
     * 设置光标视觉映射, 并指定提供器给出结果前显示的占位.
     *
     * @param cursorVisualizerProvider 光标视觉映射, {@code null} 表示移除这一层
     * @param placeholder 首次成功结果前显示的占位, {@code null} 表示显示菜单实际光标
     */
    default void setCursorVisualizerProvider(@Nullable Function<@Nullable ItemStack, @Nullable ItemProvider> cursorVisualizerProvider, @Nullable ImmediateItemProvider placeholder) {
        this.cursorVisual().setVisualizerProvider(cursorVisualizerProvider, placeholder);
    }

    /**
     * 使用直接返回 ItemStack 的映射设置光标视觉映射.
     * 参数为实际光标副本, 空光标以 null 表示; 返回 null 时保留实际光标显示.
     *
     * @param cursorVisualizer 光标物品映射, {@code null} 表示移除这一层
     */
    default void setCursorVisualizerItem(@Nullable Function<@Nullable ItemStack, @Nullable ItemStack> cursorVisualizer) {
        this.cursorVisual().setVisualizerItem(cursorVisualizer);
    }

    /**
     * 通知 Window 对指定槽位的显示内容进行更新.
     * <p>通知可以来自任意线程; 实际渲染和协议同步会合并到玩家实体 tick.
     *
     * @param windowSlot Window 槽位
     */
    void notifyUpdate(int windowSlot);

    /**
     * 通知 Window 进行一次强制全量更新.
     */
    void notifyUpdateAll();

    /**
     * 读取某个 Window 槽位最近一次推给客户端的显示内容.
     * <p>返回的是渲染缓存的副本, 不会触发重新渲染. 槽位越界或尚未渲染时返回空物品.
     *
     * @param windowSlot Window 槽位
     * @return 显示内容的副本, 没有内容时为空物品
     */
    @NotNull
    Item displayedAt(int windowSlot);

    /**
     * 此 Window 的所属玩家.
     *
     * @return 查看者
     */
    @NotNull Player viewer();

    /**
     * Window 当前是否打开.
     *
     * @return 是否打开
     */
    boolean isOpen();

    /**
     * 返回是否接受客户端主动发送的关闭请求.
     *
     * @return 是否接受客户端主动关闭
     */
    boolean isCloseable();

    /**
     * 返回占据玩家物品栏区域的下部 Pane.
     * <p>合并窗口的 upper 与 lower 区域会返回同一个根 Pane.
     *
     * @return 下部 Pane
     */
    @NotNull
    Pane lowerPane();

    /**
     * 返回下部 Pane 按默认布局引用的 ReferencingInventory.
     *
     * @return 默认 ReferencingInventory
     */
    @Nullable
    default ReferencingInventory defaultLowerInventory() {
        Pane lowerPane = this.lowerPane();
        if (lowerPane.width() != 9 || lowerPane.height() != 4) {
            return null;
        }
        // 默认 lower 由首槽标识; 只有需要区分同形自定义 Pane 时才记录构建来源.
        return lowerPane.element(0) instanceof Element.InventoryLink(ReferencingInventory inventory, int slot) && slot == 0
                ? inventory
                : null;
    }

    /**
     * 返回 Window 直接拥有的根 Pane, 不包含嵌套 Pane.
     *
     * @return 根 Pane 列表
     */
    @Unmodifiable
    @NotNull List<Pane> panes();

    /**
     * 返回 Window 槽位对应的根 Pane 链接.
     *
     * @param windowSlot Window 槽位
     * @return 根 Pane 链接
     */
    @NotNull
    Element.PaneLink paneAt(int windowSlot);

    /**
     * 返回玩家快捷栏槽位对应的根 Pane 链接.
     *
     * @param hotbarSlot 快捷栏索引
     * @return 根 Pane 链接
     */
    @NotNull
    Element.PaneLink paneAtHotbar(int hotbarSlot);

    /**
     * 返回玩家快捷栏槽位对应的协议槽位.
     *
     * @param hotbarSlot 快捷栏索引(0-8)
     * @return 对应的协议槽位(raw slot)
     * @throws IndexOutOfBoundsException 快捷栏索引超出 0-8 时
     */
    int windowSlotAtHotbar(int hotbarSlot);

    /**
     * 打开请求的执行结果.
     */
    enum OpenResult {
        OPENED,
        ALREADY_OPEN,
        VIEWER_UNAVAILABLE
    }

    /**
     * 关闭请求的执行结果.
     */
    enum CloseResult {
        CLOSED,
        ALREADY_CLOSED
    }

    /**
     * 可重复使用的类型化 Window Builder.
     *
     * @param <W> 创建的 Window 类型
     * @param <B> 具体 Builder 类型
     */
    interface Builder<W extends Window, B extends Builder<W, B>> extends Cloneable {

        /**
         * 设置 {@link #build()} 使用的玩家.
         *
         * @param viewer 查看者
         * @return 此 Builder
         */
        @NotNull B setViewer(@NotNull Player viewer);

        /**
         * 设置动态标题来源.
         *
         * @param titleSupplier 标题来源
         * @return 此 Builder
         */
        @NotNull B setTitleSupplier(@NotNull Supplier<? extends Component> titleSupplier);

        /**
         * 设置固定标题.
         *
         * @param title 标题
         * @return 此 Builder
         */
        @NotNull B setTitle(@NotNull Component title);

        /**
         * 使用纯文本组件设置固定标题.
         *
         * @param title 标题
         * @return 此 Builder
         */
        default @NotNull B setTitle(@NotNull String title) {
            return this.setTitle(Component.text(title));
        }

        /**
         * 设置是否接受客户端主动关闭.
         *
         * @param closeable 是否可由客户端主动关闭
         * @return 此 Builder
         */
        @NotNull B setCloseable(boolean closeable);

        /**
         * 替换打开后依次执行的处理器列表.
         *
         * @param openHandlers 打开处理器
         * @return 此 Builder
         */
        @NotNull B setOpenHandlers(@NotNull List<? extends Runnable> openHandlers);

        /**
         * 追加一个打开处理器.
         *
         * @param openHandler 打开处理器
         * @return 此 Builder
         */
        @NotNull B addOpenHandler(@NotNull Runnable openHandler);

        /**
         * 替换关闭后依次执行的处理器列表.
         *
         * @param closeHandlers 关闭处理器
         * @return 此 Builder
         */
        @NotNull B setCloseHandlers(
                @NotNull List<? extends Consumer<? super InventoryCloseEvent.Reason>> closeHandlers
        );

        /**
         * 追加一个关闭处理器.
         *
         * @param closeHandler 关闭处理器
         * @return 此 Builder
         */
        @NotNull B addCloseHandler(@NotNull Consumer<? super InventoryCloseEvent.Reason> closeHandler);

        /**
         * 替换容器外点击处理器列表. 每个处理器同时接收本 Builder 创建的具体 Window.
         *
         * @param outsideClickHandlers 容器外点击处理器
         * @return 此 Builder
         */
        @NotNull
        B setOutsideClickHandlers(
                @NotNull List<? extends BiConsumer<? super W, ? super WindowOutsideClick>> outsideClickHandlers
        );

        /**
         * 追加一个容器外点击处理器. 处理器同时接收本 Builder 创建的具体 Window.
         *
         * @param outsideClickHandler 容器外点击处理器
         * @return 此 Builder
         */
        @NotNull
        default B addOutsideClickHandler(@NotNull BiConsumer<? super W, ? super WindowOutsideClick> outsideClickHandler) {
            return this.addModifier(window -> window.addOutsideClickHandler(click -> outsideClickHandler.accept(window, click)));
        }

        /**
         * 追加一个只接收点击上下文的容器外点击处理器.
         *
         * @param outsideClickHandler 容器外点击处理器
         * @return 此 Builder
         */
        @NotNull
        B addOutsideClickHandler(@NotNull Consumer<? super WindowOutsideClick> outsideClickHandler);

        /**
         * 设置玩家主动关闭时是否返回上一扇, 默认 false.
         * 语义同 {@link Window#backOnPlayerClose(boolean)}.
         *
         * @param backOnPlayerClose true 表示返回上一扇
         * @return 此 Builder
         */
        @NotNull B setBackOnPlayerClose(boolean backOnPlayerClose);

        /**
         * 设置随 Window 携带的用户对象, 语义见 {@link Window#data()}.
         *
         * @param data 携带的对象
         * @return 此 Builder
         */
        @NotNull B setData(@NotNull Object data);

        /**
         * 设置本 Window 成为根窗时新会话的类型, 默认 {@link WindowSession.Kind#STACK}.
         * 本 Window 经 {@link Window#navigate} 接入既有会话时此声明不生效.
         *
         * @param kind 会话类型
         * @return 此 Builder
         */
        @NotNull B setSessionKind(@NotNull WindowSession.Kind kind);

        /**
         * 追加一个会话结束处理器: 本 Window 成为根窗时装进新会话, 整段交互结束时恰好触发一次.
         * 本 Window 经 {@link Window#navigate} 接入既有会话时此声明不生效.
         *
         * @param handler 结束处理器, 参数为结束原因
         * @return 此 Builder
         */
        @NotNull B addSessionEndHandler(@NotNull Consumer<? super InventoryCloseEvent.Reason> handler);

        /**
         * 设置初始服务器窗口状态.
         *
         * @param windowState 初始状态
         * @return 此 Builder
         */
        @NotNull B setWindowState(int windowState);

        /**
         * 替换客户端状态确认处理器列表.
         *
         * @param handlers 状态确认处理器
         * @return 此 Builder
         */
        @NotNull B setWindowStateChangeHandlers(
                @NotNull List<? extends Consumer<? super Integer>> handlers
        );

        /**
         * 追加一个客户端状态确认处理器.
         *
         * @param handler 状态确认处理器
         * @return 此 Builder
         */
        @NotNull B addWindowStateChangeHandler(@NotNull Consumer<? super Integer> handler);

        /**
         * 设置 Window 全局视觉映射, 提供器给出结果前显示该槽真实内容.
         * <p>约定与 {@link Window#setVisualizerProvider(Function)} 相同, 打开前即已生效.
         *
         * @param visualizerProvider 全局视觉映射, {@code null} 表示不设置这一层
         * @return 此 Builder
         */
        @NotNull
        default B setVisualizerProvider(@Nullable Function<@Nullable ItemStack, @Nullable ItemProvider> visualizerProvider) {
            return this.setVisualizerProvider(visualizerProvider, null);
        }

        /**
         * 设置 Window 全局视觉映射, 并指定提供器给出结果前显示的占位.
         *
         * @param visualizerProvider 全局视觉映射, {@code null} 表示不设置这一层
         * @param placeholder 首次成功结果前显示的占位, {@code null} 表示终点连接 Inventory 时显示该槽真实内容, 其余终点显示空
         * @return 此 Builder
         */
        @NotNull
        B setVisualizerProvider(
                @Nullable Function<@Nullable ItemStack, @Nullable ItemProvider> visualizerProvider,
                @Nullable ImmediateItemProvider placeholder
        );

        /**
         * 使用直接返回 ItemStack 的映射设置 Window 全局视觉映射.
         *
         * @param visualizer 全局物品映射, {@code null} 表示不设置这一层
         * @return 此 Builder
         */
        @NotNull
        default B setVisualizerItem(@Nullable Function<@Nullable ItemStack, @Nullable ItemStack> visualizer) {
            return this.setVisualizerProvider(VisualLayer.itemVisualizer(visualizer));
        }

        /**
         * 设置光标视觉映射, 提供器给出结果前显示菜单实际光标.
         *
         * @param cursorVisualizerProvider 光标视觉映射, {@code null} 表示不设置这一层
         * @return 此 Builder
         */
        @NotNull
        default B setCursorVisualizerProvider(@Nullable Function<@Nullable ItemStack, @Nullable ItemProvider> cursorVisualizerProvider) {
            return this.setCursorVisualizerProvider(cursorVisualizerProvider, null);
        }

        /**
         * 设置光标视觉映射, 并指定提供器给出结果前显示的占位.
         *
         * @param cursorVisualizerProvider 光标视觉映射, {@code null} 表示不设置这一层
         * @param placeholder 首次成功结果前显示的占位, {@code null} 表示显示菜单实际光标
         * @return 此 Builder
         */
        @NotNull
        B setCursorVisualizerProvider(
                @Nullable Function<@Nullable ItemStack, @Nullable ItemProvider> cursorVisualizerProvider,
                @Nullable ImmediateItemProvider placeholder
        );

        /**
         * 使用直接返回 ItemStack 的映射设置光标视觉映射.
         * 参数为实际光标副本, 空光标以 null 表示.
         *
         * @param cursorVisualizer 光标物品映射, {@code null} 表示不设置这一层
         * @return 此 Builder
         */
        @NotNull
        default B setCursorVisualizerItem(@Nullable Function<@Nullable ItemStack, @Nullable ItemStack> cursorVisualizer) {
            return this.setCursorVisualizerProvider(VisualLayer.itemVisualizer(cursorVisualizer));
        }

        /**
         * 替换创建完成后依次执行的 Window 修改器列表.
         *
         * @param modifiers Window 修改器
         * @return 此 Builder
         */
        @NotNull B setModifiers(@NotNull List<? extends Consumer<? super W>> modifiers);

        /**
         * 追加一个创建完成后执行的 Window 修改器.
         *
         * @param modifier Window 修改器
         * @return 此 Builder
         */
        @NotNull B addModifier(@NotNull Consumer<? super W> modifier);

        /**
         * 创建独立的 Builder 副本.
         * 可变处理器列表会被复制, 已引用的 Pane 与函数对象保持复用.
         *
         * @return Builder 副本
         */
        @NotNull B clone();

        /**
         * 使用已设置的查看者创建 Window.
         * <p>若未显式设置 lower Pane, 此调用会同步读取查看者的 Bukkit 背包来创建
         * {@link ReferencingInventory}. 调用方必须保证当前线程可以合法访问该背包;
         *
         * @return 新的未打开 Window
         * @throws IllegalStateException 未设置查看者时抛出
         */
        @NotNull W build();

        /**
         * 为指定查看者创建 Window.
         * <p>若未显式设置 lower Pane, 此调用会同步读取查看者的 Bukkit 背包来创建
         * {@link ReferencingInventory}. 调用方必须保证当前线程可以合法访问该背包;
         *
         * @param viewer 查看者
         * @return 新的未打开 Window
         */
        @NotNull W build(@NotNull Player viewer);

        /**
         * 为指定查看者创建并请求打开 Window.
         * <p>此方法先同步调用 {@link #build(Player)}, 因而同样受其 Bukkit 背包线程约束.
         *
         * @param viewer 查看者
         * @return 打开请求的执行结果
         */
        default @NotNull CompletableFuture<OpenResult> open(@NotNull Player viewer) {
            return this.build(viewer).open();
        }

    }
}
