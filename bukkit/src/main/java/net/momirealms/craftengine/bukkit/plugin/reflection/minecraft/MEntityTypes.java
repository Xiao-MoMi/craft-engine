package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.ReflectionInitException;
import net.momirealms.craftengine.core.util.VersionHelper;

public final class MEntityTypes {
    private MEntityTypes() {}

    public static final Object instance$EntityType$TEXT_DISPLAY;
    public static final int instance$EntityType$TEXT_DISPLAY$registryId;
    public static final Object instance$EntityType$ITEM_DISPLAY;
    public static final int instance$EntityType$ITEM_DISPLAY$registryId;
    public static final Object instance$EntityType$BLOCK_DISPLAY;
    public static final int instance$EntityType$BLOCK_DISPLAY$registryId;
    public static final Object instance$EntityType$ARMOR_STAND;
    public static final int instance$EntityType$ARMOR_STAND$registryId;
    public static final Object instance$EntityType$FALLING_BLOCK;
    public static final int instance$EntityType$FALLING_BLOCK$registryId;
    public static final Object instance$EntityType$INTERACTION;
    public static final int instance$EntityType$INTERACTION$registryId;
    public static final Object instance$EntityType$SHULKER;
    public static final int instance$EntityType$SHULKER$registryId;
    public static final Object instance$EntityType$OAK_BOAT;
    public static final int instance$EntityType$OAK_BOAT$registryId;
    public static final Object instance$EntityType$TRIDENT;
    public static final int instance$EntityType$TRIDENT$registryId;
    public static final Object instance$EntityType$SNOWBALL;
    public static final int instance$EntityType$SNOWBALL$registryId;
    public static final Object instance$EntityType$FIREBALL;
    public static final int instance$EntityType$FIREBALL$registryId;
    public static final Object instance$EntityType$EYE_OF_ENDER;
    public static final int instance$EntityType$EYE_OF_ENDER$registryId;
    public static final Object instance$EntityType$FIREWORK_ROCKET;
    public static final int instance$EntityType$FIREWORK_ROCKET$registryId;
    public static final Object instance$EntityType$ITEM;
    public static final int instance$EntityType$ITEM$registryId;
    public static final Object instance$EntityType$ITEM_FRAME;
    public static final int instance$EntityType$ITEM_FRAME$registryId;
    public static final Object instance$EntityType$GLOW_ITEM_FRAME;
    public static final int instance$EntityType$GLOW_ITEM_FRAME$registryId;
    public static final Object instance$EntityType$OMINOUS_ITEM_SPAWNER;
    public static final int instance$EntityType$OMINOUS_ITEM_SPAWNER$registryId;
    public static final Object instance$EntityType$SMALL_FIREBALL;
    public static final int instance$EntityType$SMALL_FIREBALL$registryId;
    public static final Object instance$EntityType$EGG;
    public static final int instance$EntityType$EGG$registryId;
    public static final Object instance$EntityType$ENDER_PEARL;
    public static final int instance$EntityType$ENDER_PEARL$registryId;
    public static final Object instance$EntityType$EXPERIENCE_BOTTLE;
    public static final int instance$EntityType$EXPERIENCE_BOTTLE$registryId;
    public static final Object instance$EntityType$POTION;
    public static final int instance$EntityType$POTION$registryId;
    public static final Object instance$EntityType$PLAYER;
    public static final int instance$EntityType$PLAYER$registryId;

    private static Object getById(String id) throws ReflectiveOperationException {
        Object rl = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", id);
        return CoreReflections.method$Registry$get.invoke(MBuiltInRegistries.ENTITY_TYPE, rl);
    }

    private static int getRegistryId(Object type) throws ReflectiveOperationException {
        if (type == null) return -1;
        return (int) CoreReflections.method$Registry$getId.invoke(MBuiltInRegistries.ENTITY_TYPE, type);
    }

    static {
        try {
            instance$EntityType$TEXT_DISPLAY = getById("text_display");
            instance$EntityType$TEXT_DISPLAY$registryId = getRegistryId(instance$EntityType$TEXT_DISPLAY);
            instance$EntityType$ITEM_DISPLAY = getById("item_display");
            instance$EntityType$ITEM_DISPLAY$registryId = getRegistryId(instance$EntityType$ITEM_DISPLAY);
            instance$EntityType$BLOCK_DISPLAY = getById("block_display");
            instance$EntityType$BLOCK_DISPLAY$registryId = getRegistryId(instance$EntityType$BLOCK_DISPLAY);
            instance$EntityType$FALLING_BLOCK = getById("falling_block");
            instance$EntityType$FALLING_BLOCK$registryId = getRegistryId(instance$EntityType$FALLING_BLOCK);
            instance$EntityType$INTERACTION = getById("interaction");
            instance$EntityType$INTERACTION$registryId = getRegistryId(instance$EntityType$INTERACTION);
            instance$EntityType$SHULKER = getById("shulker");
            instance$EntityType$SHULKER$registryId = getRegistryId(instance$EntityType$SHULKER);
            instance$EntityType$ARMOR_STAND = getById("armor_stand");
            instance$EntityType$ARMOR_STAND$registryId = getRegistryId(instance$EntityType$ARMOR_STAND);
            instance$EntityType$OAK_BOAT = getById(VersionHelper.isOrAbove1_21_2() ? "oak_boat" : "boat");
            instance$EntityType$OAK_BOAT$registryId = getRegistryId(instance$EntityType$OAK_BOAT);
            instance$EntityType$TRIDENT = getById("trident");
            instance$EntityType$TRIDENT$registryId = getRegistryId(instance$EntityType$TRIDENT);
            instance$EntityType$SNOWBALL = getById("snowball");
            instance$EntityType$SNOWBALL$registryId = getRegistryId(instance$EntityType$SNOWBALL);
            instance$EntityType$FIREBALL = getById("fireball");
            instance$EntityType$FIREBALL$registryId = getRegistryId(instance$EntityType$FIREBALL);
            instance$EntityType$EYE_OF_ENDER = getById("eye_of_ender");
            instance$EntityType$EYE_OF_ENDER$registryId = getRegistryId(instance$EntityType$EYE_OF_ENDER);
            instance$EntityType$FIREWORK_ROCKET = getById("firework_rocket");
            instance$EntityType$FIREWORK_ROCKET$registryId = getRegistryId(instance$EntityType$FIREWORK_ROCKET);
            instance$EntityType$ITEM = getById("item");
            instance$EntityType$ITEM$registryId = getRegistryId(instance$EntityType$ITEM);
            instance$EntityType$ITEM_FRAME = getById("item_frame");
            instance$EntityType$ITEM_FRAME$registryId = getRegistryId(instance$EntityType$ITEM_FRAME);
            instance$EntityType$GLOW_ITEM_FRAME = getById("glow_item_frame");
            instance$EntityType$GLOW_ITEM_FRAME$registryId = getRegistryId(instance$EntityType$GLOW_ITEM_FRAME);
            instance$EntityType$SMALL_FIREBALL = getById("small_fireball");
            instance$EntityType$SMALL_FIREBALL$registryId = getRegistryId(instance$EntityType$SMALL_FIREBALL);
            instance$EntityType$EGG = getById("egg");
            instance$EntityType$EGG$registryId = getRegistryId(instance$EntityType$EGG);
            instance$EntityType$ENDER_PEARL = getById("ender_pearl");
            instance$EntityType$ENDER_PEARL$registryId = getRegistryId(instance$EntityType$ENDER_PEARL);
            instance$EntityType$EXPERIENCE_BOTTLE = getById("experience_bottle");
            instance$EntityType$EXPERIENCE_BOTTLE$registryId = getRegistryId(instance$EntityType$EXPERIENCE_BOTTLE);
            instance$EntityType$POTION = getById("potion");
            instance$EntityType$POTION$registryId = getRegistryId(instance$EntityType$POTION);
            instance$EntityType$OMINOUS_ITEM_SPAWNER = VersionHelper.isOrAbove1_20_5() ? getById("ominous_item_spawner") : null;
            instance$EntityType$OMINOUS_ITEM_SPAWNER$registryId = getRegistryId(instance$EntityType$OMINOUS_ITEM_SPAWNER);
            instance$EntityType$PLAYER = getById("player");
            instance$EntityType$PLAYER$registryId = getRegistryId(instance$EntityType$PLAYER);
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to init EntityTypes", e);
        }
    }
}
