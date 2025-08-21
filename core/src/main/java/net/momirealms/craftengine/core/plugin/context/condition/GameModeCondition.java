package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.entity.player.GameMode;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class GameModeCondition<CTX extends Context> implements Condition<CTX> {
    private final GameMode gameMode;

    public GameModeCondition(GameMode gamemode) {
        this.gameMode = gamemode;
    }

    @Override
    public Key type() {
        return CommonConditions.GAMEMODE;
    }

    @Override
    public boolean test(CTX ctx) {
        Optional<Player> player = ctx.getOptionalParameter(DirectContextParameters.PLAYER);
        if (player.isPresent()) {
            GameMode gameMode = player.get().gameMode();
            return gameMode == this.gameMode;
        }
        return false;
    }

    public static class FactoryImpl<CTX extends Context> implements ConditionFactory<CTX> {

        @Override
        public Condition<CTX> create(Map<String, Object> arguments) {
            String gameMode = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("gamemode"), "warning.config.condition.gamemode.missing_gamemode");
            try {
                return new GameModeCondition<>(GameMode.valueOf(gameMode.toUpperCase(Locale.ENGLISH)));
            } catch (IllegalArgumentException e) {
                throw new LocalizedResourceConfigException("warning.config.condition.gamemode.invalid_gamemode", gameMode);
            }
        }
    }
}
