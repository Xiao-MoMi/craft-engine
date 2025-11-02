package net.momirealms.craftengine.core.block;

import net.momirealms.craftengine.core.util.Key;

import java.util.List;

public final class BlockKeys {
    private BlockKeys() {
    }

    // 特殊
    public static final Key AIR = Key.of("minecraft:air");

    public static final Key SUGAR_CANE = Key.of("minecraft:sugar_cane");
    public static final Key NOTE_BLOCK = Key.of("minecraft:note_block");
    public static final Key TRIPWIRE = Key.of("minecraft:tripwire");
    public static final Key CACTUS = Key.of("minecraft:cactus");
    public static final Key POWDER_SNOW = Key.of("minecraft:powder_snow");
    // 功能方块
    public static final Key CRAFTING_TABLE = Key.of("minecraft:crafting_table");
    public static final Key STONECUTTER = Key.of("minecraft:stonecutter");
    public static final Key CARTOGRAPHY_TABLE = Key.of("minecraft:cartography_table");
    public static final Key SMITHING_TABLE = Key.of("minecraft:smithing_table");
    public static final Key GRINDSTONE = Key.of("minecraft:grindstone");
    public static final Key LOOM = Key.of("minecraft:loom");
    public static final Key FURNACE = Key.of("minecraft:furnace");
    public static final Key SMOKER = Key.of("minecraft:smoker");
    public static final Key BLAST_FURNACE = Key.of("minecraft:blast_furnace");
    public static final Key CAMPFIRE = Key.of("minecraft:campfire");
    public static final Key SOUL_CAMPFIRE = Key.of("minecraft:soul_campfire");
    public static final Key ANVIL = Key.of("minecraft:anvil");
    public static final Key CHIPPED_ANVIL = Key.of("minecraft:chipped_anvil");
    public static final Key DAMAGED_ANVIL = Key.of("minecraft:damaged_anvil");
    public static final Key COMPOSTER = Key.of("minecraft:composter");
    public static final Key JUKEBOX = Key.of("minecraft:jukebox");
    public static final Key ENCHANTING_TABLE = Key.of("minecraft:enchanting_table");
    public static final Key BREWING_STAND = Key.of("minecraft:brewing_stand");
    public static final Key CAULDRON = Key.of("minecraft:cauldron");
    public static final Key LAVA_CAULDRON = Key.of("minecraft:lava_cauldron");
    public static final Key WATER_CAULDRON = Key.of("minecraft:water_cauldron");
    public static final Key BELL = Key.of("minecraft:bell");
    public static final Key BEACON = Key.of("minecraft:beacon");
    public static final Key BEE_NEST = Key.of("minecraft:bee_nest");
    public static final Key BEEHIVE = Key.of("minecraft:beehive");
    public static final Key FLOWER_POT = Key.of("minecraft:flower_pot");
    public static final Key DECORATED_POT = Key.of("minecraft:decorated_pot");
    public static final Key CHISELED_BOOKSHELF = Key.of("minecraft:chiseled_bookshelf");
    public static final Key LECTERN = Key.of("minecraft:lectern");
    public static final Key FARMLAND = Key.of("minecraft:farmland");

    public static final Key RESPAWN_ANCHOR = Key.of("minecraft:respawn_anchor");
    public static final Key DRAGON_EGG = Key.of("minecraft:dragon_egg");
    public static final Key END_PORTAL_FRAME = Key.of("minecraft:end_portal_frame");
    public static final Key VAULT = Key.of("minecraft:vault");

    public static final Key SPAWNER = Key.of("minecraft:spawner");
    public static final Key TRIAL_SPAWNER = Key.of("minecraft:trial_spawner");
    // 箱子容器
    public static final Key CHEST = Key.of("minecraft:chest");
    public static final Key BARREL = Key.of("minecraft:barrel");
    public static final Key ENDER_CHEST = Key.of("minecraft:ender_chest");
    public static final Key TRAPPED_CHEST = Key.of("minecraft:trapped_chest");

    public static final Key COPPER_CHEST = Key.of("minecraft:copper_chest");
    public static final Key EXPOSED_COPPER_CHEST = Key.of("minecraft:exposed_copper_chest");
    public static final Key WEATHERED_COPPER_CHEST = Key.of("minecraft:weathered_copper_chest");
    public static final Key OXIDIZED_COPPER_CHEST = Key.of("minecraft:oxidized_copper_chest");
    public static final Key WAXED_COPPER_CHEST = Key.of("minecraft:waxed_copper_chest");
    public static final Key WAXED_EXPOSED_COPPER_CHEST = Key.of("minecraft:waxed_exposed_copper_chest");
    public static final Key WAXED_WEATHERED_COPPER_CHEST = Key.of("minecraft:waxed_weathered_copper_chest");
    public static final Key WAXED_OXIDIZED_COPPER_CHEST = Key.of("minecraft:waxed_oxidized_copper_chest");
    // 红石方块
    public static final Key REDSTONE_WIRE = Key.of("minecraft:redstone_wire");
    public static final Key REDSTONE_TORCH = Key.of("minecraft:redstone_torch");
    public static final Key REDSTONE_BLOCK = Key.of("minecraft:redstone_block");
    public static final Key REPEATER = Key.of("minecraft:repeater");
    public static final Key COMPARATOR = Key.of("minecraft:comparator");
    public static final Key TARGET = Key.of("minecraft:target");
    public static final Key LEVER = Key.of("minecraft:lever");
    public static final Key SCULK_SENSOR = Key.of("minecraft:sculk_sensor");
    public static final Key CALIBRATED_SCULK_SENSOR = Key.of("minecraft:calibrated_sculk_sensor");
    public static final Key TRIPWIRE_HOOK = Key.of("minecraft:tripwire_hook");
    public static final Key DAYLIGHT_DETECTOR = Key.of("minecraft:daylight_detector");
    public static final Key LIGHTNING_ROD = Key.of("minecraft:lightning_rod");
    public static final Key DISPENSER = Key.of("minecraft:dispenser");
    public static final Key DROPPER = Key.of("minecraft:dropper");
    public static final Key CRAFTER = Key.of("minecraft:crafter");
    public static final Key HOPPER = Key.of("minecraft:hopper");
    public static final Key OBSERVER = Key.of("minecraft:observer");
    public static final Key DETECTOR_RAIL = Key.of("minecraft:detector_rail");
    public static final Key TNT = Key.of("minecraft:tnt");
    public static final Key REDSTONE_ORE = Key.of("minecraft:redstone_ore");
    public static final Key DEEPSLATE_REDSTONE_ORE = Key.of("minecraft:deepslate_redstone_ore");
    // 按钮
    public static final Key OAK_BUTTON = Key.of("minecraft:oak_button");
    public static final Key SPRUCE_BUTTON = Key.of("minecraft:spruce_button");
    public static final Key BIRCH_BUTTON = Key.of("minecraft:birch_button");
    public static final Key JUNGLE_BUTTON = Key.of("minecraft:jungle_button");
    public static final Key ACACIA_BUTTON = Key.of("minecraft:acacia_button");
    public static final Key DARK_OAK_BUTTON = Key.of("minecraft:dark_oak_button");
    public static final Key MANGROVE_BUTTON = Key.of("minecraft:mangrove_button");
    public static final Key CHERRY_BUTTON = Key.of("minecraft:cherry_button");
    public static final Key PALE_OAK_BUTTON = Key.of("minecraft:pale_oak_button");
    public static final Key BAMBOO_BUTTON = Key.of("minecraft:bamboo_button");
    public static final Key CRIMSON_BUTTON = Key.of("minecraft:crimson_button");
    public static final Key WARPED_BUTTON = Key.of("minecraft:warped_button");
    public static final Key STONE_BUTTON = Key.of("minecraft:stone_button");
    public static final Key POLISHED_BLACKSTONE_BUTTON = Key.of("minecraft:polished_blackstone_button");
    // 压力板
    public static final Key OAK_PRESSURE_PLATE = Key.of("minecraft:oak_pressure_plate");
    public static final Key SPRUCE_PRESSURE_PLATE = Key.of("minecraft:spruce_pressure_plate");
    public static final Key BIRCH_PRESSURE_PLATE = Key.of("minecraft:birch_pressure_plate");
    public static final Key JUNGLE_PRESSURE_PLATE = Key.of("minecraft:jungle_pressure_plate");
    public static final Key ACACIA_PRESSURE_PLATE = Key.of("minecraft:acacia_pressure_plate");
    public static final Key DARK_OAK_PRESSURE_PLATE = Key.of("minecraft:dark_oak_pressure_plate");
    public static final Key MANGROVE_PRESSURE_PLATE = Key.of("minecraft:mangrove_pressure_plate");
    public static final Key CHERRY_PRESSURE_PLATE = Key.of("minecraft:cherry_pressure_plate");
    public static final Key PALE_OAK_PRESSURE_PLATE = Key.of("minecraft:pale_oak_pressure_plate");
    public static final Key BAMBOO_PRESSURE_PLATE = Key.of("minecraft:bamboo_pressure_plate");
    public static final Key CRIMSON_PRESSURE_PLATE = Key.of("minecraft:crimson_pressure_plate");
    public static final Key WARPED_PRESSURE_PLATE = Key.of("minecraft:warped_pressure_plate");
    public static final Key STONE_PRESSURE_PLATE = Key.of("minecraft:stone_pressure_plate");
    public static final Key POLISHED_BLACKSTONE_PRESSURE_PLATE = Key.of("minecraft:polished_blackstone_pressure_plate");
    public static final Key LIGHT_WEIGHTED_PRESSURE_PLATE = Key.of("minecraft:light_weighted_pressure_plate");
    public static final Key HEAVY_WEIGHTED_PRESSURE_PLATE = Key.of("minecraft:heavy_weighted_pressure_plate");
    // 管理员用品
    public static final Key COMMAND_BLOCK = Key.of("minecraft:command_block");
    public static final Key CHAIN_COMMAND_BLOCK = Key.of("minecraft:chain_command_block");
    public static final Key REPEATING_COMMAND_BLOCK = Key.of("minecraft:repeating_command_block");
    public static final Key JIGSAW = Key.of("minecraft:jigsaw");
    public static final Key STRUCTURE_BLOCK = Key.of("minecraft:structure_block");
    public static final Key TEST_INSTANCE_BLOCK = Key.of("minecraft:test_instance_block");
    public static final Key TEST_BLOCK = Key.of("minecraft:test_block");
    public static final Key LIGHT = Key.of("minecraft:light");
    // 门
    public static final Key OAK_DOOR = Key.of("minecraft:oak_door");
    public static final Key SPRUCE_DOOR = Key.of("minecraft:spruce_door");
    public static final Key BIRCH_DOOR = Key.of("minecraft:birch_door");
    public static final Key JUNGLE_DOOR = Key.of("minecraft:jungle_door");
    public static final Key ACACIA_DOOR = Key.of("minecraft:acacia_door");
    public static final Key DARK_OAK_DOOR = Key.of("minecraft:dark_oak_door");
    public static final Key MANGROVE_DOOR = Key.of("minecraft:mangrove_door");
    public static final Key CHERRY_DOOR = Key.of("minecraft:cherry_door");
    public static final Key PALE_OAK_DOOR = Key.of("minecraft:pale_oak_door");
    public static final Key BAMBOO_DOOR = Key.of("minecraft:bamboo_door");
    public static final Key CRIMSON_DOOR = Key.of("minecraft:crimson_door");
    public static final Key WARPED_DOOR = Key.of("minecraft:warped_door");
    public static final Key IRON_DOOR = Key.of("minecraft:iron_door");

    public static final Key COPPER_DOOR = Key.of("minecraft:copper_door");
    public static final Key EXPOSED_COPPER_DOOR = Key.of("minecraft:exposed_copper_door");
    public static final Key WEATHERED_COPPER_DOOR = Key.of("minecraft:weathered_copper_door");
    public static final Key OXIDIZED_COPPER_DOOR = Key.of("minecraft:oxidized_copper_door");
    public static final Key WAXED_COPPER_DOOR = Key.of("minecraft:waxed_copper_door");
    public static final Key WAXED_EXPOSED_COPPER_DOOR = Key.of("minecraft:waxed_exposed_copper_door");
    public static final Key WAXED_WEATHERED_COPPER_DOOR = Key.of("minecraft:waxed_weathered_copper_door");
    public static final Key WAXED_OXIDIZED_COPPER_DOOR = Key.of("minecraft:waxed_oxidized_copper_door");
    // 活板门
    public static final Key OAK_TRAPDOOR = Key.of("minecraft:oak_trapdoor");
    public static final Key SPRUCE_TRAPDOOR = Key.of("minecraft:spruce_trapdoor");
    public static final Key BIRCH_TRAPDOOR = Key.of("minecraft:birch_trapdoor");
    public static final Key JUNGLE_TRAPDOOR = Key.of("minecraft:jungle_trapdoor");
    public static final Key ACACIA_TRAPDOOR = Key.of("minecraft:acacia_trapdoor");
    public static final Key DARK_OAK_TRAPDOOR = Key.of("minecraft:dark_oak_trapdoor");
    public static final Key MANGROVE_TRAPDOOR = Key.of("minecraft:mangrove_trapdoor");
    public static final Key CHERRY_TRAPDOOR = Key.of("minecraft:cherry_trapdoor");
    public static final Key PALE_OAK_TRAPDOOR = Key.of("minecraft:pale_oak_trapdoor");
    public static final Key BAMBOO_TRAPDOOR = Key.of("minecraft:bamboo_trapdoor");
    public static final Key CRIMSON_TRAPDOOR = Key.of("minecraft:crimson_trapdoor");
    public static final Key WARPED_TRAPDOOR = Key.of("minecraft:warped_trapdoor");
    public static final Key IRON_TRAPDOOR = Key.of("minecraft:iron_trapdoor");

    public static final Key COPPER_TRAPDOOR = Key.of("minecraft:copper_trapdoor");
    public static final Key EXPOSED_COPPER_TRAPDOOR = Key.of("minecraft:exposed_copper_trapdoor");
    public static final Key WEATHERED_COPPER_TRAPDOOR = Key.of("minecraft:weathered_copper_trapdoor");
    public static final Key OXIDIZED_COPPER_TRAPDOOR = Key.of("minecraft:oxidized_copper_trapdoor");
    public static final Key WAXED_COPPER_TRAPDOOR = Key.of("minecraft:waxed_copper_trapdoor");
    public static final Key WAXED_EXPOSED_COPPER_TRAPDOOR = Key.of("minecraft:waxed_exposed_copper_trapdoor");
    public static final Key WAXED_WEATHERED_COPPER_TRAPDOOR = Key.of("minecraft:waxed_weathered_copper_trapdoor");
    public static final Key WAXED_OXIDIZED_COPPER_TRAPDOOR = Key.of("minecraft:waxed_oxidized_copper_trapdoor");
    // 栅栏门
    public static final Key OAK_FENCE_GATE = Key.of("minecraft:oak_fence_gate");
    public static final Key SPRUCE_FENCE_GATE = Key.of("minecraft:spruce_fence_gate");
    public static final Key BIRCH_FENCE_GATE = Key.of("minecraft:birch_fence_gate");
    public static final Key JUNGLE_FENCE_GATE = Key.of("minecraft:jungle_fence_gate");
    public static final Key ACACIA_FENCE_GATE = Key.of("minecraft:acacia_fence_gate");
    public static final Key DARK_OAK_FENCE_GATE = Key.of("minecraft:dark_oak_fence_gate");
    public static final Key MANGROVE_FENCE_GATE = Key.of("minecraft:mangrove_fence_gate");
    public static final Key CHERRY_FENCE_GATE = Key.of("minecraft:cherry_fence_gate");
    public static final Key PALE_OAK_FENCE_GATE = Key.of("minecraft:pale_oak_fence_gate");
    public static final Key BAMBOO_FENCE_GATE = Key.of("minecraft:bamboo_fence_gate");
    public static final Key CRIMSON_FENCE_GATE = Key.of("minecraft:crimson_fence_gate");
    public static final Key WARPED_FENCE_GATE = Key.of("minecraft:warped_fence_gate");
    // 床
    public static final Key WHITE_BED = Key.of("minecraft:white_bed");
    public static final Key LIGHT_GRAY_BED = Key.of("minecraft:light_gray_bed");
    public static final Key GRAY_BED = Key.of("minecraft:gray_bed");
    public static final Key BLACK_BED = Key.of("minecraft:black_bed");
    public static final Key BROWN_BED = Key.of("minecraft:brown_bed");
    public static final Key RED_BED = Key.of("minecraft:red_bed");
    public static final Key ORANGE_BED = Key.of("minecraft:orange_bed");
    public static final Key YELLOW_BED = Key.of("minecraft:yellow_bed");
    public static final Key LIME_BED = Key.of("minecraft:lime_bed");
    public static final Key GREEN_BED = Key.of("minecraft:green_bed");
    public static final Key CYAN_BED = Key.of("minecraft:cyan_bed");
    public static final Key LIGHT_BLUE_BED = Key.of("minecraft:light_blue_bed");
    public static final Key BLUE_BED = Key.of("minecraft:blue_bed");
    public static final Key PURPLE_BED = Key.of("minecraft:purple_bed");
    public static final Key MAGENTA_BED = Key.of("minecraft:magenta_bed");
    public static final Key PINK_BED = Key.of("minecraft:pink_bed");
    // 蜡烛
    public static final Key CANDLE = Key.of("minecraft:candle");
    public static final Key WHITE_CANDLE = Key.of("minecraft:white_candle");
    public static final Key LIGHT_GRAY_CANDLE = Key.of("minecraft:light_gray_candle");
    public static final Key GRAY_CANDLE = Key.of("minecraft:gray_candle");
    public static final Key BLACK_CANDLE = Key.of("minecraft:black_candle");
    public static final Key BROWN_CANDLE = Key.of("minecraft:brown_candle");
    public static final Key RED_CANDLE = Key.of("minecraft:red_candle");
    public static final Key ORANGE_CANDLE = Key.of("minecraft:orange_candle");
    public static final Key YELLOW_CANDLE = Key.of("minecraft:yellow_candle");
    public static final Key LIME_CANDLE = Key.of("minecraft:lime_candle");
    public static final Key GREEN_CANDLE = Key.of("minecraft:green_candle");
    public static final Key CYAN_CANDLE = Key.of("minecraft:cyan_candle");
    public static final Key LIGHT_BLUE_CANDLE = Key.of("minecraft:light_blue_candle");
    public static final Key BLUE_CANDLE = Key.of("minecraft:blue_candle");
    public static final Key PURPLE_CANDLE = Key.of("minecraft:purple_candle");
    public static final Key MAGENTA_CANDLE = Key.of("minecraft:magenta_candle");
    public static final Key PINK_CANDLE = Key.of("minecraft:pink_candle");
    // 蛋糕
    public static final Key CAKE = Key.of("minecraft:cake");
    public static final Key CANDLE_CAKE = Key.of("minecraft:candle_cake");
    public static final Key WHITE_CANDLE_CAKE = Key.of("minecraft:white_candle_cake");
    public static final Key LIGHT_GRAY_CANDLE_CAKE = Key.of("minecraft:light_gray_candle_cake");
    public static final Key GRAY_CANDLE_CAKE = Key.of("minecraft:gray_candle_cake");
    public static final Key BLACK_CANDLE_CAKE = Key.of("minecraft:black_candle_cake");
    public static final Key BROWN_CANDLE_CAKE = Key.of("minecraft:brown_candle_cake");
    public static final Key RED_CANDLE_CAKE = Key.of("minecraft:red_candle_cake");
    public static final Key ORANGE_CANDLE_CAKE = Key.of("minecraft:orange_candle_cake");
    public static final Key YELLOW_CANDLE_CAKE = Key.of("minecraft:yellow_candle_cake");
    public static final Key LIME_CANDLE_CAKE = Key.of("minecraft:lime_candle_cake");
    public static final Key GREEN_CANDLE_CAKE = Key.of("minecraft:green_candle_cake");
    public static final Key CYAN_CANDLE_CAKE = Key.of("minecraft:cyan_candle_cake");
    public static final Key LIGHT_BLUE_CANDLE_CAKE = Key.of("minecraft:light_blue_candle_cake");
    public static final Key BLUE_CANDLE_CAKE = Key.of("minecraft:blue_candle_cake");
    public static final Key PURPLE_CANDLE_CAKE = Key.of("minecraft:purple_candle_cake");
    public static final Key MAGENTA_CANDLE_CAKE = Key.of("minecraft:magenta_candle_cake");
    public static final Key PINK_CANDLE_CAKE = Key.of("minecraft:pink_candle_cake");
    // 潜影盒
    public static final Key SHULKER_BOX = Key.of("minecraft:shulker_box");
    public static final Key WHITE_SHULKER_BOX = Key.of("minecraft:white_shulker_box");
    public static final Key LIGHT_GRAY_SHULKER_BOX = Key.of("minecraft:light_gray_shulker_box");
    public static final Key GRAY_SHULKER_BOX = Key.of("minecraft:gray_shulker_box");
    public static final Key BLACK_SHULKER_BOX = Key.of("minecraft:black_shulker_box");
    public static final Key BROWN_SHULKER_BOX = Key.of("minecraft:brown_shulker_box");
    public static final Key RED_SHULKER_BOX = Key.of("minecraft:red_shulker_box");
    public static final Key ORANGE_SHULKER_BOX = Key.of("minecraft:orange_shulker_box");
    public static final Key YELLOW_SHULKER_BOX = Key.of("minecraft:yellow_shulker_box");
    public static final Key LIME_SHULKER_BOX = Key.of("minecraft:lime_shulker_box");
    public static final Key GREEN_SHULKER_BOX = Key.of("minecraft:green_shulker_box");
    public static final Key CYAN_SHULKER_BOX = Key.of("minecraft:cyan_shulker_box");
    public static final Key LIGHT_BLUE_SHULKER_BOX = Key.of("minecraft:light_blue_shulker_box");
    public static final Key BLUE_SHULKER_BOX = Key.of("minecraft:blue_shulker_box");
    public static final Key PURPLE_SHULKER_BOX = Key.of("minecraft:purple_shulker_box");
    public static final Key MAGENTA_SHULKER_BOX = Key.of("minecraft:magenta_shulker_box");
    public static final Key PINK_SHULKER_BOX = Key.of("minecraft:pink_shulker_box");
    // 告示牌
    public static final Key OAK_SIGN = Key.of("minecraft:oak_sign");
    public static final Key SPRUCE_SIGN = Key.of("minecraft:spruce_sign");
    public static final Key BIRCH_SIGN = Key.of("minecraft:birch_sign");
    public static final Key JUNGLE_SIGN = Key.of("minecraft:jungle_sign");
    public static final Key ACACIA_SIGN = Key.of("minecraft:acacia_sign");
    public static final Key DARK_OAK_SIGN = Key.of("minecraft:dark_oak_sign");
    public static final Key MANGROVE_SIGN = Key.of("minecraft:mangrove_sign");
    public static final Key CHERRY_SIGN = Key.of("minecraft:cherry_sign");
    public static final Key PALE_OAK_SIGN = Key.of("minecraft:pale_oak_sign");
    public static final Key BAMBOO_SIGN = Key.of("minecraft:bamboo_sign");
    public static final Key CRIMSON_SIGN = Key.of("minecraft:crimson_sign");
    public static final Key WARPED_SIGN = Key.of("minecraft:warped_sign");
    // 靠墙告示牌
    public static final Key OAK_WALL_SIGN = Key.of("minecraft:oak_wall_sign");
    public static final Key SPRUCE_WALL_SIGN = Key.of("minecraft:spruce_wall_sign");
    public static final Key BIRCH_WALL_SIGN = Key.of("minecraft:birch_wall_sign");
    public static final Key JUNGLE_WALL_SIGN = Key.of("minecraft:jungle_wall_sign");
    public static final Key ACACIA_WALL_SIGN = Key.of("minecraft:acacia_wall_sign");
    public static final Key DARK_OAK_WALL_SIGN = Key.of("minecraft:dark_oak_wall_sign");
    public static final Key MANGROVE_WALL_SIGN = Key.of("minecraft:mangrove_wall_sign");
    public static final Key CHERRY_WALL_SIGN = Key.of("minecraft:cherry_wall_sign");
    public static final Key PALE_OAK_WALL_SIGN = Key.of("minecraft:pale_oak_wall_sign");
    public static final Key BAMBOO_WALL_SIGN = Key.of("minecraft:bamboo_wall_sign");
    public static final Key CRIMSON_WALL_SIGN = Key.of("minecraft:crimson_wall_sign");
    public static final Key WARPED_WALL_SIGN = Key.of("minecraft:warped_wall_sign");
    // 悬挂式告示牌
    public static final Key OAK_HANGING_SIGN = Key.of("minecraft:oak_hanging_sign");
    public static final Key SPRUCE_HANGING_SIGN = Key.of("minecraft:spruce_hanging_sign");
    public static final Key BIRCH_HANGING_SIGN = Key.of("minecraft:birch_hanging_sign");
    public static final Key JUNGLE_HANGING_SIGN = Key.of("minecraft:jungle_hanging_sign");
    public static final Key ACACIA_HANGING_SIGN = Key.of("minecraft:acacia_hanging_sign");
    public static final Key DARK_OAK_HANGING_SIGN = Key.of("minecraft:dark_oak_hanging_sign");
    public static final Key MANGROVE_HANGING_SIGN = Key.of("minecraft:mangrove_hanging_sign");
    public static final Key CHERRY_HANGING_SIGN = Key.of("minecraft:cherry_hanging_sign");
    public static final Key PALE_OAK_HANGING_SIGN = Key.of("minecraft:pale_oak_hanging_sign");
    public static final Key BAMBOO_HANGING_SIGN = Key.of("minecraft:bamboo_hanging_sign");
    public static final Key CRIMSON_HANGING_SIGN = Key.of("minecraft:crimson_hanging_sign");
    public static final Key WARPED_HANGING_SIGN = Key.of("minecraft:warped_hanging_sign");
    // 靠墙悬挂式告示牌
    public static final Key OAK_WALL_HANGING_SIGN = Key.of("minecraft:oak_wall_hanging_sign");
    public static final Key SPRUCE_WALL_HANGING_SIGN = Key.of("minecraft:spruce_wall_hanging_sign");
    public static final Key BIRCH_WALL_HANGING_SIGN = Key.of("minecraft:birch_wall_hanging_sign");
    public static final Key JUNGLE_WALL_HANGING_SIGN = Key.of("minecraft:jungle_wall_hanging_sign");
    public static final Key ACACIA_WALL_HANGING_SIGN = Key.of("minecraft:acacia_wall_hanging_sign");
    public static final Key DARK_OAK_WALL_HANGING_SIGN = Key.of("minecraft:dark_oak_wall_hanging_sign");
    public static final Key MANGROVE_WALL_HANGING_SIGN = Key.of("minecraft:mangrove_wall_hanging_sign");
    public static final Key CHERRY_WALL_HANGING_SIGN = Key.of("minecraft:cherry_wall_hanging_sign");
    public static final Key PALE_OAK_WALL_HANGING_SIGN = Key.of("minecraft:pale_oak_wall_hanging_sign");
    public static final Key BAMBOO_WALL_HANGING_SIGN = Key.of("minecraft:bamboo_wall_hanging_sign");
    public static final Key CRIMSON_WALL_HANGING_SIGN = Key.of("minecraft:crimson_wall_hanging_sign");
    public static final Key WARPED_WALL_HANGING_SIGN = Key.of("minecraft:warped_wall_hanging_sign");
    // 展示架
    public static final Key OAK_SHELF = Key.of("minecraft:oak_shelf");
    public static final Key SPRUCE_SHELF = Key.of("minecraft:spruce_shelf");
    public static final Key BIRCH_SHELF = Key.of("minecraft:birch_shelf");
    public static final Key JUNGLE_SHELF = Key.of("minecraft:jungle_shelf");
    public static final Key ACACIA_SHELF = Key.of("minecraft:acacia_shelf");
    public static final Key DARK_OAK_SHELF = Key.of("minecraft:dark_oak_shelf");
    public static final Key MANGROVE_SHELF = Key.of("minecraft:mangrove_shelf");
    public static final Key CHERRY_SHELF = Key.of("minecraft:cherry_shelf");
    public static final Key PALE_OAK_SHELF = Key.of("minecraft:pale_oak_shelf");
    public static final Key BAMBOO_SHELF = Key.of("minecraft:bamboo_shelf");
    public static final Key CRIMSON_SHELF = Key.of("minecraft:crimson_shelf");
    public static final Key WARPED_SHELF = Key.of("minecraft:warped_shelf");
    // 铜傀儡像
    public static final Key COPPER_GOLEM_STATUE = Key.of("minecraft:copper_golem_statue");
    public static final Key EXPOSED_COPPER_GOLEM_STATUE = Key.of("minecraft:exposed_copper_golem_statue");
    public static final Key WEATHERED_COPPER_GOLEM_STATUE = Key.of("minecraft:weathered_copper_golem_statue");
    public static final Key OXIDIZED_COPPER_GOLEM_STATUE = Key.of("minecraft:oxidized_copper_golem_statue");
    public static final Key WAXED_COPPER_GOLEM_STATUE = Key.of("minecraft:waxed_copper_golem_statue");
    public static final Key WAXED_EXPOSED_COPPER_GOLEM_STATUE = Key.of("minecraft:waxed_exposed_copper_golem_statue");
    public static final Key WAXED_WEATHERED_COPPER_GOLEM_STATUE = Key.of("minecraft:waxed_weathered_copper_golem_statue");
    public static final Key WAXED_OXIDIZED_COPPER_GOLEM_STATUE = Key.of("minecraft:waxed_oxidized_copper_golem_statue");

    public static final Key BROWN_MUSHROOM_BLOCK = Key.of("minecraft:brown_mushroom_block");
    public static final Key RED_MUSHROOM_BLOCK = Key.of("minecraft:red_mushroom_block");
    public static final Key MUSHROOM_STEM = Key.of("minecraft:mushroom_stem");

    public static final Key OAK_LEAVES = Key.of("minecraft:oak_leaves");
    public static final Key SPRUCE_LEAVES = Key.of("minecraft:spruce_leaves");
    public static final Key BIRCH_LEAVES = Key.of("minecraft:birch_leaves");
    public static final Key JUNGLE_LEAVES = Key.of("minecraft:jungle_leaves");
    public static final Key ACACIA_LEAVES = Key.of("minecraft:acacia_leaves");
    public static final Key DARK_OAK_LEAVES = Key.of("minecraft:dark_oak_leaves");
    public static final Key MANGROVE_LEAVES = Key.of("minecraft:mangrove_leaves");
    public static final Key CHERRY_LEAVES = Key.of("minecraft:cherry_leaves");
    public static final Key PALE_OAK_LEAVES = Key.of("minecraft:pale_oak_leaves");
    public static final Key AZALEA_LEAVES = Key.of("minecraft:azalea_leaves");
    public static final Key FLOWERING_AZALEA_LEAVES = Key.of("minecraft:flowering_azalea_leaves");

    public static final Key OAK_SAPLING = Key.of("minecraft:oak_sapling");
    public static final Key SPRUCE_SAPLING = Key.of("minecraft:spruce_sapling");
    public static final Key BIRCH_SAPLING = Key.of("minecraft:birch_sapling");
    public static final Key JUNGLE_SAPLING = Key.of("minecraft:jungle_sapling");
    public static final Key DARK_OAK_SAPLING = Key.of("minecraft:dark_oak_sapling");
    public static final Key ACACIA_SAPLING = Key.of("minecraft:acacia_sapling");
    public static final Key CHERRY_SAPLING = Key.of("minecraft:cherry_sapling");
    public static final Key PALE_OAK_SAPLING = Key.of("minecraft:pale_oak_sapling");

    public static final Key[] BUTTONS = new Key[]{
            OAK_BUTTON, SPRUCE_BUTTON, BIRCH_BUTTON, JUNGLE_BUTTON, ACACIA_BUTTON, DARK_OAK_BUTTON, MANGROVE_BUTTON, CHERRY_BUTTON,
            PALE_OAK_BUTTON, BAMBOO_BUTTON, CRIMSON_BUTTON, WARPED_BUTTON, STONE_BUTTON, POLISHED_BLACKSTONE_BUTTON
    };

    public static final Key[] PRESSURE_PLATES = new Key[]{
            OAK_PRESSURE_PLATE, SPRUCE_PRESSURE_PLATE, BIRCH_PRESSURE_PLATE, JUNGLE_PRESSURE_PLATE, ACACIA_PRESSURE_PLATE,
            DARK_OAK_PRESSURE_PLATE, MANGROVE_PRESSURE_PLATE, CHERRY_PRESSURE_PLATE, PALE_OAK_PRESSURE_PLATE, BAMBOO_PRESSURE_PLATE,
            CRIMSON_PRESSURE_PLATE, WARPED_PRESSURE_PLATE, STONE_PRESSURE_PLATE, POLISHED_BLACKSTONE_PRESSURE_PLATE,
            LIGHT_WEIGHTED_PRESSURE_PLATE, HEAVY_WEIGHTED_PRESSURE_PLATE
    };

    public static final Key[] REDSTONE_CONNECTION = new Key[]{
            REDSTONE_WIRE, REDSTONE_TORCH, REDSTONE_BLOCK, REPEATER, COMPARATOR, TARGET, LEVER, SCULK_SENSOR, CALIBRATED_SCULK_SENSOR,
            TRIPWIRE_HOOK, LECTERN, DAYLIGHT_DETECTOR, LIGHTNING_ROD, TRAPPED_CHEST, JUKEBOX, OBSERVER, DETECTOR_RAIL
    };

    public static final List<Key> WOODEN_TRAPDOORS = List.of(OAK_TRAPDOOR, SPRUCE_TRAPDOOR, BIRCH_TRAPDOOR,
            ACACIA_TRAPDOOR, PALE_OAK_TRAPDOOR, DARK_OAK_TRAPDOOR, MANGROVE_TRAPDOOR, JUNGLE_TRAPDOOR);
    public static final List<Key> CHERRY_TRAPDOORS = List.of(CHERRY_TRAPDOOR);
    public static final List<Key> BAMBOO_TRAPDOORS = List.of(BAMBOO_TRAPDOOR);
    public static final List<Key> NETHER_TRAPDOORS = List.of(WARPED_TRAPDOOR, CRIMSON_TRAPDOOR);
    public static final List<Key> COPPER_TRAPDOORS = List.of(COPPER_TRAPDOOR, EXPOSED_COPPER_TRAPDOOR, WEATHERED_COPPER_TRAPDOOR, OXIDIZED_COPPER_DOOR,
            WAXED_COPPER_TRAPDOOR, WAXED_EXPOSED_COPPER_TRAPDOOR, WAXED_WEATHERED_COPPER_TRAPDOOR, WAXED_OXIDIZED_COPPER_TRAPDOOR);

    public static final List<Key> WOODEN_DOORS = List.of(OAK_DOOR, SPRUCE_DOOR, BIRCH_DOOR,
            ACACIA_DOOR, PALE_OAK_DOOR, DARK_OAK_DOOR, MANGROVE_DOOR, JUNGLE_DOOR);
    public static final List<Key> CHERRY_DOORS = List.of(CHERRY_DOOR);
    public static final List<Key> BAMBOO_DOORS = List.of(BAMBOO_DOOR);
    public static final List<Key> NETHER_DOORS = List.of(WARPED_DOOR, CRIMSON_DOOR);
    public static final List<Key> COPPER_DOORS = List.of(COPPER_DOOR, EXPOSED_COPPER_DOOR, WEATHERED_COPPER_DOOR, OXIDIZED_COPPER_DOOR,
            WAXED_COPPER_DOOR, WAXED_EXPOSED_COPPER_DOOR, WAXED_WEATHERED_COPPER_DOOR, WAXED_OXIDIZED_COPPER_DOOR);

    public static final List<Key> WOODEN_FENCE_GATES = List.of(OAK_FENCE_GATE, SPRUCE_FENCE_GATE, BIRCH_FENCE_GATE,
            ACACIA_FENCE_GATE, PALE_OAK_FENCE_GATE, DARK_OAK_FENCE_GATE, MANGROVE_FENCE_GATE, JUNGLE_FENCE_GATE);
    public static final List<Key> CHERRY_FENCE_GATES = List.of(CHERRY_FENCE_GATE);
    public static final List<Key> BAMBOO_FENCE_GATES = List.of(BAMBOO_FENCE_GATE);
    public static final List<Key> NETHER_FENCE_GATES = List.of(WARPED_FENCE_GATE, CRIMSON_FENCE_GATE);

    public static final List<Key> WOODEN_BUTTONS = List.of(OAK_BUTTON, SPRUCE_BUTTON, BIRCH_BUTTON, JUNGLE_BUTTON,
            ACACIA_BUTTON, DARK_OAK_BUTTON, PALE_OAK_BUTTON, MANGROVE_BUTTON);
    public static final List<Key> CHERRY_BUTTONS = List.of(CHERRY_BUTTON);
    public static final List<Key> BAMBOO_BUTTONS = List.of(BAMBOO_BUTTON);
    public static final List<Key> NETHER_BUTTONS = List.of(CRIMSON_BUTTON, WARPED_BUTTON);
    public static final List<Key> STONE_BUTTONS = List.of(STONE_BUTTON, POLISHED_BLACKSTONE_BUTTON);
}
