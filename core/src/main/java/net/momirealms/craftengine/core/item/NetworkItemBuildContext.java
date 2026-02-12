package net.momirealms.craftengine.core.item;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.text.minimessage.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class NetworkItemBuildContext extends ItemBuildContext {

    public NetworkItemBuildContext(@Nullable Player player, @NotNull ContextHolder contexts) {
        super(player, contexts);
    }

    @NotNull
    public static NetworkItemBuildContext empty() {
        return new NetworkItemBuildContext(null, ContextHolder.empty());
    }

    @NotNull
    public static NetworkItemBuildContext of(@Nullable Player player, @NotNull ContextHolder contexts) {
        return new NetworkItemBuildContext(player, contexts);
    }

    @NotNull
    public static NetworkItemBuildContext of(@Nullable Player player, @NotNull ContextHolder.Builder builder) {
        if (player != null) builder.withParameter(DirectContextParameters.PLAYER, player);
        return new NetworkItemBuildContext(player, builder.build());
    }

    @NotNull
    public static NetworkItemBuildContext of(@Nullable Player player) {
        if (player == null) return new NetworkItemBuildContext(null, ContextHolder.empty());
        return new NetworkItemBuildContext(player, new ContextHolder(Map.of(DirectContextParameters.PLAYER, () -> player)));
    }

    @NotNull
    protected TagResolver[] getInternalTagResolvers() {
        return new TagResolver[]{ShiftTag.INSTANCE, ImageTag.INSTANCE, I18NTag.INSTANCE, new NetworkL10NTag(this), new NamedArgumentTag(this),
                new PlaceholderTag(this), ExpressionTag.INSTANCE, GlobalVariableTag.INSTANCE};
    }
}
