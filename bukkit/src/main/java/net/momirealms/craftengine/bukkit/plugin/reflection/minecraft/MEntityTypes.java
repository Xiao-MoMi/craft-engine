package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;

public final class MEntityTypes {
    private MEntityTypes() {}

    public static final Object TEXT_DISPLAY = getById("text_display");
    public static final int TEXT_DISPLAY$registryId = getRegistryId(TEXT_DISPLAY);
    public static final Object ITEM_DISPLAY = getById("item_display");
    public static final int ITEM_DISPLAY$registryId = getRegistryId(ITEM_DISPLAY);
    public static final Object BLOCK_DISPLAY = getById("block_display");
    public static final int BLOCK_DISPLAY$registryId = getRegistryId(BLOCK_DISPLAY);
    public static final Object ARMOR_STAND = getById("armor_stand");
    public static final int ARMOR_STAND$registryId = getRegistryId(ARMOR_STAND);
    public static final Object FALLING_BLOCK = getById("falling_block");
    public static final int FALLING_BLOCK$registryId = getRegistryId(FALLING_BLOCK);
    public static final Object INTERACTION = getById("interaction");
    public static final int INTERACTION$registryId = getRegistryId(INTERACTION);
    public static final Object SHULKER = getById("shulker");
    public static final int SHULKER$registryId = getRegistryId(SHULKER);
    public static final Object OAK_BOAT = getById("oak_boat");
    public static final int OAK_BOAT$registryId = getRegistryId(OAK_BOAT);
    public static final Object TRIDENT = getById("trident");
    public static final int TRIDENT$registryId = getRegistryId(TRIDENT);
    public static final Object ARROW = getById("arrow");
    public static final int ARROW$registryId = getRegistryId(ARROW);
    public static final Object SPECTRAL_ARROW = getById("spectral_arrow");
    public static final int SPECTRAL_ARROW$registryId = getRegistryId(SPECTRAL_ARROW);
    public static final Object SNOWBALL = getById("snowball");
    public static final int SNOWBALL$registryId = getRegistryId(SNOWBALL);
    public static final Object FIREBALL = getById("fireball");
    public static final int FIREBALL$registryId = getRegistryId(FIREBALL);
    public static final Object EYE_OF_ENDER = getById("eye_of_ender");
    public static final int EYE_OF_ENDER$registryId = getRegistryId(EYE_OF_ENDER);
    public static final Object FIREWORK_ROCKET = getById("firework_rocket");
    public static final int FIREWORK_ROCKET$registryId = getRegistryId(FIREWORK_ROCKET);
    public static final Object ITEM = getById("item");
    public static final int ITEM$registryId = getRegistryId(ITEM);
    public static final Object ITEM_FRAME = getById("item_frame");
    public static final int ITEM_FRAME$registryId = getRegistryId(ITEM_FRAME);
    public static final Object GLOW_ITEM_FRAME = getById("glow_item_frame");
    public static final int GLOW_ITEM_FRAME$registryId = getRegistryId(GLOW_ITEM_FRAME);
    public static final Object OMINOUS_ITEM_SPAWNER = getById("ominous_item_spawner");
    public static final int OMINOUS_ITEM_SPAWNER$registryId = getRegistryId(OMINOUS_ITEM_SPAWNER);
    public static final Object SMALL_FIREBALL = getById("small_fireball");
    public static final int SMALL_FIREBALL$registryId = getRegistryId(SMALL_FIREBALL);
    public static final Object EGG = getById("egg");
    public static final int EGG$registryId = getRegistryId(EGG);
    public static final Object ENDER_PEARL = getById("ender_pearl");
    public static final int ENDER_PEARL$registryId = getRegistryId(ENDER_PEARL);
    public static final Object EXPERIENCE_BOTTLE = getById("experience_bottle");
    public static final int EXPERIENCE_BOTTLE$registryId = getRegistryId(EXPERIENCE_BOTTLE);
    public static final Object POTION = getById("potion");
    public static final int POTION$registryId = getRegistryId(POTION);
    public static final Object HAPPY_GHAST = getById("happy_ghast");
    public static final int HAPPY_GHAST$registryId = getRegistryId(HAPPY_GHAST);
    public static final Object PLAYER = getById("player");
    public static final int PLAYER$registryId = getRegistryId(PLAYER);
    public static final Object ENDERMAN = getById("enderman");
    public static final int ENDERMAN$registryId = getRegistryId(ENDERMAN);
    public static final Object TNT = getById("tnt");
    public static final int TNT$registryId = getRegistryId(TNT);
    public static final Object CHEST_MINECART = getById("chest_minecart");
    public static final int CHEST_MINECART$registryId = getRegistryId(CHEST_MINECART);
    public static final Object COMMAND_BLOCK_MINECART = getById("command_block_minecart");
    public static final int COMMAND_BLOCK_MINECART$registryId = getRegistryId(COMMAND_BLOCK_MINECART);
    public static final Object FURNACE_MINECART = getById("furnace_minecart");
    public static final int FURNACE_MINECART$registryId = getRegistryId(FURNACE_MINECART);
    public static final Object HOPPER_MINECART  = getById("hopper_minecart");
    public static final int HOPPER_MINECART$registryId = getRegistryId(HOPPER_MINECART);
    public static final Object MINECART  = getById("minecart");
    public static final int MINECART$registryId = getRegistryId(MINECART);
    public static final Object SPAWNER_MINECART  = getById("spawner_minecart");
    public static final int SPAWNER_MINECART$registryId = getRegistryId(SPAWNER_MINECART);
    public static final Object TNT_MINECART = getById("tnt_minecart");
    public static final int TNT_MINECART$registryId = getRegistryId(TNT_MINECART);

    private static Object getById(String path) {
        Object id = IdentifierProxy.INSTANCE.newInstance("minecraft", path);
        return RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.ENTITY_TYPE, id);
    }

    private static int getRegistryId(Object type) {
        if (type == null) return -1;
        return RegistryProxy.INSTANCE.getId$0(BuiltInRegistriesProxy.ENTITY_TYPE, type);
    }
}
