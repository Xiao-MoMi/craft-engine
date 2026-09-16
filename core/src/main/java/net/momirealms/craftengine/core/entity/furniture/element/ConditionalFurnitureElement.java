package net.momirealms.craftengine.core.entity.furniture.element;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.PlayerContext;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public interface ConditionalFurnitureElement extends FurnitureElement {
    // 未配置显示条件的元素共享此标记，展示时无需构造条件上下文。
    Predicate<PlayerContext> ALWAYS_VISIBLE = context -> true;

    @NotNull
    Predicate<PlayerContext> condition();

    @NotNull
    Furniture furniture();

    @Override
    default boolean canSee(Player player) {
        Predicate<PlayerContext> condition = condition();
        if (condition == ALWAYS_VISIBLE) return true;
        // 首次展示和变体切换使用相同的参数，条件都可以访问当前玩家及家具。
        PlayerOptionalContext context = PlayerOptionalContext.of(player, ContextHolder.builder(DirectContextParameters.PLAYER, player, DirectContextParameters.FURNITURE, furniture()).build());
        return condition.test(context);
    }

    @Override
    default void show(Player player) {
        if (canSee(player)) {
            showInternal(player);
        }
    }

    void showInternal(Player player);
}
