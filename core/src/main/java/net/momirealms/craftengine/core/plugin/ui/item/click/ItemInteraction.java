package net.momirealms.craftengine.core.plugin.ui.item.click;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.ui.window.Window;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ItemInteraction {

    /**
     * 返回发起交互的玩家.
     *
     * @return 玩家
     */
    @NotNull
    Player player();

    /**
     * 返回交互所属的 Window.
     *
     * @return Window
     */
    @NotNull
    Window window();

    /**
     * 返回交互发生的 Window 槽位.
     *
     * @return Window 槽位
     */
    int windowSlot();

    /**
     * 返回当前槽位最近一次渲染记录的数据.
     *
     * @param <T> 调用方期望的类型
     * @return 记录的数据, 没有时为 {@code null}
     */
    @Nullable
    @SuppressWarnings("unchecked")
    default <T> T remembered() {
        return (T) this.window().rememberedAt(this.windowSlot());
    }
}
