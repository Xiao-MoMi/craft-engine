package net.momirealms.craftengine.core.plugin.ui.item.provider;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.ui.window.Window;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class RenderContext {
    /** 本次渲染所属的 Window. */
    public final Window window;
    /** 本次渲染的目标类型. */
    public final Kind kind;
    /** 最终 Window 槽位, 光标与非槽位内容固定为 {@code -1}. */
    public final int windowSlot;
    @Nullable
    private final Consumer<Object> memo;

    public RenderContext(@NotNull Window window, int windowSlot) {
        this(window, Kind.WINDOW_SLOT, windowSlot, null);
    }

    @ApiStatus.Internal
    public RenderContext(@NotNull Window window, int windowSlot, @NotNull Consumer<Object> memo) {
        this(window, Kind.WINDOW_SLOT, windowSlot, memo);
    }

    /**
     * 创建用于渲染 Window 光标内容的上下文.
     *
     * @param window 所属 Window
     * @return 光标渲染上下文
     */
    @NotNull
    public static RenderContext cursor(@NotNull Window window) {
        return new RenderContext(window, Kind.CURSOR, -1, null);
    }

    /**
     * 创建用于渲染非槽位内容的上下文, 例如商人交易列表.
     *
     * @param window 所属 Window
     * @return 非槽位渲染上下文
     */
    @NotNull
    public static RenderContext offSlot(@NotNull Window window) {
        return new RenderContext(window, Kind.OFF_SLOT, -1, null);
    }

    private RenderContext(@NotNull Window window, @NotNull Kind kind, int windowSlot, @Nullable Consumer<Object> memo) {
        if (windowSlot < 0 && kind == Kind.WINDOW_SLOT) {
            throw new IllegalArgumentException("windowSlot must be non-negative");
        }

        this.window = window;
        this.kind = kind;
        this.windowSlot = windowSlot;
        this.memo = memo;
    }

    /**
     * 返回当前 Window 的查看者.
     *
     * @return 查看者
     */
    @NotNull
    public Player player() {
        return this.window.viewer();
    }

    /**
     * 同步记录当前 Window 槽位的交互数据, 后续调用覆盖前值, {@code null} 表示清除.
     * <p><strong>必须在本次渲染回调返回前调用.</strong> 光标与非槽位上下文没有记录目标.
     *
     * @param value 要记录的数据, {@code null} 表示清除
     */
    public void remember(@Nullable Object value) {
        if (this.memo != null) {
            this.memo.accept(value);
        }
    }

    public enum Kind {
        WINDOW_SLOT,
        CURSOR,
        OFF_SLOT
    }
}
