package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.item.recipe.BukkitRecipeManager;
import net.momirealms.craftengine.core.block.BlockKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.recipe.OptimizedIDItem;
import net.momirealms.craftengine.core.item.recipe.RecipeTypes;
import net.momirealms.craftengine.core.item.recipe.input.SingleItemInput;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.QuadFunction;
import net.momirealms.craftengine.core.world.BlockHitResult;
import net.momirealms.craftengine.core.world.BlockPos;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bell;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InteractUtils {
    private static final Map<Key, QuadFunction<Player, Item<ItemStack>, BlockData, BlockHitResult, Boolean>> INTERACTIONS = new HashMap<>();
    private static final Key NOTE_BLOCK_TOP_INSTRUMENTS = Key.of("minecraft:noteblock_top_instruments");

    private InteractUtils() {}

    static {
        register(BlockKeys.NOTE_BLOCK, (player, item, blockState, result) -> result.getDirection() != Direction.UP || !item.is(NOTE_BLOCK_TOP_INSTRUMENTS));
        register(BlockKeys.CAKE, (player, item, blockState, result) -> !canEat(player, false));
        register(BlockKeys.CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        register(BlockKeys.WHITE_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        register(BlockKeys.ORANGE_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        register(BlockKeys.MAGENTA_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        register(BlockKeys.LIGHT_BLUE_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        register(BlockKeys.YELLOW_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        register(BlockKeys.LIME_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        register(BlockKeys.PINK_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        register(BlockKeys.GRAY_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        register(BlockKeys.LIGHT_GRAY_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        register(BlockKeys.CYAN_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        register(BlockKeys.PURPLE_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        register(BlockKeys.BLUE_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        register(BlockKeys.BROWN_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        register(BlockKeys.GREEN_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        register(BlockKeys.RED_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        register(BlockKeys.BLACK_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        register(BlockKeys.BELL, (player, item, blockState, result) -> {
            Direction direction = result.getDirection();
            BlockPos pos = result.getBlockPos();
            if (blockState instanceof Bell bell) {
                double y = result.getLocation().y() - pos.y();
                if (direction.axis() != Direction.Axis.Y && y <= 0.8123999834060669D) {
                    Direction facing = DirectionUtils.toDirection(bell.getFacing());
                    Bell.Attachment attachment = bell.getAttachment();
                    switch (attachment) {
                        case FLOOR -> {
                            return facing.axis() == direction.axis();
                        }
                        case DOUBLE_WALL, SINGLE_WALL -> {
                            return facing.axis() != direction.axis();
                        }
                        case CEILING -> {
                            return true;
                        }
                        default -> {
                            return false;
                        }
                    }
                }
            }
            return false;
        });
        register(BlockKeys.SOUL_CAMPFIRE, (player, item, blockState, result) -> {
            if (!Config.enableRecipeSystem()) return false;
            Optional<Holder.Reference<Key>> optional = BuiltInRegistries.OPTIMIZED_ITEM_ID.get(item.id());
            return optional.filter(keyReference -> BukkitRecipeManager.instance().recipeByInput(RecipeTypes.CAMPFIRE_COOKING, new SingleItemInput<>(new OptimizedIDItem<>(
                    keyReference, item.getItem()
            ))) != null).isPresent();
        });
        register(BlockKeys.CAMPFIRE, (player, item, blockState, result) -> {
            if (!Config.enableRecipeSystem()) return false;
            Optional<Holder.Reference<Key>> optional = BuiltInRegistries.OPTIMIZED_ITEM_ID.get(item.id());
            return optional.filter(keyReference -> BukkitRecipeManager.instance().recipeByInput(RecipeTypes.CAMPFIRE_COOKING, new SingleItemInput<>(new OptimizedIDItem<>(
                    keyReference, item.getItem()
            ))) != null).isPresent();
        });
        register(BlockKeys.HOPPER, (player, item, blockState, result) -> true);
        register(BlockKeys.DISPENSER, (player, item, blockState, result) -> true);
        register(BlockKeys.DROPPER, (player, item, blockState, result) -> true);
        register(BlockKeys.CRAFTER, (player, item, blockState, result) -> true);
        register(BlockKeys.REPEATER, (player, item, blockState, result) -> true);
        register(BlockKeys.COMPARATOR, (player, item, blockState, result) -> true);
        register(BlockKeys.DAYLIGHT_DETECTOR, (player, item, blockState, result) -> true);
        register(BlockKeys.LECTERN, (player, item, blockState, result) -> true);
        register(BlockKeys.CHEST, (player, item, blockState, result) -> true);
        register(BlockKeys.ENDER_CHEST, (player, item, blockState, result) -> true);
        register(BlockKeys.TRAPPED_CHEST, (player, item, blockState, result) -> true);
        register(BlockKeys.BEACON, (player, item, blockState, result) -> true);
        register(BlockKeys.ENCHANTING_TABLE, (player, item, blockState, result) -> true);
        register(BlockKeys.BREWING_STAND, (player, item, blockState, result) -> true);
        register(BlockKeys.GRINDSTONE, (player, item, blockState, result) -> true);
        register(BlockKeys.ANVIL, (player, item, blockState, result) -> true);
        register(BlockKeys.CHIPPED_ANVIL, (player, item, blockState, result) -> true);
        register(BlockKeys.DAMAGED_ANVIL, (player, item, blockState, result) -> true);
        register(BlockKeys.FURNACE, (player, item, blockState, result) -> true);
        register(BlockKeys.CRAFTING_TABLE, (player, item, blockState, result) -> true);
        register(BlockKeys.STONECUTTER, (player, item, blockState, result) -> true);
        register(BlockKeys.SMITHING_TABLE, (player, item, blockState, result) -> true);
        register(BlockKeys.LOOM, (player, item, blockState, result) -> true);
        register(BlockKeys.BARREL, (player, item, blockState, result) -> true);
        register(BlockKeys.SMOKER, (player, item, blockState, result) -> true);
        register(BlockKeys.BLAST_FURNACE, (player, item, blockState, result) -> true);
        register(BlockKeys.LEVER, (player, item, blockState, result) -> true);
        register(BlockKeys.OAK_BUTTON, (player, item, blockState, result) -> true);
        register(BlockKeys.SPRUCE_BUTTON, (player, item, blockState, result) -> true);
        register(BlockKeys.BIRCH_BUTTON, (player, item, blockState, result) -> true);
        register(BlockKeys.JUNGLE_BUTTON, (player, item, blockState, result) -> true);
        register(BlockKeys.ACACIA_BUTTON, (player, item, blockState, result) -> true);
        register(BlockKeys.CHERRY_BUTTON, (player, item, blockState, result) -> true);
        register(BlockKeys.DARK_OAK_BUTTON, (player, item, blockState, result) -> true);
        register(BlockKeys.PALE_OAK_BUTTON, (player, item, blockState, result) -> true);
        register(BlockKeys.MANGROVE_BUTTON, (player, item, blockState, result) -> true);
        register(BlockKeys.BAMBOO_BUTTON, (player, item, blockState, result) -> true);
        register(BlockKeys.OAK_TRAPDOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.SPRUCE_TRAPDOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.BIRCH_TRAPDOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.JUNGLE_TRAPDOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.ACACIA_TRAPDOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.CHERRY_TRAPDOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.DARK_OAK_TRAPDOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.PALE_OAK_TRAPDOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.MANGROVE_TRAPDOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.BAMBOO_TRAPDOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.CRIMSON_TRAPDOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.WARPED_TRAPDOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.OAK_DOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.SPRUCE_DOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.BIRCH_DOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.JUNGLE_DOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.ACACIA_DOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.CHERRY_DOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.DARK_OAK_DOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.PALE_OAK_DOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.MANGROVE_DOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.BAMBOO_DOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.CRIMSON_DOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.WARPED_DOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.COPPER_DOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.EXPOSED_COPPER_DOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.OXIDIZED_COPPER_DOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.WEATHERED_COPPER_DOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.WAXED_COPPER_DOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.WAXED_EXPOSED_COPPER_DOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.WAXED_OXIDIZED_COPPER_DOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.WAXED_WEATHERED_COPPER_DOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.EXPOSED_COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.OXIDIZED_COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.WEATHERED_COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.WAXED_COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.WAXED_EXPOSED_COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.WAXED_OXIDIZED_COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.WAXED_WEATHERED_COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        register(BlockKeys.OAK_FENCE_GATE, (player, item, blockState, result) -> true);
        register(BlockKeys.SPRUCE_FENCE_GATE, (player, item, blockState, result) -> true);
        register(BlockKeys.BIRCH_FENCE_GATE, (player, item, blockState, result) -> true);
        register(BlockKeys.JUNGLE_FENCE_GATE, (player, item, blockState, result) -> true);
        register(BlockKeys.ACACIA_FENCE_GATE, (player, item, blockState, result) -> true);
        register(BlockKeys.CHERRY_FENCE_GATE, (player, item, blockState, result) -> true);
        register(BlockKeys.DARK_OAK_FENCE_GATE, (player, item, blockState, result) -> true);
        register(BlockKeys.PALE_OAK_FENCE_GATE, (player, item, blockState, result) -> true);
        register(BlockKeys.MANGROVE_FENCE_GATE, (player, item, blockState, result) -> true);
        register(BlockKeys.BAMBOO_FENCE_GATE, (player, item, blockState, result) -> true);
        register(BlockKeys.CRIMSON_FENCE_GATE, (player, item, blockState, result) -> true);
        register(BlockKeys.WARPED_FENCE_GATE, (player, item, blockState, result) -> true);
        register(BlockKeys.OAK_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.SPRUCE_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.ACACIA_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.BIRCH_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.CHERRY_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.JUNGLE_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.DARK_OAK_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.PALE_OAK_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.MANGROVE_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.BAMBOO_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.WARPED_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.CRIMSON_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.OAK_WALL_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.SPRUCE_WALL_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.BIRCH_WALL_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.ACACIA_WALL_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.CHERRY_WALL_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.JUNGLE_WALL_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.DARK_OAK_WALL_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.PALE_OAK_WALL_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.MANGROVE_WALL_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.BAMBOO_WALL_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.CRIMSON_WALL_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.WARPED_WALL_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.OAK_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.SPRUCE_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.BIRCH_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.ACACIA_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.CHERRY_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.JUNGLE_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.DARK_OAK_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.PALE_OAK_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.CRIMSON_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.WARPED_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.MANGROVE_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.BAMBOO_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.OAK_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.SPRUCE_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.BIRCH_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.ACACIA_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.CHERRY_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.JUNGLE_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.DARK_OAK_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.PALE_OAK_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.MANGROVE_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.CRIMSON_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.WARPED_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.BAMBOO_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        register(BlockKeys.SHULKER_BOX, (player, item, blockState, result) -> true);
        register(BlockKeys.WHITE_SHULKER_BOX, (player, item, blockState, result) -> true);
        register(BlockKeys.ORANGE_SHULKER_BOX, (player, item, blockState, result) -> true);
        register(BlockKeys.MAGENTA_SHULKER_BOX, (player, item, blockState, result) -> true);
        register(BlockKeys.LIGHT_BLUE_SHULKER_BOX, (player, item, blockState, result) -> true);
        register(BlockKeys.YELLOW_SHULKER_BOX, (player, item, blockState, result) -> true);
        register(BlockKeys.LIME_SHULKER_BOX, (player, item, blockState, result) -> true);
        register(BlockKeys.PINK_SHULKER_BOX, (player, item, blockState, result) -> true);
        register(BlockKeys.GRAY_SHULKER_BOX, (player, item, blockState, result) -> true);
        register(BlockKeys.LIGHT_GRAY_SHULKER_BOX, (player, item, blockState, result) -> true);
        register(BlockKeys.CYAN_SHULKER_BOX, (player, item, blockState, result) -> true);
        register(BlockKeys.PURPLE_SHULKER_BOX, (player, item, blockState, result) -> true);
        register(BlockKeys.BLUE_SHULKER_BOX, (player, item, blockState, result) -> true);
        register(BlockKeys.BROWN_SHULKER_BOX, (player, item, blockState, result) -> true);
        register(BlockKeys.GREEN_SHULKER_BOX, (player, item, blockState, result) -> true);
        register(BlockKeys.RED_SHULKER_BOX, (player, item, blockState, result) -> true);
        register(BlockKeys.BLACK_SHULKER_BOX, (player, item, blockState, result) -> true);
        register(BlockKeys.WHITE_BED, (player, item, blockState, result) -> true);
        register(BlockKeys.ORANGE_BED, (player, item, blockState, result) -> true);
        register(BlockKeys.MAGENTA_BED, (player, item, blockState, result) -> true);
        register(BlockKeys.LIGHT_BLUE_BED, (player, item, blockState, result) -> true);
        register(BlockKeys.YELLOW_BED, (player, item, blockState, result) -> true);
        register(BlockKeys.LIME_BED, (player, item, blockState, result) -> true);
        register(BlockKeys.PINK_BED, (player, item, blockState, result) -> true);
        register(BlockKeys.GRAY_BED, (player, item, blockState, result) -> true);
        register(BlockKeys.LIGHT_GRAY_BED, (player, item, blockState, result) -> true);
        register(BlockKeys.CYAN_BED, (player, item, blockState, result) -> true);
        register(BlockKeys.PURPLE_BED, (player, item, blockState, result) -> true);
        register(BlockKeys.BLUE_BED, (player, item, blockState, result) -> true);
        register(BlockKeys.BROWN_BED, (player, item, blockState, result) -> true);
        register(BlockKeys.GREEN_BED, (player, item, blockState, result) -> true);
        register(BlockKeys.RED_BED, (player, item, blockState, result) -> true);
        register(BlockKeys.BLACK_BED, (player, item, blockState, result) -> true);
        register(BlockKeys.DRAGON_EGG, (player, item, blockState, result) -> true);
        register(BlockKeys.REPEATING_COMMAND_BLOCK, (player, item, blockState, result) -> true);
        register(BlockKeys.CHAIN_COMMAND_BLOCK, (player, item, blockState, result) -> true);
        register(BlockKeys.COMMAND_BLOCK, (player, item, blockState, result) -> true);
    }

    private static void register(Key key, QuadFunction<org.bukkit.entity.Player, Item<ItemStack>, BlockData, BlockHitResult, Boolean> function) {
        var previous = INTERACTIONS.put(key, function);
        if (previous != null) {
            CraftEngine.instance().logger().warn("Duplicated interaction check: " + key);
        }
    }

    public static boolean isInteractable(Player player, BlockData state, BlockHitResult hit, Item<ItemStack> item) {
        Key blockType = BlockStateUtils.getBlockOwnerId(state);
        if (INTERACTIONS.containsKey(blockType)) {
            return INTERACTIONS.get(blockType).apply(player, item, state, hit);
        } else {
            return false;
        }
    }
    
    private static boolean canEat(Player player, boolean ignoreHunger) {
        return ignoreHunger || player.isInvulnerable() || player.getFoodLevel() < 20;
    }
}
