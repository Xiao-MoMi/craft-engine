package net.momirealms.craftengine.core.plugin.context.parameter;

import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.ChainParameterProvider;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.util.MCUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class PlayerParameterProvider implements ChainParameterProvider<Player> {
    private static final Map<ContextKey<?>, Function<Player, Object>> CONTEXT_FUNCTIONS = new HashMap<>();
    static {
        CONTEXT_FUNCTIONS.put(DirectContextParameters.X, Entity::x);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.Y, Entity::y);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.Z, Entity::z);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.PITCH, Entity::xRot);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.YAW, Entity::yRot);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.POSITION, Entity::position);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_X, p -> MCUtils.fastFloor(p.x()));
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_Y, p -> MCUtils.fastFloor(p.y()));
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_Z, p -> MCUtils.fastFloor(p.z()));
        CONTEXT_FUNCTIONS.put(DirectContextParameters.FOOD, Player::foodLevel);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.SATURATION, Player::saturation);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.NAME, Player::name);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.UUID, Player::uuid);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.WORLD, Entity::world);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.IS_SNEAKING, Player::isSneaking);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.IS_SWIMMING, Player::isSwimming);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.IS_CLIMBING, Player::isClimbing);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.IS_GLIDING, Player::isGliding);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.IS_FLYING, Player::isFlying);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.GAMEMODE, Player::gameMode);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.MAIN_HAND_ITEM, p -> p.getItemInHand(InteractionHand.MAIN_HAND));
        CONTEXT_FUNCTIONS.put(DirectContextParameters.OFF_HAND_ITEM, p -> p.getItemInHand(InteractionHand.OFF_HAND));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter, Player player) {
        return (Optional<T>) Optional.ofNullable(CONTEXT_FUNCTIONS.get(parameter)).map(f -> f.apply(player));
    }
}