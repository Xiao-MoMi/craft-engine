package net.momirealms.craftengine.core.plugin.context;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.text.minimessage.*;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class NetworkTextReplaceContext extends PlayerOptionalContext implements PlayerContext {

    public NetworkTextReplaceContext(Player player) {
        super(player, new ContextHolder(Map.of(DirectContextParameters.PLAYER, () -> player)));
    }

    public static @NotNull NetworkTextReplaceContext of(Player player) {
        return new NetworkTextReplaceContext(player);
    }

    @Override
    public Player player() {
        return super.player;
    }

    @NotNull
    protected TagResolver[] getInternalTagResolvers() {
        return new TagResolver[]{ShiftTag.INSTANCE, ImageTag.INSTANCE, I18NTag.INSTANCE, new NetworkL10NTag(this), new NamedArgumentTag(this),
                new PlaceholderTag(this), ExpressionTag.INSTANCE, GlobalVariableTag.INSTANCE};
    }
}
