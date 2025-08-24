package net.momirealms.craftengine.bukkit.util;

import io.papermc.paper.entity.Shearable;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.item.behavior.BlockItemBehavior;
import net.momirealms.craftengine.bukkit.item.recipe.BukkitRecipeManager;
import net.momirealms.craftengine.core.block.BlockKeys;
import net.momirealms.craftengine.core.entity.EntityTypeKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.recipe.RecipeType;
import net.momirealms.craftengine.core.item.recipe.UniqueIdItem;
import net.momirealms.craftengine.core.item.recipe.input.SingleItemInput;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.QuadFunction;
import net.momirealms.craftengine.core.util.TriFunction;
import net.momirealms.craftengine.core.world.BlockHitResult;
import net.momirealms.craftengine.core.world.BlockPos;
import org.bukkit.GameMode;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.Bell;
import org.bukkit.block.data.type.ChiseledBookshelf;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InteractUtils {
    private static final Map<Key, QuadFunction<Player, Item<ItemStack>, BlockData, BlockHitResult, Boolean>> INTERACTIONS = new HashMap<>();
    private static final Map<Key, QuadFunction<Player, Item<ItemStack>, BlockData, BlockHitResult, Boolean>> WILL_CONSUME = new HashMap<>();
    private static final Map<Key, TriFunction<Player, Entity, @Nullable Item<ItemStack>, Boolean>> ENTITY_INTERACTIONS = new HashMap<>();
    private static final Key NOTE_BLOCK_TOP_INSTRUMENTS = Key.of("minecraft:noteblock_top_instruments");

    private InteractUtils() {}

    static {
        registerInteraction(BlockKeys.NOTE_BLOCK, (player, item, blockState, result) -> result.getDirection() != Direction.UP || !item.hasItemTag(NOTE_BLOCK_TOP_INSTRUMENTS));
        registerInteraction(BlockKeys.CAKE, (player, item, blockState, result) -> !canEat(player, false));
        registerInteraction(BlockKeys.CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        registerInteraction(BlockKeys.WHITE_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        registerInteraction(BlockKeys.ORANGE_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        registerInteraction(BlockKeys.MAGENTA_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        registerInteraction(BlockKeys.LIGHT_BLUE_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        registerInteraction(BlockKeys.YELLOW_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        registerInteraction(BlockKeys.LIME_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        registerInteraction(BlockKeys.PINK_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        registerInteraction(BlockKeys.GRAY_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        registerInteraction(BlockKeys.LIGHT_GRAY_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        registerInteraction(BlockKeys.CYAN_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        registerInteraction(BlockKeys.PURPLE_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        registerInteraction(BlockKeys.BLUE_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        registerInteraction(BlockKeys.BROWN_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        registerInteraction(BlockKeys.GREEN_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        registerInteraction(BlockKeys.RED_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        registerInteraction(BlockKeys.BLACK_CANDLE_CAKE, (player, item, blockState, result) -> !canEat(player, false));
        registerInteraction(BlockKeys.COMMAND_BLOCK, (player, item, blockState, result) -> player.isOp() && player.getGameMode() == GameMode.CREATIVE);
        registerInteraction(BlockKeys.CHAIN_COMMAND_BLOCK, (player, item, blockState, result) -> player.isOp() && player.getGameMode() == GameMode.CREATIVE);
        registerInteraction(BlockKeys.REPEATING_COMMAND_BLOCK, (player, item, blockState, result) -> player.isOp() && player.getGameMode() == GameMode.CREATIVE);
        registerInteraction(BlockKeys.JIGSAW, (player, item, blockState, result) -> player.isOp() && player.getGameMode() == GameMode.CREATIVE);
        registerInteraction(BlockKeys.STRUCTURE_BLOCK, (player, item, blockState, result) -> player.isOp() && player.getGameMode() == GameMode.CREATIVE);
        registerInteraction(BlockKeys.TEST_INSTANCE_BLOCK, (player, item, blockState, result) -> player.isOp() && player.getGameMode() == GameMode.CREATIVE);
        registerInteraction(BlockKeys.TEST_BLOCK, (player, item, blockState, result) -> player.isOp() && player.getGameMode() == GameMode.CREATIVE);
        registerInteraction(BlockKeys.LIGHT, (player, item, blockState, result) -> item.vanillaId().equals(ItemKeys.LIGHT));
        registerInteraction(BlockKeys.LODESTONE, (player, item, blockState, result) -> item.vanillaId().equals(ItemKeys.COMPASS));
        registerInteraction(BlockKeys.BEE_NEST, (player, item, blockState, result) -> {
            Key id = item.vanillaId();
            return ItemKeys.SHEARS.equals(id) || ItemKeys.GLASS_BOTTLE.equals(id);
        });
        registerInteraction(BlockKeys.BEEHIVE, (player, item, blockState, result) -> {
            Key id = item.vanillaId();
            return ItemKeys.SHEARS.equals(id) || ItemKeys.GLASS_BOTTLE.equals(id);
        });
        registerInteraction(BlockKeys.POWDER_SNOW, (player, item, blockState, result) -> item.vanillaId().equals(ItemKeys.BUCKET));
        registerInteraction(BlockKeys.REDSTONE_ORE, (player, item, blockState, result) -> {
            Optional<List<ItemBehavior>> behaviors = item.getItemBehavior();
            if (behaviors.isPresent()) {
                for (ItemBehavior behavior : behaviors.get()) {
                    if (behavior instanceof BlockItemBehavior) return false;
                }
            }
            return true;
        });
        registerInteraction(BlockKeys.DEEPSLATE_REDSTONE_ORE, (player, item, blockState, result) -> {
            Optional<List<ItemBehavior>> behaviors = item.getItemBehavior();
            if (behaviors.isPresent()) {
                for (ItemBehavior behavior : behaviors.get()) {
                    if (behavior instanceof BlockItemBehavior) return false;
                }
            }
            return true;
        });

        registerInteraction(BlockKeys.BELL, (player, item, blockState, result) -> {
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
        registerInteraction(BlockKeys.SOUL_CAMPFIRE, (player, item, blockState, result) -> {
            if (!Config.enableRecipeSystem()) return false;
            return BukkitRecipeManager.instance().recipeByInput(RecipeType.CAMPFIRE_COOKING, new SingleItemInput<>(UniqueIdItem.of(item))) != null;
        });
        registerInteraction(BlockKeys.CAMPFIRE, (player, item, blockState, result) -> {
            if (!Config.enableRecipeSystem()) return false;
            return BukkitRecipeManager.instance().recipeByInput(RecipeType.CAMPFIRE_COOKING, new SingleItemInput<>(UniqueIdItem.of(item))) != null;
        });
        registerInteraction(BlockKeys.CHISELED_BOOKSHELF, (player, item, blockState, result) -> {
            if (!(blockState instanceof ChiseledBookshelf chiseledBookshelf)) return false;
            return DirectionUtils.toDirection(chiseledBookshelf.getFacing()) == result.getDirection();
        });
        registerInteraction(BlockKeys.COMPOSTER, (player, item, blockState, result) -> {
            if (item.getItem().getType().isCompostable()) return true;
            return blockState instanceof Levelled levelled && levelled.getLevel() == levelled.getMaximumLevel();
        });
        registerInteraction(BlockKeys.RESPAWN_ANCHOR, (player, item, blockState, result) -> {
            if (item.vanillaId().equals(ItemKeys.GLOWSTONE)) return true;
            return blockState instanceof RespawnAnchor respawnAnchor && respawnAnchor.getCharges() != 0;
        });
        registerInteraction(BlockKeys.DECORATED_POT, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.FLOWER_POT, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.HOPPER, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DISPENSER, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DROPPER, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CRAFTER, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.REPEATER, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.COMPARATOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DAYLIGHT_DETECTOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.LECTERN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CHEST, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ENDER_CHEST, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.TRAPPED_CHEST, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BEACON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ENCHANTING_TABLE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BREWING_STAND, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.GRINDSTONE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ANVIL, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CHIPPED_ANVIL, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DAMAGED_ANVIL, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.FURNACE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CRAFTING_TABLE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CARTOGRAPHY_TABLE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.STONECUTTER, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.SMITHING_TABLE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.LOOM, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BARREL, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.SMOKER, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BLAST_FURNACE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.LEVER, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.OAK_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.SPRUCE_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BIRCH_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.JUNGLE_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ACACIA_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CHERRY_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DARK_OAK_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PALE_OAK_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.MANGROVE_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BAMBOO_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.STONE_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.POLISHED_BLACKSTONE_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CRIMSON_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WARPED_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.OAK_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.SPRUCE_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BIRCH_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.JUNGLE_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ACACIA_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CHERRY_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DARK_OAK_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PALE_OAK_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.MANGROVE_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BAMBOO_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CRIMSON_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WARPED_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.OAK_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.SPRUCE_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BIRCH_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.JUNGLE_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ACACIA_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CHERRY_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DARK_OAK_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PALE_OAK_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.MANGROVE_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BAMBOO_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CRIMSON_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WARPED_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.COPPER_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.EXPOSED_COPPER_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.OXIDIZED_COPPER_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WEATHERED_COPPER_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WAXED_COPPER_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WAXED_EXPOSED_COPPER_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WAXED_OXIDIZED_COPPER_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WAXED_WEATHERED_COPPER_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.EXPOSED_COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.OXIDIZED_COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WEATHERED_COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WAXED_COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WAXED_EXPOSED_COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WAXED_OXIDIZED_COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WAXED_WEATHERED_COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.OAK_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.SPRUCE_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BIRCH_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.JUNGLE_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ACACIA_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CHERRY_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DARK_OAK_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PALE_OAK_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.MANGROVE_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BAMBOO_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CRIMSON_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WARPED_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.OAK_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.SPRUCE_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ACACIA_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BIRCH_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CHERRY_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.JUNGLE_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DARK_OAK_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PALE_OAK_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.MANGROVE_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BAMBOO_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WARPED_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CRIMSON_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.OAK_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.SPRUCE_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BIRCH_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ACACIA_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CHERRY_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.JUNGLE_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DARK_OAK_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PALE_OAK_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.MANGROVE_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BAMBOO_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CRIMSON_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WARPED_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.OAK_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.SPRUCE_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BIRCH_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ACACIA_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CHERRY_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.JUNGLE_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DARK_OAK_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PALE_OAK_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CRIMSON_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WARPED_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.MANGROVE_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BAMBOO_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.OAK_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.SPRUCE_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BIRCH_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ACACIA_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CHERRY_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.JUNGLE_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DARK_OAK_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PALE_OAK_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.MANGROVE_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CRIMSON_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WARPED_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BAMBOO_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WHITE_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ORANGE_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.MAGENTA_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.LIGHT_BLUE_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.YELLOW_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.LIME_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PINK_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.GRAY_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.LIGHT_GRAY_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CYAN_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PURPLE_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BLUE_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BROWN_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.GREEN_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.RED_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BLACK_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WHITE_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ORANGE_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.MAGENTA_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.LIGHT_BLUE_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.YELLOW_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.LIME_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PINK_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.GRAY_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.LIGHT_GRAY_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CYAN_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PURPLE_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BLUE_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BROWN_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.GREEN_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.RED_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BLACK_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DRAGON_EGG, (player, item, blockState, result) -> true);
    }

    static {
        registerWillConsume(BlockKeys.CACTUS, (player, item, blockState, result) ->
                result.getDirection() == Direction.UP && item.id().equals(ItemKeys.CACTUS));
        registerWillConsume(BlockKeys.CAULDRON, (player, item, blockState, result) -> {
            Key id = item.vanillaId();
            return ItemKeys.WATER_BUCKET.equals(id) || ItemKeys.LAVA_BUCKET.equals(id);
        });
        registerWillConsume(BlockKeys.LAVA_CAULDRON, (player, item, blockState, result) -> {
            Key id = item.vanillaId();
            return ItemKeys.BUCKET.equals(id) || ItemKeys.LAVA_BUCKET.equals(id) || ItemKeys.WATER_BUCKET.equals(id);
        });
        registerWillConsume(BlockKeys.WATER_CAULDRON, (player, item, blockState, result) -> {
            if (blockState instanceof Levelled levelled && levelled.getLevel() == levelled.getMaximumLevel())
                return item.vanillaId().equals(ItemKeys.BUCKET);
            Key id = item.vanillaId();
            return ItemKeys.GLASS_BOTTLE.equals(id) || ItemKeys.WATER_BUCKET.equals(id) || ItemKeys.LAVA_BUCKET.equals(id);
        });
    }

    static {
        registerEntityInteraction(EntityTypeKeys.BEE, (player, entity, item) -> canFeed(entity, item));
        registerEntityInteraction(EntityTypeKeys.FOX, (player, entity, item) -> canFeed(entity, item));
        registerEntityInteraction(EntityTypeKeys.FROG, (player, entity, item) -> canFeed(entity, item));
        registerEntityInteraction(EntityTypeKeys.PANDA, (player, entity, item) -> canFeed(entity, item));
        registerEntityInteraction(EntityTypeKeys.HOGLIN, (player, entity, item) -> canFeed(entity, item));
        registerEntityInteraction(EntityTypeKeys.OCELOT, (player, entity, item) -> canFeed(entity, item));
        registerEntityInteraction(EntityTypeKeys.RABBIT, (player, entity, item) -> canFeed(entity, item));
        registerEntityInteraction(EntityTypeKeys.TURTLE, (player, entity, item) -> canFeed(entity, item));
        registerEntityInteraction(EntityTypeKeys.CHICKEN, (player, entity, item) -> canFeed(entity, item));
        registerEntityInteraction(EntityTypeKeys.SNIFFER, (player, entity, item) -> canFeed(entity, item));
        registerEntityInteraction(EntityTypeKeys.AXOLOTL, (player, entity, item) ->
                canFeed(entity, item) || (item != null && item.vanillaId().equals(ItemKeys.WATER_BUCKET)));
        registerEntityInteraction(EntityTypeKeys.COD, (player, entity, item) ->
                item != null && item.vanillaId().equals(ItemKeys.WATER_BUCKET));
        registerEntityInteraction(EntityTypeKeys.SALMON, (player, entity, item) ->
                item != null && item.vanillaId().equals(ItemKeys.WATER_BUCKET));
        registerEntityInteraction(EntityTypeKeys.TROPICAL_FISH, (player, entity, item) ->
                item != null && item.vanillaId().equals(ItemKeys.WATER_BUCKET));
        registerEntityInteraction(EntityTypeKeys.PUFFERFISH, (player, entity, item) ->
                item != null && item.vanillaId().equals(ItemKeys.WATER_BUCKET));
        registerEntityInteraction(EntityTypeKeys.TADPOLE, (player, entity, item) ->
                item != null && item.vanillaId().equals(ItemKeys.WATER_BUCKET));
        registerEntityInteraction(EntityTypeKeys.SNOW_GOLEM, (player, entity, item) ->
                shearable(entity, item));
        registerEntityInteraction(EntityTypeKeys.SHEEP, (player, entity, item) ->
                canFeed(entity, item) || shearable(entity, item));
        registerEntityInteraction(EntityTypeKeys.BOGGED, (player, entity, item) ->
                canFeed(entity, item) || shearable(entity, item));
        registerEntityInteraction(EntityTypeKeys.MOOSHROOM, (player, entity, item) ->
                canFeed(entity, item) || shearable(entity, item) || (item != null && (item.vanillaId().equals(ItemKeys.BUCKET) || item.vanillaId().equals(ItemKeys.BOWL))));
        registerEntityInteraction(EntityTypeKeys.COW, (player, entity, item) ->
                canFeed(entity, item) || (item != null && item.vanillaId().equals(ItemKeys.BUCKET)));
        registerEntityInteraction(EntityTypeKeys.GOAT, (player, entity, item) ->
                canFeed(entity, item) || (item != null && item.vanillaId().equals(ItemKeys.BUCKET)));
        registerEntityInteraction(EntityTypeKeys.CREEPER, (player, entity, item) ->
                item != null && item.vanillaId().equals(ItemKeys.FLINT_AND_STEEL));
        registerEntityInteraction(EntityTypeKeys.PIGLIN, (player, entity, item) ->
                item != null && item.vanillaId().equals(ItemKeys.GOLD_INGOT));
        registerEntityInteraction(EntityTypeKeys.ARMADILLO, (player, entity, item) ->
                canFeed(entity, item) || (item != null && item.vanillaId().equals(ItemKeys.BRUSH)));
        registerEntityInteraction(EntityTypeKeys.ZOMBIE_HORSE, (player, entity, item) ->
                entity instanceof Tameable tameable && tameable.isTamed());
        registerEntityInteraction(EntityTypeKeys.SKELETON_HORSE, (player, entity, item) ->
                entity instanceof Tameable tameable && tameable.isTamed());
        registerEntityInteraction(EntityTypeKeys.PIG, (player, entity, item) ->
                canFeed(entity, item) || (item != null && item.vanillaId().equals(ItemKeys.SADDLE) && !hasSaddle(player, entity)) || (hasSaddle(player, entity) && !player.isSneaking()));
        registerEntityInteraction(EntityTypeKeys.STRIDER, (player, entity, item) ->
                canFeed(entity, item) || (item != null && item.vanillaId().equals(ItemKeys.SADDLE) && !hasSaddle(player, entity)) || (hasSaddle(player, entity) && !player.isSneaking()));
        registerEntityInteraction(EntityTypeKeys.WOLF, (player, entity, item) -> canFeed(entity, item) || isPetOwner(player, entity));
        registerEntityInteraction(EntityTypeKeys.CAT, (player, entity, item) -> canFeed(entity, item) || isPetOwner(player, entity));
        registerEntityInteraction(EntityTypeKeys.BOAT, (player, entity, item) -> !player.isSneaking());
        registerEntityInteraction(EntityTypeKeys.OAK_BOAT, (player, entity, item) -> !player.isSneaking());
        registerEntityInteraction(EntityTypeKeys.SPRUCE_BOAT, (player, entity, item) -> !player.isSneaking());
        registerEntityInteraction(EntityTypeKeys.BIRCH_BOAT, (player, entity, item) -> !player.isSneaking());
        registerEntityInteraction(EntityTypeKeys.JUNGLE_BOAT, (player, entity, item) -> !player.isSneaking());
        registerEntityInteraction(EntityTypeKeys.ACACIA_BOAT, (player, entity, item) -> !player.isSneaking());
        registerEntityInteraction(EntityTypeKeys.DARK_OAK_BOAT, (player, entity, item) -> !player.isSneaking());
        registerEntityInteraction(EntityTypeKeys.MANGROVE_BOAT, (player, entity, item) -> !player.isSneaking());
        registerEntityInteraction(EntityTypeKeys.CHERRY_BOAT, (player, entity, item) -> !player.isSneaking());
        registerEntityInteraction(EntityTypeKeys.PALE_OAK_BOAT, (player, entity, item) -> !player.isSneaking());
        registerEntityInteraction(EntityTypeKeys.BAMBOO_RAFT, (player, entity, item) -> !player.isSneaking());
        registerEntityInteraction(EntityTypeKeys.MINECART, (player, entity, item) -> !player.isSneaking());
        registerEntityInteraction(EntityTypeKeys.PARROT, (player, entity, item) -> {
            if (item != null && item.hasItemTag(Key.of("parrot_poisonous_food"))) return true;
            return canFeed(entity, item) || isPetOwner(player, entity);
        });
        registerEntityInteraction(EntityTypeKeys.HAPPY_GHAST, (player, entity, item) -> {
            if (item != null && item.vanillaId().equals(ItemKeys.HARNESS)) return true;
            if (entity instanceof HappyGhast happyGhast && !player.isSneaking()) {
                ItemStack bodyItem = happyGhast.getEquipment().getItem(EquipmentSlot.BODY);
                return BukkitItemManager.instance().wrap(bodyItem).hasItemTag(Key.of("harnesses"));
            }
            return canFeed(entity, item);
        });
        registerEntityInteraction(EntityTypeKeys.ALLAY, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.HORSE, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.DONKEY, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.MULE, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.VILLAGER, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.WANDERING_TRADER, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.LLAMA, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.TRADER_LLAMA, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.CAMEL, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.ITEM_FRAME, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.GLOW_ITEM_FRAME, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.INTERACTION, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.CHEST_BOAT, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.OAK_CHEST_BOAT, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.SPRUCE_CHEST_BOAT, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.BIRCH_CHEST_BOAT, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.JUNGLE_CHEST_BOAT, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.ACACIA_CHEST_BOAT, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.DARK_OAK_CHEST_BOAT, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.MANGROVE_CHEST_BOAT, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.CHERRY_CHEST_BOAT, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.PALE_OAK_CHEST_BOAT, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.BAMBOO_CHEST_RAFT, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.CHEST_MINECART, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.FURNACE_MINECART, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.HOPPER_MINECART, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.COMMAND_BLOCK_MINECART, (player, entity, item) -> true);
    }

    private static void registerInteraction(Key key, QuadFunction<org.bukkit.entity.Player, Item<ItemStack>, BlockData, BlockHitResult, Boolean> function) {
        var previous = INTERACTIONS.put(key, function);
        if (previous != null) {
            CraftEngine.instance().logger().warn("Duplicated interaction check: " + key);
        }
    }

    private static void registerWillConsume(Key key, QuadFunction<org.bukkit.entity.Player, Item<ItemStack>, BlockData, BlockHitResult, Boolean> function) {
        var previous = WILL_CONSUME.put(key, function);
        if (previous != null) {
            CraftEngine.instance().logger().warn("Duplicated interaction check: " + key);
        }
    }

    private static void registerEntityInteraction(Key key, TriFunction<Player, Entity, @Nullable Item<ItemStack>, Boolean> function) {
        var previous = ENTITY_INTERACTIONS.put(key, function);
        if (previous != null) {
            CraftEngine.instance().logger().warn("Duplicated entity interaction check: " + key);
        }
    }

    public static boolean isInteractable(Player player, BlockData state, BlockHitResult hit, @Nullable Item<ItemStack> item) {
        Key blockType = BlockStateUtils.getBlockOwnerIdFromData(state);
        if (INTERACTIONS.containsKey(blockType)) {
            return INTERACTIONS.get(blockType).apply(player, item, state, hit);
        } else {
            return false;
        }
    }

    public static boolean isEntityInteractable(Player player, Entity entity, @Nullable Item<ItemStack> item) {
        TriFunction<Player, Entity, Item<ItemStack>, Boolean> func = ENTITY_INTERACTIONS.get(EntityUtils.getEntityType(entity));
        return func != null && func.apply(player, entity, item);
    }

    public static boolean willConsume(Player player, BlockData state, BlockHitResult hit, @Nullable Item<ItemStack> item) {
        if (item == null) return false;
        Key blockType = BlockStateUtils.getBlockOwnerIdFromData(state);
        if (WILL_CONSUME.containsKey(blockType)) {
            return WILL_CONSUME.get(blockType).apply(player, item, state, hit);
        } else {
            return false;
        }
    }

    private static boolean canEat(Player player, boolean ignoreHunger) {
        return ignoreHunger || player.isInvulnerable() || player.getFoodLevel() < 20;
    }

    private static boolean canFeed(Entity entity, Item<ItemStack> item) {
        return entity instanceof Animals && item.hasItemTag(Key.of(EntityUtils.getEntityType(entity).value() + "_food"));
    }

    private static boolean hasSaddle(Player player, Entity entity) {
        return entity instanceof Steerable steerable && steerable.hasSaddle() && !player.isSneaking();
    }

    private static boolean shearable(Entity entity, Item<ItemStack> item) {
        return entity instanceof Shearable shearable && item.vanillaId().equals(ItemKeys.SHEARS) && shearable.readyToBeSheared();
    }

    private static boolean isPetOwner(Player player, Entity entity) {
        return entity instanceof Tameable tameable && tameable.isTamed() && player.getUniqueId().equals(tameable.getOwnerUniqueId());
    }
}
