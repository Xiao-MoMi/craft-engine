package net.momirealms.craftengine.bukkit.util;

import io.papermc.paper.entity.Shearable;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.item.behavior.BlockItemBehavior;
import net.momirealms.craftengine.bukkit.item.behavior.FlintAndSteelItemBehavior;
import net.momirealms.craftengine.bukkit.item.recipe.BukkitRecipeManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.world.BukkitExistingBlock;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.block.BlockKeys;
import net.momirealms.craftengine.core.entity.EntityTypeKeys;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.processor.AttributeModifiersProcessor;
import net.momirealms.craftengine.core.item.recipe.RecipeType;
import net.momirealms.craftengine.core.item.recipe.UniqueIdItem;
import net.momirealms.craftengine.core.item.recipe.input.SingleItemInput;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.BlockHitResult;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.type.*;
import org.bukkit.block.data.type.Observer;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class InteractUtils {
    private static final Map<Key, QuadFunction<Player, Item<ItemStack>, BlockData, BlockHitResult, Boolean>> INTERACTIONS = new HashMap<>();
    private static final Map<Key, QuadFunction<Player, Item<ItemStack>, BlockData, BlockHitResult, Boolean>> CAN_PLACE = new HashMap<>();
    private static final Map<Key, TriFunction<Player, Entity, @Nullable Item<ItemStack>, Boolean>> ENTITY_INTERACTIONS = new HashMap<>();

    private static final Key NOTE_BLOCK_TOP_INSTRUMENTS = Key.of("minecraft:noteblock_top_instruments");
    private static final Key PARROT_POISONOUS_FOOD = Key.of("minecraft:parrot_poisonous_food");
    private static final Key HARNESSES = Key.of("minecraft:harnesses");
    private static final Key FROG_FOOD = Key.of("minecraft:frog_food");
    private static final Key CANDLES = Key.of("minecraft:candles");

    private InteractUtils() {}

    // 方块
    static {
        registerInteraction(BlockKeys.NOTE_BLOCK, (player, item, blockState, result) -> result.direction() != Direction.UP || !item.hasItemTag(NOTE_BLOCK_TOP_INSTRUMENTS));
        registerInteraction(BlockKeys.POWDER_SNOW, (player, item, blockState, result) -> {
            Key id = item.vanillaId();
            return ItemKeys.BUCKET.equals(id);
        });
        // 功能方块
        registerInteraction(BlockKeys.CRAFTING_TABLE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.STONECUTTER, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CARTOGRAPHY_TABLE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.SMITHING_TABLE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.GRINDSTONE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.LOOM, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.FURNACE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.SMOKER, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BLAST_FURNACE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CAMPFIRE, (player, item, blockState, result) -> {
            if (!Config.enableRecipeSystem()) return false;
            return BukkitRecipeManager.instance().recipeByInput(RecipeType.CAMPFIRE_COOKING, new SingleItemInput<>(UniqueIdItem.of(item))) != null;
        });
        registerInteraction(BlockKeys.SOUL_CAMPFIRE, (player, item, blockState, result) -> {
            if (!Config.enableRecipeSystem()) return false;
            return BukkitRecipeManager.instance().recipeByInput(RecipeType.CAMPFIRE_COOKING, new SingleItemInput<>(UniqueIdItem.of(item))) != null;
        });
        registerInteraction(BlockKeys.ANVIL, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CHIPPED_ANVIL, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DAMAGED_ANVIL, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.COMPOSTER, (player, item, blockState, result) -> {
            if (item.getItem().getType().isCompostable()) return true;
            return blockState instanceof Levelled levelled && levelled.getLevel() == levelled.getMaximumLevel();
        });
        registerInteraction(BlockKeys.JUKEBOX, (player, item, blockState, result) -> {
            if (blockState instanceof Jukebox jukebox && jukebox.hasRecord()) return true;
            return item.getItem().getType().isRecord();
        });
        registerInteraction(BlockKeys.ENCHANTING_TABLE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BREWING_STAND, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CAULDRON, (player, item, blockState, result) -> {
            Key id = item.vanillaId();
            return ItemKeys.WATER_BUCKET.equals(id) || ItemKeys.LAVA_BUCKET.equals(id);
        });
        registerInteraction(BlockKeys.LAVA_CAULDRON, (player, item, blockState, result) -> {
            Key id = item.vanillaId();
            return ItemKeys.BUCKET.equals(id) || ItemKeys.LAVA_BUCKET.equals(id) || ItemKeys.WATER_BUCKET.equals(id);
        });
        registerInteraction(BlockKeys.WATER_CAULDRON, (player, item, blockState, result) -> {
            if (blockState instanceof Levelled levelled && levelled.getLevel() == levelled.getMaximumLevel())
                return item.vanillaId().equals(ItemKeys.BUCKET);
            Key id = item.vanillaId();
            return ItemKeys.GLASS_BOTTLE.equals(id) || ItemKeys.WATER_BUCKET.equals(id) || ItemKeys.LAVA_BUCKET.equals(id);
        });
        registerInteraction(BlockKeys.BELL, (player, item, blockState, result) -> {
            Direction direction = result.direction();
            BlockPos pos = result.blockPos();
            if (blockState instanceof Bell bell) {
                double y = result.location().y() - pos.y();
                if (direction.axis() != Direction.Axis.Y && !(y > 0.8124F)) {
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
        registerInteraction(BlockKeys.BEACON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BEE_NEST, (player, item, blockState, result) -> {
            if (blockState instanceof Beehive beehive && beehive.getHoneyLevel() == beehive.getMaximumHoneyLevel()) {
                Key id = item.vanillaId();
                return ItemKeys.SHEARS.equals(id) || ItemKeys.GLASS_BOTTLE.equals(id);
            }
            return false;
        });
        registerInteraction(BlockKeys.BEEHIVE, (player, item, blockState, result) -> {
            if (blockState instanceof Beehive beehive && beehive.getHoneyLevel() == beehive.getMaximumHoneyLevel()) {
                Key id = item.vanillaId();
                return ItemKeys.SHEARS.equals(id) || ItemKeys.GLASS_BOTTLE.equals(id);
            }
            return false;
        });
        registerInteraction(BlockKeys.FLOWER_POT, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DECORATED_POT, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CHISELED_BOOKSHELF, (player, item, blockState, result) -> {
            if (!(blockState instanceof ChiseledBookshelf chiseledBookshelf)) return false;
            return DirectionUtils.toDirection(chiseledBookshelf.getFacing()) == result.direction();
        });
        registerInteraction(BlockKeys.LECTERN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CHEST, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BARREL, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ENDER_CHEST, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.TRAPPED_CHEST, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.RESPAWN_ANCHOR, (player, item, blockState, result) -> {
            Key id = item.vanillaId();
            if (ItemKeys.GLOWSTONE.equals(id)) return true;
            return blockState instanceof RespawnAnchor respawnAnchor && respawnAnchor.getCharges() != 0;
        });
        registerInteraction(BlockKeys.DRAGON_EGG, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.END_PORTAL_FRAME, (player, item, blockState, result) -> {
            Key id = item.vanillaId();
            return ItemKeys.ENDER_EYE.equals(id);
        });
        registerInteraction(BlockKeys.VAULT, (player, item, blockState, result) -> blockState instanceof Vault vault && vault.getVaultState() == Vault.State.ACTIVE);
        registerInteraction(BlockKeys.SPAWNER, (player, item, blockState, result) -> {
            Key id = item.vanillaId();
            return id.asString().endsWith("_spawn_egg");
        });
        registerInteraction(BlockKeys.TRIAL_SPAWNER, (player, item, blockState, result) -> {
            Key id = item.vanillaId();
            return id.asString().endsWith("_spawn_egg");
        });
        // 红石方块
        registerInteraction(BlockKeys.REDSTONE_WIRE, (player, item, blockState, result) -> {
            if (blockState instanceof RedstoneWire redstoneWire) {
                boolean isCross = redstoneWire.getFace(BlockFace.EAST).equals(RedstoneWire.Connection.SIDE)
                        && redstoneWire.getFace(BlockFace.NORTH).equals(RedstoneWire.Connection.SIDE)
                        && redstoneWire.getFace(BlockFace.SOUTH).equals(RedstoneWire.Connection.SIDE)
                        && redstoneWire.getFace(BlockFace.WEST).equals(RedstoneWire.Connection.SIDE);
                boolean isDot = redstoneWire.getFace(BlockFace.EAST).equals(RedstoneWire.Connection.NONE)
                        && redstoneWire.getFace(BlockFace.NORTH).equals(RedstoneWire.Connection.NONE)
                        && redstoneWire.getFace(BlockFace.SOUTH).equals(RedstoneWire.Connection.NONE)
                        && redstoneWire.getFace(BlockFace.WEST).equals(RedstoneWire.Connection.NONE);
                if (isCross || isDot) {
                    BlockPos blockPos = result.blockPos();
                    BukkitWorld bukkitWorld = BukkitAdaptors.adapt(player.getWorld());
                    World world = bukkitWorld.platformWorld();

                    Direction[] directions = {Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};
                    for (Direction direction : directions) {
                        BlockPos neighborPos = blockPos.relative(direction);
                        Block neighborBlock = world.getBlockAt(neighborPos.x(), neighborPos.y(), neighborPos.z());
                        Key neighborBlockKey = new BukkitExistingBlock(neighborBlock).id();
                        BlockData neighborBlockData = neighborBlock.getBlockData();
                        boolean canConnection = ArrayUtils.contains(BlockKeys.REDSTONE_CONNECTION, neighborBlockKey)
                                || ArrayUtils.contains(BlockKeys.PRESSURE_PLATES, neighborBlockKey)
                                || ArrayUtils.contains(BlockKeys.BUTTONS, neighborBlockKey);
                        if (canConnection) {
                            return switch (neighborBlockData) {
                                case Repeater repeater -> {
                                    Direction neighborDirection = DirectionUtils.toDirection(repeater.getFacing());
                                    yield !(neighborDirection == direction || neighborDirection == direction.opposite());
                                }
                                case Observer observer -> {
                                    Direction neighborDirection = DirectionUtils.toDirection(observer.getFacing());
                                    yield !(neighborDirection == direction);
                                }
                                default -> false;
                            };
                        }
                    }
                    return true;
                }
            }
            return false;
        });
        registerInteraction(BlockKeys.REPEATER, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.COMPARATOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.LEVER, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DAYLIGHT_DETECTOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DISPENSER, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DROPPER, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CRAFTER, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.HOPPER, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.TNT, (player, item, blockState, result) -> {
            Optional<List<ItemBehavior>> behaviors = item.getItemBehavior();
            if (behaviors.isPresent()) {
                for (ItemBehavior behavior : behaviors.get()) {
                    if (behavior instanceof FlintAndSteelItemBehavior) return true;
                }
            }
            return false;
        });
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
        // 管理员用品
        registerInteraction(BlockKeys.COMMAND_BLOCK, (player, item, blockState, result) -> player.isOp() && player.getGameMode() == GameMode.CREATIVE);
        registerInteraction(BlockKeys.CHAIN_COMMAND_BLOCK, (player, item, blockState, result) -> player.isOp() && player.getGameMode() == GameMode.CREATIVE);
        registerInteraction(BlockKeys.REPEATING_COMMAND_BLOCK, (player, item, blockState, result) -> player.isOp() && player.getGameMode() == GameMode.CREATIVE);
        registerInteraction(BlockKeys.JIGSAW, (player, item, blockState, result) -> player.isOp() && player.getGameMode() == GameMode.CREATIVE);
        registerInteraction(BlockKeys.STRUCTURE_BLOCK, (player, item, blockState, result) -> player.isOp() && player.getGameMode() == GameMode.CREATIVE);
        registerInteraction(BlockKeys.TEST_INSTANCE_BLOCK, (player, item, blockState, result) -> player.isOp() && player.getGameMode() == GameMode.CREATIVE);
        registerInteraction(BlockKeys.TEST_BLOCK, (player, item, blockState, result) -> player.isOp() && player.getGameMode() == GameMode.CREATIVE);
        registerInteraction(BlockKeys.LIGHT, (player, item, blockState, result) -> {
            if (player.isOp() && player.getGameMode() == GameMode.CREATIVE) {
                Key id = item.vanillaId();
                return ItemKeys.LIGHT.equals(id);
            }
            return false;
        });
        // 床
        registerInteraction(BlockKeys.WHITE_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.LIGHT_GRAY_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.GRAY_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BLACK_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BROWN_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.RED_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ORANGE_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.YELLOW_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.LIME_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.GREEN_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CYAN_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.LIGHT_BLUE_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BLUE_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PURPLE_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.MAGENTA_BED, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PINK_BED, (player, item, blockState, result) -> true);
        // 蜡烛
        registerInteraction(BlockKeys.CANDLE, (player, item, blockState, result) -> {
            if (blockState instanceof Candle candle) {
                Key id = item.vanillaId();
                if (!candle.isLit()) {
                    return ItemKeys.FLINT_AND_STEEL.equals(id);
                }
            }
            return false;
        });
        registerInteraction(BlockKeys.WHITE_CANDLE, (player, item, blockState, result) -> {
            if (blockState instanceof Candle candle) {
                Key id = item.vanillaId();
                if (!candle.isLit()) {
                    return ItemKeys.FLINT_AND_STEEL.equals(id);
                }
            }
            return false;
        });
        registerInteraction(BlockKeys.LIGHT_GRAY_CANDLE, (player, item, blockState, result) -> {
            if (blockState instanceof Candle candle) {
                Key id = item.vanillaId();
                if (!candle.isLit()) {
                    return ItemKeys.FLINT_AND_STEEL.equals(id);
                }
            }
            return false;
        });
        registerInteraction(BlockKeys.GRAY_CANDLE, (player, item, blockState, result) -> {
            if (blockState instanceof Candle candle) {
                Key id = item.vanillaId();
                if (!candle.isLit()) {
                    return ItemKeys.FLINT_AND_STEEL.equals(id);
                }
            }
            return false;
        });
        registerInteraction(BlockKeys.BLACK_CANDLE, (player, item, blockState, result) -> {
            if (blockState instanceof Candle candle) {
                Key id = item.vanillaId();
                if (!candle.isLit()) {
                    return ItemKeys.FLINT_AND_STEEL.equals(id);
                }
            }
            return false;
        });
        registerInteraction(BlockKeys.BROWN_CANDLE, (player, item, blockState, result) -> {
            if (blockState instanceof Candle candle) {
                Key id = item.vanillaId();
                if (!candle.isLit()) {
                    return ItemKeys.FLINT_AND_STEEL.equals(id);
                }
            }
            return false;
        });
        registerInteraction(BlockKeys.RED_CANDLE, (player, item, blockState, result) -> {
            if (blockState instanceof Candle candle) {
                Key id = item.vanillaId();
                if (!candle.isLit()) {
                    return ItemKeys.FLINT_AND_STEEL.equals(id);
                }
            }
            return false;
        });
        registerInteraction(BlockKeys.ORANGE_CANDLE, (player, item, blockState, result) -> {
            if (blockState instanceof Candle candle) {
                Key id = item.vanillaId();
                if (!candle.isLit()) {
                    return ItemKeys.FLINT_AND_STEEL.equals(id);
                }
            }
            return false;
        });
        registerInteraction(BlockKeys.YELLOW_CANDLE, (player, item, blockState, result) -> {
            if (blockState instanceof Candle candle) {
                Key id = item.vanillaId();
                if (!candle.isLit()) {
                    return ItemKeys.FLINT_AND_STEEL.equals(id);
                }
            }
            return false;
        });
        registerInteraction(BlockKeys.LIME_CANDLE, (player, item, blockState, result) -> {
            if (blockState instanceof Candle candle) {
                Key id = item.vanillaId();
                if (!candle.isLit()) {
                    return ItemKeys.FLINT_AND_STEEL.equals(id);
                }
            }
            return false;
        });
        registerInteraction(BlockKeys.GREEN_CANDLE, (player, item, blockState, result) -> {
            if (blockState instanceof Candle candle) {
                Key id = item.vanillaId();
                if (!candle.isLit()) {
                    return ItemKeys.FLINT_AND_STEEL.equals(id);
                }
            }
            return false;
        });
        registerInteraction(BlockKeys.CYAN_CANDLE, (player, item, blockState, result) -> {
            if (blockState instanceof Candle candle) {
                Key id = item.vanillaId();
                if (!candle.isLit()) {
                    return ItemKeys.FLINT_AND_STEEL.equals(id);
                }
            }
            return false;
        });
        registerInteraction(BlockKeys.LIGHT_BLUE_CANDLE, (player, item, blockState, result) -> {
            if (blockState instanceof Candle candle) {
                Key id = item.vanillaId();
                if (!candle.isLit()) {
                    return ItemKeys.FLINT_AND_STEEL.equals(id);
                }
            }
            return false;
        });
        registerInteraction(BlockKeys.BLUE_CANDLE, (player, item, blockState, result) -> {
            if (blockState instanceof Candle candle) {
                Key id = item.vanillaId();
                if (!candle.isLit()) {
                    return ItemKeys.FLINT_AND_STEEL.equals(id);
                }
            }
            return false;
        });
        registerInteraction(BlockKeys.PURPLE_CANDLE, (player, item, blockState, result) -> {
            if (blockState instanceof Candle candle) {
                Key id = item.vanillaId();
                if (!candle.isLit()) {
                    return ItemKeys.FLINT_AND_STEEL.equals(id);
                }
            }
            return false;
        });
        registerInteraction(BlockKeys.MAGENTA_CANDLE, (player, item, blockState, result) -> {
            if (blockState instanceof Candle candle) {
                Key id = item.vanillaId();
                if (!candle.isLit()) {
                    return ItemKeys.FLINT_AND_STEEL.equals(id);
                }
            }
            return false;
        });
        registerInteraction(BlockKeys.PINK_CANDLE, (player, item, blockState, result) -> {
            if (blockState instanceof Candle candle) {
                Key id = item.vanillaId();
                if (!candle.isLit()) {
                    return ItemKeys.FLINT_AND_STEEL.equals(id);
                }
            }
            return false;
        });
        // 蛋糕
        registerInteraction(BlockKeys.CAKE, (player, item, blockState, result) -> {
            if (blockState instanceof Cake cake && cake.getBites() == 0 && item.hasItemTag(CANDLES)) return true;
            return canEat(player);
        });
        registerInteraction(BlockKeys.CANDLE_CAKE, (player, item, blockState, result) -> {
            if (blockState instanceof Lightable lightable && !(lightable.isLit())) {
                Key id = item.vanillaId();
                return ItemKeys.FLINT_AND_STEEL.equals(id);
            }
            return canEat(player);
        });
        registerInteraction(BlockKeys.WHITE_CANDLE_CAKE, (player, item, blockState, result) -> {
            if (blockState instanceof Lightable lightable && !(lightable.isLit())) {
                Key id = item.vanillaId();
                return ItemKeys.FLINT_AND_STEEL.equals(id);
            }
            return canEat(player);
        });
        registerInteraction(BlockKeys.LIGHT_GRAY_CANDLE_CAKE, (player, item, blockState, result) -> {
            if (blockState instanceof Lightable lightable && !(lightable.isLit())) {
                Key id = item.vanillaId();
                return ItemKeys.FLINT_AND_STEEL.equals(id);
            }
            return canEat(player);
        });
        registerInteraction(BlockKeys.GRAY_CANDLE_CAKE, (player, item, blockState, result) -> {
            if (blockState instanceof Lightable lightable && !(lightable.isLit())) {
                Key id = item.vanillaId();
                return ItemKeys.FLINT_AND_STEEL.equals(id);
            }
            return canEat(player);
        });
        registerInteraction(BlockKeys.BLACK_CANDLE_CAKE, (player, item, blockState, result) -> {
            if (blockState instanceof Lightable lightable && !(lightable.isLit())) {
                Key id = item.vanillaId();
                return ItemKeys.FLINT_AND_STEEL.equals(id);
            }
            return canEat(player);
        });
        registerInteraction(BlockKeys.BROWN_CANDLE_CAKE, (player, item, blockState, result) -> {
            if (blockState instanceof Lightable lightable && !(lightable.isLit())) {
                Key id = item.vanillaId();
                return ItemKeys.FLINT_AND_STEEL.equals(id);
            }
            return canEat(player);
        });
        registerInteraction(BlockKeys.RED_CANDLE_CAKE, (player, item, blockState, result) -> {
            if (blockState instanceof Lightable lightable && !(lightable.isLit())) {
                Key id = item.vanillaId();
                return ItemKeys.FLINT_AND_STEEL.equals(id);
            }
            return canEat(player);
        });
        registerInteraction(BlockKeys.ORANGE_CANDLE_CAKE, (player, item, blockState, result) -> {
            if (blockState instanceof Lightable lightable && !(lightable.isLit())) {
                Key id = item.vanillaId();
                return ItemKeys.FLINT_AND_STEEL.equals(id);
            }
            return canEat(player);
        });
        registerInteraction(BlockKeys.YELLOW_CANDLE_CAKE, (player, item, blockState, result) -> {
            if (blockState instanceof Lightable lightable && !(lightable.isLit())) {
                Key id = item.vanillaId();
                return ItemKeys.FLINT_AND_STEEL.equals(id);
            }
            return canEat(player);
        });
        registerInteraction(BlockKeys.LIME_CANDLE_CAKE, (player, item, blockState, result) -> {
            if (blockState instanceof Lightable lightable && !(lightable.isLit())) {
                Key id = item.vanillaId();
                return ItemKeys.FLINT_AND_STEEL.equals(id);
            }
            return canEat(player);
        });
        registerInteraction(BlockKeys.GREEN_CANDLE_CAKE, (player, item, blockState, result) -> {
            if (blockState instanceof Lightable lightable && !(lightable.isLit())) {
                Key id = item.vanillaId();
                return ItemKeys.FLINT_AND_STEEL.equals(id);
            }
            return canEat(player);
        });
        registerInteraction(BlockKeys.CYAN_CANDLE_CAKE, (player, item, blockState, result) -> {
            if (blockState instanceof Lightable lightable && !(lightable.isLit())) {
                Key id = item.vanillaId();
                return ItemKeys.FLINT_AND_STEEL.equals(id);
            }
            return canEat(player);
        });
        registerInteraction(BlockKeys.LIGHT_BLUE_CANDLE_CAKE, (player, item, blockState, result) -> {
            if (blockState instanceof Lightable lightable && !(lightable.isLit())) {
                Key id = item.vanillaId();
                return ItemKeys.FLINT_AND_STEEL.equals(id);
            }
            return canEat(player);
        });
        registerInteraction(BlockKeys.BLUE_CANDLE_CAKE, (player, item, blockState, result) -> {
            if (blockState instanceof Lightable lightable && !(lightable.isLit())) {
                Key id = item.vanillaId();
                return ItemKeys.FLINT_AND_STEEL.equals(id);
            }
            return canEat(player);
        });
        registerInteraction(BlockKeys.PURPLE_CANDLE_CAKE, (player, item, blockState, result) -> {
            if (blockState instanceof Lightable lightable && !(lightable.isLit())) {
                Key id = item.vanillaId();
                return ItemKeys.FLINT_AND_STEEL.equals(id);
            }
            return canEat(player);
        });
        registerInteraction(BlockKeys.MAGENTA_CANDLE_CAKE, (player, item, blockState, result) -> {
            if (blockState instanceof Lightable lightable && !(lightable.isLit())) {
                Key id = item.vanillaId();
                return ItemKeys.FLINT_AND_STEEL.equals(id);
            }
            return canEat(player);
        });
        registerInteraction(BlockKeys.PINK_CANDLE_CAKE, (player, item, blockState, result) -> {
            if (blockState instanceof Lightable lightable && !(lightable.isLit())) {
                Key id = item.vanillaId();
                return ItemKeys.FLINT_AND_STEEL.equals(id);
            }
            return canEat(player);
        });
        // 潜影盒
        registerInteraction(BlockKeys.SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WHITE_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.LIGHT_GRAY_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.GRAY_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BLACK_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BROWN_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.RED_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ORANGE_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.YELLOW_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.LIME_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.GREEN_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CYAN_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.LIGHT_BLUE_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BLUE_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PURPLE_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.MAGENTA_SHULKER_BOX, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PINK_SHULKER_BOX, (player, item, blockState, result) -> true);
        // 按钮
        registerInteraction(BlockKeys.OAK_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.SPRUCE_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BIRCH_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.JUNGLE_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ACACIA_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DARK_OAK_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.MANGROVE_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CHERRY_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PALE_OAK_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BAMBOO_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CRIMSON_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WARPED_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.STONE_BUTTON, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.POLISHED_BLACKSTONE_BUTTON, (player, item, blockState, result) -> true);
        // 门
        registerInteraction(BlockKeys.OAK_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.SPRUCE_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BIRCH_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.JUNGLE_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ACACIA_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DARK_OAK_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.MANGROVE_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CHERRY_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PALE_OAK_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BAMBOO_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CRIMSON_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WARPED_DOOR, (player, item, blockState, result) -> true);

        registerInteraction(BlockKeys.COPPER_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.EXPOSED_COPPER_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WEATHERED_COPPER_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.OXIDIZED_COPPER_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WAXED_COPPER_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WAXED_EXPOSED_COPPER_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WAXED_WEATHERED_COPPER_DOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WAXED_OXIDIZED_COPPER_DOOR, (player, item, blockState, result) -> true);
        // 活板门
        registerInteraction(BlockKeys.OAK_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.SPRUCE_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BIRCH_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.JUNGLE_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ACACIA_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DARK_OAK_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.MANGROVE_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CHERRY_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PALE_OAK_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BAMBOO_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CRIMSON_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WARPED_TRAPDOOR, (player, item, blockState, result) -> true);

        registerInteraction(BlockKeys.COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.EXPOSED_COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WEATHERED_COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.OXIDIZED_COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WAXED_COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WAXED_EXPOSED_COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WAXED_WEATHERED_COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WAXED_OXIDIZED_COPPER_TRAPDOOR, (player, item, blockState, result) -> true);
        // 栅栏门
        registerInteraction(BlockKeys.OAK_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.SPRUCE_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BIRCH_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.JUNGLE_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ACACIA_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DARK_OAK_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.MANGROVE_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CHERRY_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PALE_OAK_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BAMBOO_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CRIMSON_FENCE_GATE, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WARPED_FENCE_GATE, (player, item, blockState, result) -> true);
        // 告示牌
        registerInteraction(BlockKeys.OAK_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.SPRUCE_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BIRCH_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.JUNGLE_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ACACIA_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DARK_OAK_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.MANGROVE_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CHERRY_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PALE_OAK_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BAMBOO_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CRIMSON_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WARPED_SIGN, (player, item, blockState, result) -> true);
        // 靠墙告示牌
        registerInteraction(BlockKeys.OAK_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.SPRUCE_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BIRCH_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.JUNGLE_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ACACIA_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DARK_OAK_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.MANGROVE_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CHERRY_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PALE_OAK_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BAMBOO_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CRIMSON_WALL_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WARPED_WALL_SIGN, (player, item, blockState, result) -> true);
        // 悬挂式告示牌
        registerInteraction(BlockKeys.OAK_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.SPRUCE_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BIRCH_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.JUNGLE_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ACACIA_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DARK_OAK_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.MANGROVE_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CHERRY_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PALE_OAK_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BAMBOO_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CRIMSON_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WARPED_HANGING_SIGN, (player, item, blockState, result) -> true);
        // 靠墙悬挂式告示牌
        registerInteraction(BlockKeys.OAK_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.SPRUCE_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BIRCH_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.JUNGLE_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.ACACIA_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.DARK_OAK_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.MANGROVE_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CHERRY_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.PALE_OAK_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.BAMBOO_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.CRIMSON_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        registerInteraction(BlockKeys.WARPED_WALL_HANGING_SIGN, (player, item, blockState, result) -> true);
        // 展示柜
        registerInteraction(BlockKeys.OAK_SHELF, (player, item, blockState, result) -> blockState instanceof Directional directional && DirectionUtils.toBlockFace(result.direction()).equals(directional.getFacing()));
        registerInteraction(BlockKeys.SPRUCE_SHELF, (player, item, blockState, result) -> blockState instanceof Directional directional && DirectionUtils.toBlockFace(result.direction()).equals(directional.getFacing()));
        registerInteraction(BlockKeys.BIRCH_SHELF, (player, item, blockState, result) -> blockState instanceof Directional directional && DirectionUtils.toBlockFace(result.direction()).equals(directional.getFacing()));
        registerInteraction(BlockKeys.JUNGLE_SHELF, (player, item, blockState, result) -> blockState instanceof Directional directional && DirectionUtils.toBlockFace(result.direction()).equals(directional.getFacing()));
        registerInteraction(BlockKeys.ACACIA_SHELF, (player, item, blockState, result) -> blockState instanceof Directional directional && DirectionUtils.toBlockFace(result.direction()).equals(directional.getFacing()));
        registerInteraction(BlockKeys.DARK_OAK_SHELF, (player, item, blockState, result) -> blockState instanceof Directional directional && DirectionUtils.toBlockFace(result.direction()).equals(directional.getFacing()));
        registerInteraction(BlockKeys.MANGROVE_SHELF, (player, item, blockState, result) -> blockState instanceof Directional directional && DirectionUtils.toBlockFace(result.direction()).equals(directional.getFacing()));
        registerInteraction(BlockKeys.CHERRY_SHELF, (player, item, blockState, result) -> blockState instanceof Directional directional && DirectionUtils.toBlockFace(result.direction()).equals(directional.getFacing()));
        registerInteraction(BlockKeys.PALE_OAK_SHELF, (player, item, blockState, result) -> blockState instanceof Directional directional && DirectionUtils.toBlockFace(result.direction()).equals(directional.getFacing()));
        registerInteraction(BlockKeys.BAMBOO_SHELF, (player, item, blockState, result) -> blockState instanceof Directional directional && DirectionUtils.toBlockFace(result.direction()).equals(directional.getFacing()));
        registerInteraction(BlockKeys.CRIMSON_SHELF, (player, item, blockState, result) -> blockState instanceof Directional directional && DirectionUtils.toBlockFace(result.direction()).equals(directional.getFacing()));
        registerInteraction(BlockKeys.WARPED_SHELF, (player, item, blockState, result) -> blockState instanceof Directional directional && DirectionUtils.toBlockFace(result.direction()).equals(directional.getFacing()));
        // 铜傀儡雕像
        registerInteraction(BlockKeys.COPPER_GOLEM_STATUE, ((player, item, blockData, result) -> true));
        registerInteraction(BlockKeys.EXPOSED_COPPER_GOLEM_STATUE, ((player, item, blockData, result) -> true));
        registerInteraction(BlockKeys.WEATHERED_COPPER_GOLEM_STATUE, ((player, item, blockData, result) -> true));
        registerInteraction(BlockKeys.OXIDIZED_COPPER_GOLEM_STATUE, ((player, item, blockData, result) -> true));
        registerInteraction(BlockKeys.WAXED_COPPER_GOLEM_STATUE, ((player, item, blockData, result) -> true));
        registerInteraction(BlockKeys.WAXED_EXPOSED_COPPER_GOLEM_STATUE, ((player, item, blockData, result) -> true));
        registerInteraction(BlockKeys.WAXED_WEATHERED_COPPER_GOLEM_STATUE, ((player, item, blockData, result) -> true));
        registerInteraction(BlockKeys.WAXED_OXIDIZED_COPPER_GOLEM_STATUE, ((player, item, blockData, result) -> true));
        // 铜箱子
        registerInteraction(BlockKeys.COPPER_CHEST, ((player, item, blockData, result) -> true));
        registerInteraction(BlockKeys.EXPOSED_COPPER_CHEST, ((player, item, blockData, result) -> true));
        registerInteraction(BlockKeys.WEATHERED_COPPER_CHEST, ((player, item, blockData, result) -> true));
        registerInteraction(BlockKeys.OXIDIZED_COPPER_CHEST, ((player, item, blockData, result) -> true));
        registerInteraction(BlockKeys.WAXED_COPPER_CHEST, ((player, item, blockData, result) -> true));
        registerInteraction(BlockKeys.WAXED_EXPOSED_COPPER_CHEST, ((player, item, blockData, result) -> true));
        registerInteraction(BlockKeys.WAXED_WEATHERED_COPPER_CHEST, ((player, item, blockData, result) -> true));
        registerInteraction(BlockKeys.WAXED_OXIDIZED_COPPER_CHEST, ((player, item, blockData, result) -> true));
    }

    static {
        registerCanPlace(BlockKeys.CACTUS, (player, item, blockState, result) -> {
            Key id = item.vanillaId();
            return result.direction() == Direction.UP && ItemKeys.CACTUS.equals(id);
        });
        registerCanPlace(BlockKeys.SUGAR_CANE, (player, item, blockState, result) -> {
            Key id = item.vanillaId();
            return result.direction() == Direction.UP && ItemKeys.SUGAR_CANE.equals(id);
        });
    }

    // 实体
    static {
        registerEntityInteraction(EntityTypeKeys.BEE, (player, entity, item) -> canBeFeed(entity, item));
        registerEntityInteraction(EntityTypeKeys.FOX, (player, entity, item) -> canBeFeed(entity, item));
        registerEntityInteraction(EntityTypeKeys.FROG, (player, entity, item) -> canBeFeed(entity, item));
        registerEntityInteraction(EntityTypeKeys.PANDA, (player, entity, item) -> canBeFeed(entity, item));
        registerEntityInteraction(EntityTypeKeys.HOGLIN, (player, entity, item) -> canBeFeed(entity, item));
        registerEntityInteraction(EntityTypeKeys.OCELOT, (player, entity, item) -> canBeFeed(entity, item));
        registerEntityInteraction(EntityTypeKeys.RABBIT, (player, entity, item) -> canBeFeed(entity, item));
        registerEntityInteraction(EntityTypeKeys.TURTLE, (player, entity, item) -> canBeFeed(entity, item));
        registerEntityInteraction(EntityTypeKeys.CHICKEN, (player, entity, item) -> canBeFeed(entity, item));
        registerEntityInteraction(EntityTypeKeys.SNIFFER, (player, entity, item) -> canBeFeed(entity, item));
        registerEntityInteraction(EntityTypeKeys.DOLPHIN, (player, entity, item) -> {
            Key id = item.vanillaId();
            return ItemKeys.COD.equals(id) || ItemKeys.SALMON.equals(id);
        });

        registerEntityInteraction(EntityTypeKeys.AXOLOTL, (player, entity, item) -> {
            Key id = item.vanillaId();
            return canBeFeed(entity, item) || ItemKeys.WATER_BUCKET.equals(id);
        });
        registerEntityInteraction(EntityTypeKeys.COD, (player, entity, item) -> {
            Key id = item.vanillaId();
            return ItemKeys.WATER_BUCKET.equals(id);
        });
        registerEntityInteraction(EntityTypeKeys.SALMON, (player, entity, item) -> {
            Key id = item.vanillaId();
            return ItemKeys.WATER_BUCKET.equals(id);
        });
        registerEntityInteraction(EntityTypeKeys.TROPICAL_FISH, (player, entity, item) -> {
            Key id = item.vanillaId();
            return ItemKeys.WATER_BUCKET.equals(id);
        });
        registerEntityInteraction(EntityTypeKeys.PUFFERFISH, (player, entity, item) -> {
            Key id = item.vanillaId();
            return ItemKeys.WATER_BUCKET.equals(id);
        });
        registerEntityInteraction(EntityTypeKeys.TADPOLE, (player, entity, item) ->     {
            Key id = item.vanillaId();
            return ItemKeys.WATER_BUCKET.equals(id) || item.hasItemTag(FROG_FOOD);
        });

        registerEntityInteraction(EntityTypeKeys.SHEEP, (player, entity, item) -> {
            Key id = item.vanillaId();
            if (entity instanceof Sheep sheep && sheep.readyToBeSheared() && ArrayUtils.contains(ItemKeys.DYES, item)) {
                DyeColor sheepColor = sheep.getColor();
                if (sheepColor != null) {
                    String color = sheepColor.name().toLowerCase(Locale.ROOT);
                    return !Key.of(color + "_dye").equals(id);
                }
            }
            return canBeFeed(entity, item) || canBeSheared(entity, item);
        });
        registerEntityInteraction(EntityTypeKeys.MOOSHROOM, (player, entity, item) -> {
            Key id = item.vanillaId();
            return canBeFeed(entity, item, "cow_food") || canBeSheared(entity, item) || ItemKeys.BUCKET.equals(id) || ItemKeys.BOWL.equals(id);
        });
        registerEntityInteraction(EntityTypeKeys.BOGGED, (player, entity, item) -> canBeSheared(entity, item));
        registerEntityInteraction(EntityTypeKeys.SNOW_GOLEM, (player, entity, item) -> canBeSheared(entity, item));

        registerEntityInteraction(EntityTypeKeys.COW, (player, entity, item) -> {
            Key id = item.vanillaId();
            return canBeFeed(entity, item) || ItemKeys.BUCKET.equals(id);
        });
        registerEntityInteraction(EntityTypeKeys.GOAT, (player, entity, item) -> {
            Key id = item.vanillaId();
            return canBeFeed(entity, item) || ItemKeys.BUCKET.equals(id);
        });

        registerEntityInteraction(EntityTypeKeys.CREEPER, (player, entity, item) -> {
            Optional<List<ItemBehavior>> behaviors = item.getItemBehavior();
            if (behaviors.isPresent()) {
                for (ItemBehavior behavior : behaviors.get()) {
                    if (behavior instanceof FlintAndSteelItemBehavior) return true;
                }
            }
            return false;
        });
        registerEntityInteraction(EntityTypeKeys.PIGLIN, (player, entity, item) -> {
            Key id = item.vanillaId();
            return ItemKeys.GOLD_INGOT.equals(id);
        });
        registerEntityInteraction(EntityTypeKeys.ARMADILLO, (player, entity, item) -> {
            Key id = item.vanillaId();
            return canBeFeed(entity, item) || ItemKeys.BRUSH.equals(id);
        });

        registerEntityInteraction(EntityTypeKeys.WOLF, (player, entity, item) -> canBeFeed(entity, item) || isPetOwner(player, entity));
        registerEntityInteraction(EntityTypeKeys.CAT, (player, entity, item) -> canBeFeed(entity, item) || isPetOwner(player, entity));
        registerEntityInteraction(EntityTypeKeys.PARROT, (player, entity, item) -> {
            if (item.hasItemTag(PARROT_POISONOUS_FOOD)) return true;
            return canBeFeed(entity, item) || isPetOwner(player, entity);
        });

        registerEntityInteraction(EntityTypeKeys.MINECART, (player, entity, item) -> !player.isSneaking());
        registerEntityInteraction(EntityTypeKeys.CHEST_MINECART, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.FURNACE_MINECART, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.HOPPER_MINECART, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.COMMAND_BLOCK_MINECART, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.SPAWNER_MINECART, (player, entity, item) -> {
            Key id = item.vanillaId();
            return id.asString().endsWith("_spawn_egg");
        });

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

        registerEntityInteraction(EntityTypeKeys.HORSE, (player, entity, item) -> {
            if (!isAdult(entity)) return true;
            if (isFood(entity, item)) {
                return canBeFeed(entity, item);
            }
            return rideable(entity);
        });
        registerEntityInteraction(EntityTypeKeys.DONKEY, (player, entity, item) -> {
            if (!isAdult(entity)) return true;
            if (isFood("horse_food", item)) {
                return canBeFeed(entity, item, "horse_food");
            }
            return rideable(entity);
        });
        registerEntityInteraction(EntityTypeKeys.MULE, (player, entity, item) -> {
            if (!isAdult(entity)) return true;
            if (isFood("horse_food", item)) {
                return canBeFeed(entity, item, "horse_food");
            }
            return rideable(entity);
        });
        registerEntityInteraction(EntityTypeKeys.LLAMA, (player, entity, item) -> {
            if (!isAdult(entity)) return true;
            if (isFood(entity, item)) {
                return canBeFeed(entity, item);
            }
            return rideable(entity);
        });
        registerEntityInteraction(EntityTypeKeys.TRADER_LLAMA, (player, entity, item) -> {
            if (!isAdult(entity)) return true;
            if (isFood("llama_food", item)) {
                return canBeFeed(entity, item, "llama_food");
            }
            return rideable(entity);
        });
        registerEntityInteraction(EntityTypeKeys.CAMEL, (player, entity, item) -> {
            if (!isAdult(entity)) return true;
            if (isFood(entity, item)) {
                return canBeFeed(entity, item);
            }
            return true;
        });
        registerEntityInteraction(EntityTypeKeys.ZOMBIE_HORSE, (player, entity, item) -> isTamed(entity) && rideable(entity));
        registerEntityInteraction(EntityTypeKeys.SKELETON_HORSE, (player, entity, item) -> isTamed(entity) && rideable(entity));
        registerEntityInteraction(EntityTypeKeys.PIG, (player, entity, item) -> {
            Key id = item.vanillaId();
            return canBeFeed(entity, item)
                    || (ItemKeys.SADDLE.equals(id) && !hasSaddle(player, entity) && isAdult(entity))
                    || (hasSaddle(player, entity) && !player.isSneaking() && rideable(entity));
        });
        registerEntityInteraction(EntityTypeKeys.STRIDER, (player, entity, item) -> {
            Key id = item.vanillaId();
            return canBeFeed(entity, item)
                    || (ItemKeys.SADDLE.equals(id) && !hasSaddle(player, entity) && isAdult(entity))
                    || (hasSaddle(player, entity) && !player.isSneaking() && rideable(entity));
        });
        registerEntityInteraction(EntityTypeKeys.HAPPY_GHAST, (player, entity, item) -> {
            if (entity instanceof HappyGhast happyGhast && isAdult(entity)) {
                ItemStack bodyItem = happyGhast.getEquipment().getItem(EquipmentSlot.BODY);
                boolean hasHarness = BukkitItemManager.instance().wrap(bodyItem).hasItemTag(HARNESSES);
                if (item.hasItemTag(HARNESSES) && !hasHarness) return true;
                return !player.isSneaking();
            }
            return canBeFeed(entity, item);
        });

        registerEntityInteraction(EntityTypeKeys.ALLAY, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.VILLAGER, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.WANDERING_TRADER, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.ITEM_FRAME, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.GLOW_ITEM_FRAME, (player, entity, item) -> true);
        registerEntityInteraction(EntityTypeKeys.INTERACTION, (player, entity, item) -> true);
    }

    private static void registerInteraction(Key key, QuadFunction<org.bukkit.entity.Player, Item<ItemStack>, BlockData, BlockHitResult, Boolean> function) {
        var previous = INTERACTIONS.put(key, function);
        if (previous != null) {
            CraftEngine.instance().logger().warn("Duplicated interaction check: " + key);
        }
    }

    private static void registerCanPlace(Key key, QuadFunction<org.bukkit.entity.Player, Item<ItemStack>, BlockData, BlockHitResult, Boolean> function) {
        var previous = CAN_PLACE.put(key, function);
        if (previous != null) {
            CraftEngine.instance().logger().warn("Duplicated can place check: " + key);
        }
    }

    private static void registerEntityInteraction(Key key, TriFunction<Player, Entity, Item<ItemStack>, Boolean> function) {
        var previous = ENTITY_INTERACTIONS.put(key, function);
        if (previous != null) {
            CraftEngine.instance().logger().warn("Duplicated entity interaction check: " + key);
        }
    }

    public static boolean isInteractable(Player player, BlockData state, BlockHitResult hit, @Nullable Item<ItemStack> item) {
        Key blockType = BlockStateUtils.getBlockOwnerIdFromData(state);
        if (INTERACTIONS.containsKey(blockType)) {
            return INTERACTIONS.get(blockType).apply(player, item, state, hit);
        }
        return false;
    }

    // 这个方法用于解决玩家使用仙人掌放在基于仙人掌的方块上，物品暂时消失的类似问题，但是无法彻底解决
    // todo 需要通过创建代理Level来实现getBlockState的方法拦截，从而实现模拟客户端测的方块状态更新，这个过程可能也需要创建代理Chunk和代理Section
    public static boolean canPlaceVisualBlock(Player player, BlockData state, BlockHitResult hit, @Nullable Item<ItemStack> item) {
        if (item == null) return false;
        Key blockType = BlockStateUtils.getBlockOwnerIdFromData(state);
        if (CAN_PLACE.containsKey(blockType)) {
            return CAN_PLACE.get(blockType).apply(player, item, state, hit);
        }
        return false;
    }

    public static boolean isEntityInteractable(Player player, Entity entity, @Nullable Item<ItemStack> item) {
        TriFunction<Player, Entity, Item<ItemStack>, Boolean> func = ENTITY_INTERACTIONS.get(EntityUtils.getEntityType(entity));
        return func != null && func.apply(player, entity, item);
    }

    private static boolean canEat(Player player) {
        return player.isInvulnerable() || player.getFoodLevel() < 20;
    }


    private static boolean isFood(Entity entity, Item<ItemStack> item) {
        String entityType = EntityUtils.getEntityType(entity).value();
        return isFood(entityType + "_food", item);
    }

    private static boolean isFood(String food, Item<ItemStack> item) {
        return item.hasItemTag(Key.of(food));
    }

    private static boolean canBeFeed(Entity entity, Item<ItemStack> item) {
        return canBeFeed(entity, item, null);
    }

    private static boolean canBeFeed(Entity entity, Item<ItemStack> item, String food) {
        boolean isFood = food != null ? isFood(food, item) : isFood(entity, item);
        if (!isFood) return false;
        if (entity instanceof Tameable) {
            if (!isFullHealth(entity)) return true;
            if (entity instanceof AbstractHorse && !isFullTemper(entity)) return true;
            return !isInLove(entity) && isTamed(entity);
        }
        return !isInLove(entity);
    }

    private static boolean isTamed(Entity entity) {
        return entity instanceof Tameable tameable && tameable.isTamed();
    }

    private static boolean isInLove(Entity entity) {
        if (entity instanceof Animals animals) {
            return animals.isLoveMode() || !animals.canBreed();
        }
        return entity instanceof Ageable ageable && ageable.getAge() > 0;
    }

    private static boolean isFullTemper(Entity entity) {
        return entity instanceof AbstractHorse horse && horse.getDomestication() == horse.getMaxDomestication();
    }

    public static boolean isFullHealth(Entity entity) {
        if (entity instanceof LivingEntity living) {
            Key key = AttributeModifiersProcessor.getNativeAttributeName(Key.of("max_health"));
            Attribute maxHealthAttr = Registry.ATTRIBUTE.get(KeyUtils.toNamespacedKey(key));
            if (maxHealthAttr == null) return false;
            AttributeInstance attribute = living.getAttribute(maxHealthAttr);
            return attribute != null && living.getHealth() >= attribute.getValue();
        }
        return false;
    }

    private static boolean isAdult(Entity entity) {
        return entity instanceof Ageable ageable && ageable.isAdult();
    }

    private static boolean isPetOwner(Player player, Entity entity) {
        return entity instanceof Tameable tameable && tameable.isTamed() && player.getUniqueId().equals(tameable.getOwnerUniqueId());
    }

    // 判断单座位实体是否载有乘客
    private static boolean rideable(Entity entity) {
        return entity.isEmpty();
    }

    private static boolean hasSaddle(Player player, Entity entity) {
        return entity instanceof Steerable steerable && steerable.hasSaddle() && !player.isSneaking();
    }

    private static boolean canBeSheared(Entity entity, Item<ItemStack> item) {
        Key id = item.vanillaId();
        return entity instanceof Shearable shearable && shearable.readyToBeSheared() && ItemKeys.SHEARS.equals(id);
    }

    public static boolean canPlaceBlock(BlockPlaceContext context) {
        Object item = FastNMS.INSTANCE.method$ItemStack$getItem(context.getItem().getLiteralObject());
        Object block = FastNMS.INSTANCE.method$BlockItem$getBlock(item);
        Object stateToPlace = FastNMS.INSTANCE.method$Block$getStateForPlacement(block, toNMSBlockPlaceContext(context));
        return FastNMS.INSTANCE.method$BlockStateBase$canSurvive(stateToPlace, context.getLevel().serverWorld(), LocationUtils.toBlockPos(context.getClickedPos()));
    }

    private static Object toNMSHitResult(BlockHitResult result) {
        return FastNMS.INSTANCE.constructor$BlockHitResult(
                LocationUtils.toVec(result.location()),
                DirectionUtils.toNMSDirection(result.direction()),
                LocationUtils.toBlockPos(result.blockPos()),
                result.isInside()
        );
    }

    private static Object toNMSBlockPlaceContext(BlockPlaceContext context) {
        return FastNMS.INSTANCE.constructor$BlockPlaceContext(
                context.getLevel().serverWorld(),
                Optional.ofNullable(context.getPlayer()).map(net.momirealms.craftengine.core.entity.player.Player::serverPlayer).orElse(null),
                context.getHand() == InteractionHand.MAIN_HAND ? CoreReflections.instance$InteractionHand$MAIN_HAND : CoreReflections.instance$InteractionHand$OFF_HAND,
                context.getItem().getLiteralObject(),
                toNMSHitResult(context.getHitResult())
        );
    }
}
