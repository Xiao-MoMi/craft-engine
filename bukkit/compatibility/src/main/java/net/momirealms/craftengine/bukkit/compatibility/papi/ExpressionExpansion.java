package net.momirealms.craftengine.bukkit.compatibility.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.text.minimessage.ExpressionTag;
import net.momirealms.craftengine.core.util.StringUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ExpressionExpansion extends PlaceholderExpansion {
    private static final String MODULO_ESCAPE = "{mod}";

    @Override
    public @NotNull String getIdentifier() {
        return "ceexpr";
    }

    @Override
    public @NotNull String getAuthor() {
        return "XiaoMoMi";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        Player player = offlinePlayer == null ? null : offlinePlayer.getPlayer();
        return resolve(player, params);
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        return resolve(player, params);
    }

    private static @Nullable String resolve(@Nullable Player player, String params) {
        String[] split = StringUtils.split(params, '_', 2);
        if (split.length != 2 || split[0].isEmpty() || split[1].isEmpty()) {
            return null;
        }
        split[1] = split[1].replace(MODULO_ESCAPE, "%");
        PlayerOptionalContext context = player == null
                ? PlayerOptionalContext.empty()
                : PlayerOptionalContext.of(BukkitAdaptor.adapt(player));
        try {
            return ExpressionTag.INSTANCE.resolve(split, context);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
