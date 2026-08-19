package net.momirealms.craftengine.core.plugin.ui.item.provider;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.ui.window.Window;
import org.jetbrains.annotations.NotNull;

public final class RenderContext {
    private final Player player;  // 查看者, 取自所属 Window
    private final Window window;  // 所属 Window
    private final int windowSlot; // 最终槽位编号, -1 表示客户端光标

    // 为 Window 的最终槽位创建稳定的渲染上下文.
    public RenderContext(@NotNull Window window, int windowSlot) {
        this(window, windowSlot, false);
    }

    // 创建用于渲染 Window 光标可视内容的上下文.
    @NotNull
    public static RenderContext cursor(@NotNull Window window) {
        return new RenderContext(window, -1, true);
    }

    private RenderContext(@NotNull Window window, int windowSlot, boolean cursor) {
        // 普通槽位必须非负, 只有光标上下文允许使用 -1
        if (windowSlot < 0 && !cursor)
            throw new IllegalArgumentException("windowSlot must be non-negative");

        this.window = window;
        this.windowSlot = windowSlot;
        this.player = window.viewer();
    }

    @NotNull
    public Player player() {
        return this.player;
    }

    @NotNull
    public Window window() {
        return this.window;
    }

    public int windowSlot() {
        return this.windowSlot;
    }

    public boolean isCursor() {
        return this.windowSlot == -1;
    }
}
