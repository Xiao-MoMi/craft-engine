package net.momirealms.craftengine.core.plugin.context.parameter;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.player.GameMode;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.util.Cancellable;
import net.momirealms.craftengine.core.util.GameEdition;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.ExistingBlock;
import net.momirealms.craftengine.core.world.Position;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.UUID;

public final class DirectContextParameters {
    private DirectContextParameters() {}

    public static final ContextKey<Double> RANDOM = ContextKey.direct("random");
    public static final ContextKey<Double> LAST_RANDOM = ContextKey.direct("last_random");
    public static final ContextKey<World> WORLD = ContextKey.direct("world");
    public static final ContextKey<Item<?>> FURNITURE_ITEM = ContextKey.direct("furniture_item");
    public static final ContextKey<Item<?>> ITEM_IN_HAND = ContextKey.direct("item_in_hand");
    public static final ContextKey<Boolean> FALLING_BLOCK = ContextKey.direct("falling_block");
    public static final ContextKey<Float> EXPLOSION_RADIUS = ContextKey.direct("explosion_radius");
    public static final ContextKey<Player> PLAYER = ContextKey.direct("player");
    public static final ContextKey<Entity> ENTITY = ContextKey.direct("entity");
    public static final ContextKey<ImmutableBlockState> CUSTOM_BLOCK_STATE = ContextKey.direct("custom_block_state");
    public static final ContextKey<Position> COORDINATE = ContextKey.direct("coordinate");
    public static final ContextKey<WorldPosition> POSITION = ContextKey.direct("position");
    public static final ContextKey<String> NAME = ContextKey.direct("name");
    public static final ContextKey<Double> X = ContextKey.direct("x");
    public static final ContextKey<Double> Y = ContextKey.direct("y");
    public static final ContextKey<Double> Z = ContextKey.direct("z");
    public static final ContextKey<Float> YAW = ContextKey.direct("yaw");
    public static final ContextKey<Float> PITCH = ContextKey.direct("pitch");
    public static final ContextKey<Integer> BLOCK_X = ContextKey.direct("block_x");
    public static final ContextKey<Integer> BLOCK_Y = ContextKey.direct("block_y");
    public static final ContextKey<Integer> BLOCK_Z = ContextKey.direct("block_z");
    public static final ContextKey<Integer> FOOD = ContextKey.direct("food");
    public static final ContextKey<Float> SATURATION = ContextKey.direct("saturation");
    public static final ContextKey<UUID> UUID = ContextKey.direct("uuid");
    public static final ContextKey<Item<?>> MAIN_HAND_ITEM = ContextKey.direct("main_hand_item");
    public static final ContextKey<Item<?>> OFF_HAND_ITEM = ContextKey.direct("off_hand_item");
    public static final ContextKey<Item<?>> ITEM = ContextKey.direct("item");
    public static final ContextKey<CustomBlock> CUSTOM_BLOCK = ContextKey.direct("custom_block");
    public static final ContextKey<ExistingBlock> BLOCK = ContextKey.direct("block");
    public static final ContextKey<Long> TIME = ContextKey.direct("time");
    public static final ContextKey<Key> ID = ContextKey.direct("id");
    public static final ContextKey<Integer> CUSTOM_MODEL_DATA = ContextKey.direct("custom_model_data");
    public static final ContextKey<Furniture> FURNITURE = ContextKey.direct("furniture");
    public static final ContextKey<String> VARIANT = ContextKey.direct("variant");
    public static final ContextKey<InteractionHand> HAND = ContextKey.direct("hand");
    public static final ContextKey<Cancellable> EVENT = ContextKey.direct("event");
    public static final ContextKey<Boolean> IS_SNEAKING = ContextKey.direct("is_sneaking");
    public static final ContextKey<Boolean> IS_SWIMMING = ContextKey.direct("is_swimming");
    public static final ContextKey<Boolean> IS_CLIMBING = ContextKey.direct("is_climbing");
    public static final ContextKey<Boolean> IS_GLIDING = ContextKey.direct("is_gliding");
    public static final ContextKey<Boolean> IS_FLYING = ContextKey.direct("is_flying");
    public static final ContextKey<Boolean> IS_CUSTOM = ContextKey.direct("is_custom");
    public static final ContextKey<Boolean> IS_BLOCK_ITEM = ContextKey.direct("is_block_item");
    public static final ContextKey<GameMode> GAMEMODE = ContextKey.direct("gamemode");
    public static final ContextKey<Integer> COUNT = ContextKey.direct("count");
    public static final ContextKey<Integer> HIT_TIMES = ContextKey.direct("hit_times");
    public static final ContextKey<GameEdition> GAME_EDITION = ContextKey.direct("game_edition");
}
