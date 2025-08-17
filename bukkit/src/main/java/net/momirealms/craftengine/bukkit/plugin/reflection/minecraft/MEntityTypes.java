package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.util.VersionHelper;

public final class MEntityTypes {
    private MEntityTypes() {}

    public static final Object TEXT_DISPLAY;
    public static final int TEXT_DISPLAY$registryId;
    public static final Object ITEM_DISPLAY;
    public static final int ITEM_DISPLAY$registryId;
    public static final Object BLOCK_DISPLAY;
    public static final int BLOCK_DISPLAY$registryId;
    public static final Object ARMOR_STAND;
    public static final int ARMOR_STAND$registryId;
    public static final Object FALLING_BLOCK;
    public static final int FALLING_BLOCK$registryId;
    public static final Object INTERACTION;
    public static final int INTERACTION$registryId;
    public static final Object SHULKER;
    public static final int SHULKER$registryId;
    public static final Object OAK_BOAT;
    public static final int OAK_BOAT$registryId;
    public static final Object TRIDENT;
    public static final int TRIDENT$registryId;
    public static final Object ARROW;
    public static final int ARROW$registryId;
    public static final Object SPECTRAL_ARROW;
    public static final int SPECTRAL_ARROW$registryId;
    public static final Object SNOWBALL;
    public static final int SNOWBALL$registryId;
    public static final Object FIREBALL;
    public static final int FIREBALL$registryId;
    public static final Object EYE_OF_ENDER;
    public static final int EYE_OF_ENDER$registryId;
    public static final Object FIREWORK_ROCKET;
    public static final int FIREWORK_ROCKET$registryId;
    public static final Object ITEM;
    public static final int ITEM$registryId;
    public static final Object ITEM_FRAME;
    public static final int ITEM_FRAME$registryId;
    public static final Object GLOW_ITEM_FRAME;
    public static final int GLOW_ITEM_FRAME$registryId;
    public static final Object OMINOUS_ITEM_SPAWNER;
    public static final int OMINOUS_ITEM_SPAWNER$registryId;
    public static final Object SMALL_FIREBALL;
    public static final int SMALL_FIREBALL$registryId;
    public static final Object EGG;
    public static final int EGG$registryId;
    public static final Object ENDER_PEARL;
    public static final int ENDER_PEARL$registryId;
    public static final Object EXPERIENCE_BOTTLE;
    public static final int EXPERIENCE_BOTTLE$registryId;
    public static final Object POTION;
    public static final int POTION$registryId;
    public static final Object HAPPY_GHAST;
    public static final int HAPPY_GHAST$registryId;
    public static final Object PLAYER;
    public static final int PLAYER$registryId;
    public static final Object ENDERMAN;
    public static final int ENDERMAN$registryId;

    private static Object getById(String id) {
        Object rl = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", id);
        return FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.ENTITY_TYPE, rl);
    }

    private static int getRegistryId(Object type) {
        if (type == null) return -1;
        return FastNMS.INSTANCE.method$Registry$getId(MBuiltInRegistries.ENTITY_TYPE, type);
    }

    static {
        TEXT_DISPLAY = getById("text_display");
        TEXT_DISPLAY$registryId = getRegistryId(TEXT_DISPLAY);
        ITEM_DISPLAY = getById("item_display");
        ITEM_DISPLAY$registryId = getRegistryId(ITEM_DISPLAY);
        BLOCK_DISPLAY = getById("block_display");
        BLOCK_DISPLAY$registryId = getRegistryId(BLOCK_DISPLAY);
        FALLING_BLOCK = getById("falling_block");
        FALLING_BLOCK$registryId = getRegistryId(FALLING_BLOCK);
        INTERACTION = getById("interaction");
        INTERACTION$registryId = getRegistryId(INTERACTION);
        SHULKER = getById("shulker");
        SHULKER$registryId = getRegistryId(SHULKER);
        ARMOR_STAND = getById("armor_stand");
        ARMOR_STAND$registryId = getRegistryId(ARMOR_STAND);
        OAK_BOAT = getById(VersionHelper.isOrAbove1_21_2() ? "oak_boat" : "boat");
        OAK_BOAT$registryId = getRegistryId(OAK_BOAT);
        TRIDENT = getById("trident");
        TRIDENT$registryId = getRegistryId(TRIDENT);
        SNOWBALL = getById("snowball");
        SNOWBALL$registryId = getRegistryId(SNOWBALL);
        FIREBALL = getById("fireball");
        FIREBALL$registryId = getRegistryId(FIREBALL);
        EYE_OF_ENDER = getById("eye_of_ender");
        EYE_OF_ENDER$registryId = getRegistryId(EYE_OF_ENDER);
        FIREWORK_ROCKET = getById("firework_rocket");
        FIREWORK_ROCKET$registryId = getRegistryId(FIREWORK_ROCKET);
        ITEM = getById("item");
        ITEM$registryId = getRegistryId(ITEM);
        ITEM_FRAME = getById("item_frame");
        ITEM_FRAME$registryId = getRegistryId(ITEM_FRAME);
        GLOW_ITEM_FRAME = getById("glow_item_frame");
        GLOW_ITEM_FRAME$registryId = getRegistryId(GLOW_ITEM_FRAME);
        SMALL_FIREBALL = getById("small_fireball");
        SMALL_FIREBALL$registryId = getRegistryId(SMALL_FIREBALL);
        EGG = getById("egg");
        EGG$registryId = getRegistryId(EGG);
        ENDER_PEARL = getById("ender_pearl");
        ENDER_PEARL$registryId = getRegistryId(ENDER_PEARL);
        EXPERIENCE_BOTTLE = getById("experience_bottle");
        EXPERIENCE_BOTTLE$registryId = getRegistryId(EXPERIENCE_BOTTLE);
        POTION = getById("potion");
        POTION$registryId = getRegistryId(POTION);
        OMINOUS_ITEM_SPAWNER = VersionHelper.isOrAbove1_20_5() ? getById("ominous_item_spawner") : null;
        OMINOUS_ITEM_SPAWNER$registryId = getRegistryId(OMINOUS_ITEM_SPAWNER);
        HAPPY_GHAST = VersionHelper.isOrAbove1_21_6() ? getById("happy_ghast") : null;
        HAPPY_GHAST$registryId = getRegistryId(HAPPY_GHAST);
        PLAYER = getById("player");
        PLAYER$registryId = getRegistryId(PLAYER);
        ARROW = getById("arrow");
        ARROW$registryId = getRegistryId(ARROW);
        SPECTRAL_ARROW = getById("spectral_arrow");
        SPECTRAL_ARROW$registryId = getRegistryId(SPECTRAL_ARROW);
        ENDERMAN = getById("enderman");
        ENDERMAN$registryId = getRegistryId(ENDERMAN);
    }
}
