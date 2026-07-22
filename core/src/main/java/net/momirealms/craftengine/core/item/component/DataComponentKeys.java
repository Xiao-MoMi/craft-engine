package net.momirealms.craftengine.core.item.component;

import net.momirealms.craftengine.core.util.Key;

import java.util.Set;

public final class DataComponentKeys {
    private DataComponentKeys() {}

    public static final Key ATTRIBUTE_MODIFIERS = Key.minecraft("attribute_modifiers");
    public static final Key BANNER_PATTERN = Key.minecraft("banner_patterns");
    public static final Key BASE_COLOR = Key.minecraft("base_color");
    public static final Key BEES = Key.minecraft("bees");
    public static final Key BLOCK_ENTITY_DATA = Key.minecraft("block_entity_data");
    public static final Key BLOCK_STATE = Key.minecraft("block_state");
    public static final Key BLOCK_TRANSFORMER = Key.minecraft("block_transformer");
    public static final Key BLOCKS_ATTACK = Key.minecraft("blocks_attacks");
    public static final Key BREAK_SOUND = Key.minecraft("break_sound");
    public static final Key BREWING_FUEL = Key.minecraft("brewing_fuel");
    public static final Key BUCKET_ENTITY_DATA = Key.minecraft("bucket_entity_data");
    public static final Key BUNDLE_CONTENTS = Key.minecraft("bundle_contents");
    public static final Key CAN_BREAK = Key.minecraft("can_break");
    public static final Key CAN_PLACE_ON = Key.minecraft("can_place_on");
    public static final Key CHARGED_PROJECTILES = Key.minecraft("charged_projectiles");
    public static final Key COMPOSTABLE = Key.minecraft("compostable");
    public static final Key CONSUMABLE = Key.minecraft("consumable");
    public static final Key CONTAINER = Key.minecraft("container");
    public static final Key CONTAINER_LOOT = Key.minecraft("container_loot");
    public static final Key COOKING_FUEL = Key.minecraft("cooking_fuel");
    public static final Key CUSHION_COLOR = Key.minecraft("cushion/color");
    public static final Key CUSTOM_DATA = Key.minecraft("custom_data");
    public static final Key CUSTOM_MODEL_DATA = Key.minecraft("custom_model_data");
    public static final Key CUSTOM_NAME = Key.minecraft("custom_name");
    public static final Key DAMAGE = Key.minecraft("damage");
    public static final Key DAMAGE_TYPE = Key.minecraft("damage_type");
    public static final Key DAMAGE_RESISTANT = Key.minecraft("damage_resistant");
    public static final Key DEBUG_STICK_STATE = Key.minecraft("debug_stick_state");
    public static final Key DEATH_PROTECTION = Key.minecraft("death_protection");
    public static final Key DYE = Key.minecraft("dye");
    public static final Key DYED_COLOR = Key.minecraft("dyed_color");
    public static final Key ENCHANTABLE = Key.minecraft("enchantable");
    public static final Key ENCHANTMENT_GLINT_OVERRIDE = Key.minecraft("enchantment_glint_override");
    public static final Key ENCHANTMENTS = Key.minecraft("enchantments");
    public static final Key ENTITY_DATA = Key.minecraft("entity_data");
    public static final Key EQUIPPABLE = Key.minecraft("equippable");
    public static final Key FIREWORK_EXPLOSION = Key.minecraft("firework_explosion");
    public static final Key FIREWORKS = Key.minecraft("fireworks");
    public static final Key FOOD = Key.minecraft("food");
    public static final Key GLIDER = Key.minecraft("glider");
    public static final Key INSTRUMENT = Key.minecraft("instrument");
    public static final Key INTANGIBLE_PROJECTILE = Key.minecraft("intangible_projectile");
    public static final Key ITEM_MODEL = Key.minecraft("item_model");
    public static final Key ITEM_NAME = Key.minecraft("item_name");
    public static final Key JUKEBOX_PLAYABLE = Key.minecraft("jukebox_playable");
    public static final Key KINETIC_WEAPON = Key.minecraft("kinetic_weapon");
    public static final Key LOCK = Key.minecraft("lock");
    public static final Key LODESTONE_TRACKER = Key.minecraft("lodestone_tracker");
    public static final Key LORE = Key.minecraft("lore");
    public static final Key MAP_COLOR = Key.minecraft("map_color");
    public static final Key MAP_DECORATIONS = Key.minecraft("map_decorations");
    public static final Key MAP_ID = Key.minecraft("map_id");
    public static final Key MAX_DAMAGE = Key.minecraft("max_damage");
    public static final Key MAX_STACK_SIZE = Key.minecraft("max_stack_size");
    public static final Key MINIUM_ATTACK_CHARGE = Key.minecraft("minimum_attack_charge");
    public static final Key MOB_VISIBILITY = Key.minecraft("mob_visibility");
    public static final Key NOTE_BLOCK_SOUND = Key.minecraft("note_block_sound");
    public static final Key OMINOUS_BOTTLE_AMPLIFIER = Key.minecraft("ominous_bottle_amplifier");
    public static final Key PAINTING_VARIANT = Key.minecraft("painting/variant");
    public static final Key PIERCING_WEAPON = Key.minecraft("piercing_weapon");
    public static final Key POT_DECORATIONS = Key.minecraft("pot_decorations");
    public static final Key POTION_CONTENTS = Key.minecraft("potion_contents");
    public static final Key POTION_DURATION_SCALE = Key.minecraft("potion_duration_scale");
    public static final Key PROFILE = Key.minecraft("profile");
    public static final Key PROVIDES_BANNER_PATTERN = Key.minecraft("provides_banner_patterns");
    public static final Key PROVIDES_POTTERY_PATTERN = Key.minecraft("provides_pottery_pattern");
    public static final Key PROVIDES_TRIM_MATERIAL = Key.minecraft("provides_trim_material");
    public static final Key RARITY = Key.minecraft("rarity");
    public static final Key RECIPES = Key.minecraft("recipes");
    public static final Key REPAIRABLE = Key.minecraft("repairable");
    public static final Key REPAIR_COST = Key.minecraft("repair_cost");
    public static final Key SIGN_TEXT_BACK = Key.minecraft("sign_text_back");
    public static final Key SIGN_TEXT_FRONT = Key.minecraft("sign_text_front");
    public static final Key STORED_ENCHANTMENTS = Key.minecraft("stored_enchantments");
    public static final Key SULFUR_CUBE_CONTENT = Key.minecraft("sulfur_cube_content");
    public static final Key SUSPICIOUS_STEW_EFFECTS = Key.minecraft("suspicious_stew_effects");
    public static final Key SWING_ANIMATION = Key.minecraft("swing_animation");
    public static final Key TOOL = Key.minecraft("tool");
    public static final Key TOOLTIP_DISPLAY = Key.minecraft("tooltip_display");
    public static final Key TOOLTIP_STYLE = Key.minecraft("tooltip_style");
    public static final Key TRIM = Key.minecraft("trim");
    public static final Key UNBREAKABLE = Key.minecraft("unbreakable");
    public static final Key USE_COOLDOWN = Key.minecraft("use_cooldown");
    public static final Key USE_EFFECTS = Key.minecraft("use_effects");
    public static final Key USE_REMAINDER = Key.minecraft("use_remainder");
    public static final Key VILLAGER_FOOD = Key.minecraft("villager_food");
    public static final Key WAXED = Key.minecraft("waxed");
    public static final Key WEAPON = Key.minecraft("weapon");
    public static final Key WRITABLE_BOOK_CONTENT = Key.minecraft("writable_book_content");
    public static final Key WRITTEN_BOOK_CONTENT = Key.minecraft("written_book_content");

    public static final Set<Key> VALUES = Set.of(
            ATTRIBUTE_MODIFIERS,
            BANNER_PATTERN,
            BASE_COLOR,
            BEES,
            BLOCK_ENTITY_DATA,
            BLOCK_STATE,
            BLOCK_TRANSFORMER,
            BLOCKS_ATTACK,
            BREAK_SOUND,
            BREWING_FUEL,
            BUCKET_ENTITY_DATA,
            BUNDLE_CONTENTS,
            CAN_BREAK,
            CAN_PLACE_ON,
            CHARGED_PROJECTILES,
            COMPOSTABLE,
            CONSUMABLE,
            CONTAINER,
            CONTAINER_LOOT,
            COOKING_FUEL,
            CUSHION_COLOR,
            CUSTOM_DATA,
            CUSTOM_MODEL_DATA,
            CUSTOM_NAME,
            DAMAGE,
            DAMAGE_TYPE,
            DAMAGE_RESISTANT,
            DEBUG_STICK_STATE,
            DEATH_PROTECTION,
            DYE,
            DYED_COLOR,
            ENCHANTABLE,
            ENCHANTMENT_GLINT_OVERRIDE,
            ENCHANTMENTS,
            ENTITY_DATA,
            EQUIPPABLE,
            FIREWORK_EXPLOSION,
            FIREWORKS,
            FOOD,
            GLIDER,
            INSTRUMENT,
            INTANGIBLE_PROJECTILE,
            ITEM_MODEL,
            ITEM_NAME,
            JUKEBOX_PLAYABLE,
            KINETIC_WEAPON,
            LOCK,
            LODESTONE_TRACKER,
            LORE,
            MAP_COLOR,
            MAP_DECORATIONS,
            MAP_ID,
            MAX_DAMAGE,
            MAX_STACK_SIZE,
            MINIUM_ATTACK_CHARGE,
            MOB_VISIBILITY,
            NOTE_BLOCK_SOUND,
            OMINOUS_BOTTLE_AMPLIFIER,
            PAINTING_VARIANT,
            PIERCING_WEAPON,
            POT_DECORATIONS,
            POTION_CONTENTS,
            POTION_DURATION_SCALE,
            PROFILE,
            PROVIDES_BANNER_PATTERN,
            PROVIDES_POTTERY_PATTERN,
            PROVIDES_TRIM_MATERIAL,
            RARITY,
            RECIPES,
            REPAIRABLE,
            REPAIR_COST,
            SIGN_TEXT_BACK,
            SIGN_TEXT_FRONT,
            STORED_ENCHANTMENTS,
            SULFUR_CUBE_CONTENT,
            SUSPICIOUS_STEW_EFFECTS,
            SWING_ANIMATION,
            TOOL,
            TOOLTIP_DISPLAY,
            TOOLTIP_STYLE,
            TRIM,
            UNBREAKABLE,
            USE_COOLDOWN,
            USE_EFFECTS,
            USE_REMAINDER,
            VILLAGER_FOOD,
            WAXED,
            WEAPON,
            WRITABLE_BOOK_CONTENT,
            WRITTEN_BOOK_CONTENT
    );

    public static boolean contains(Key key) {
        return VALUES.contains(key);
    }
}
