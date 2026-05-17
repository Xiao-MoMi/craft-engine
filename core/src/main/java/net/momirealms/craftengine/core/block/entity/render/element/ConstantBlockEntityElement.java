package net.momirealms.craftengine.core.block.entity.render.element;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.PlayerContext;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public interface ConstantBlockEntityElement extends BlockEntityElement {

    @NotNull
    default Predicate<PlayerContext> condition() {
        return context -> true;
    }

    default boolean hasCondition() {
        return false;
    }

    default boolean canSee(PlayerContext context) {
        if (hasCondition()) {
            return condition().test(context);
        } else {
            return true;
        }
    }

    @Override
    default void show(@NotNull Player player) {
        if (hasCondition()) {
            PlayerOptionalContext context = PlayerOptionalContext.of(player);
            if (condition().test(context)) {
                showInternal(player);
            }
        } else {
            showInternal(player);
        }
    }

    default boolean supportsTransform() {
        return false;
    }

    void showInternal(Player player);
}
