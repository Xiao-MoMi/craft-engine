package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.EntityType")
public interface EntityTypeProxy {
    EntityTypeProxy INSTANCE = ASMProxyFactory.create(EntityTypeProxy.class);
    Object TEXT_DISPLAY = INSTANCE.getTextDisplay();
    Object ITEM_DISPLAY = INSTANCE.getItemDisplay();
    Object BLOCK_DISPLAY = INSTANCE.getBlockDisplay();
    Object ARMOR_STAND = INSTANCE.getArmorStand();
    Object FALLING_BLOCK = INSTANCE.getFallingBlock();
    Object INTERACTION = INSTANCE.getInteraction();
    Object SHULKER = INSTANCE.getShulker();
    Object OAK_BOAT = INSTANCE.getOakBoat();
    Object TRIDENT = INSTANCE.getTrident();
    Object ARROW = INSTANCE.getArrow();
    Object SPECTRAL_ARROW = INSTANCE.getSpectralArrow();
    Object SNOWBALL = INSTANCE.getSnowball();
    Object FIREBALL = INSTANCE.getFireball();
    Object EYE_OF_ENDER = INSTANCE.getEyeOfEnder();
    Object FIREWORK_ROCKET = INSTANCE.getFireworkRocket();
    Object ITEM = INSTANCE.getItem();
    Object ITEM_FRAME = INSTANCE.getItemFrame();
    Object GLOW_ITEM_FRAME = INSTANCE.getGlowItemFrame();
    Object OMINOUS_ITEM_SPAWNER = INSTANCE.getOminousItemSpawner();
    Object SMALL_FIREBALL = INSTANCE.getSmallFireball();
    Object EGG = INSTANCE.getEgg();
    Object ENDER_PEARL = INSTANCE.getEnderPearl();
    Object EXPERIENCE_BOTTLE = INSTANCE.getExperienceBottle();
    Object POTION = INSTANCE.getPotion();
    Object HAPPY_GHAST = INSTANCE.getHappyGhast();
    Object PLAYER = INSTANCE.getPlayer();
    Object ENDERMAN = INSTANCE.getEnderman();
    Object TNT = INSTANCE.getTnt();
    Object CHEST_MINECART = INSTANCE.getChestMinecart();
    Object COMMAND_BLOCK_MINECART = INSTANCE.getCommandBlockMinecart();
    Object FURNACE_MINECART = INSTANCE.getFurnaceMinecart();
    Object HOPPER_MINECART  = INSTANCE.getHopperMinecart();
    Object MINECART  = INSTANCE.getMinecart();
    Object SPAWNER_MINECART  = INSTANCE.getSpawnerMinecart();
    Object TNT_MINECART = INSTANCE.getTntMinecart();
    int TEXT_DISPLAY$registryId = getRegistryId(TEXT_DISPLAY);
    int ITEM_DISPLAY$registryId = getRegistryId(ITEM_DISPLAY);
    int BLOCK_DISPLAY$registryId = getRegistryId(BLOCK_DISPLAY);
    int ARMOR_STAND$registryId = getRegistryId(ARMOR_STAND);
    int FALLING_BLOCK$registryId = getRegistryId(FALLING_BLOCK);
    int INTERACTION$registryId = getRegistryId(INTERACTION);
    int SHULKER$registryId = getRegistryId(SHULKER);
    int OAK_BOAT$registryId = getRegistryId(OAK_BOAT);
    int TRIDENT$registryId = getRegistryId(TRIDENT);
    int ARROW$registryId = getRegistryId(ARROW);
    int SPECTRAL_ARROW$registryId = getRegistryId(SPECTRAL_ARROW);
    int SNOWBALL$registryId = getRegistryId(SNOWBALL);
    int FIREBALL$registryId = getRegistryId(FIREBALL);
    int EYE_OF_ENDER$registryId = getRegistryId(EYE_OF_ENDER);
    int FIREWORK_ROCKET$registryId = getRegistryId(FIREWORK_ROCKET);
    int ITEM$registryId = getRegistryId(ITEM);
    int ITEM_FRAME$registryId = getRegistryId(ITEM_FRAME);
    int GLOW_ITEM_FRAME$registryId = getRegistryId(GLOW_ITEM_FRAME);
    int OMINOUS_ITEM_SPAWNER$registryId = getRegistryId(OMINOUS_ITEM_SPAWNER);
    int SMALL_FIREBALL$registryId = getRegistryId(SMALL_FIREBALL);
    int EGG$registryId = getRegistryId(EGG);
    int ENDER_PEARL$registryId = getRegistryId(ENDER_PEARL);
    int EXPERIENCE_BOTTLE$registryId = getRegistryId(EXPERIENCE_BOTTLE);
    int POTION$registryId = getRegistryId(POTION);
    int HAPPY_GHAST$registryId = getRegistryId(HAPPY_GHAST);
    int PLAYER$registryId = getRegistryId(PLAYER);
    int ENDERMAN$registryId = getRegistryId(ENDERMAN);
    int TNT$registryId = getRegistryId(TNT);
    int CHEST_MINECART$registryId = getRegistryId(CHEST_MINECART);
    int COMMAND_BLOCK_MINECART$registryId = getRegistryId(COMMAND_BLOCK_MINECART);
    int FURNACE_MINECART$registryId = getRegistryId(FURNACE_MINECART);
    int HOPPER_MINECART$registryId = getRegistryId(HOPPER_MINECART);
    int MINECART$registryId = getRegistryId(MINECART);
    int SPAWNER_MINECART$registryId = getRegistryId(SPAWNER_MINECART);
    int TNT_MINECART$registryId = getRegistryId(TNT_MINECART);

    private static int getRegistryId(Object type) {
        if (type == null) return -1;
        return RegistryProxy.INSTANCE.getId(BuiltInRegistriesProxy.ENTITY_TYPE, type);
    }

    @FieldGetter(name = "dimensions")
    Object getDimensions(Object target);

    @FieldGetter(name = "TEXT_DISPLAY", isStatic = true)
    Object getTextDisplay();

    @FieldGetter(name = "ITEM_DISPLAY", isStatic = true)
    Object getItemDisplay();

    @FieldGetter(name = "BLOCK_DISPLAY", isStatic = true)
    Object getBlockDisplay();

    @FieldGetter(name = "ARMOR_STAND", isStatic = true)
    Object getArmorStand();

    @FieldGetter(name = "FALLING_BLOCK", isStatic = true)
    Object getFallingBlock();

    @FieldGetter(name = "INTERACTION", isStatic = true)
    Object getInteraction();

    @FieldGetter(name = "SHULKER", isStatic = true)
    Object getShulker();

    @FieldGetter(name = {"OAK_BOAT", "BOAT"}, isStatic = true)
    Object getOakBoat();

    @FieldGetter(name = "TRIDENT", isStatic = true)
    Object getTrident();

    @FieldGetter(name = "ARROW", isStatic = true)
    Object getArrow();

    @FieldGetter(name = "SPECTRAL_ARROW", isStatic = true)
    Object getSpectralArrow();

    @FieldGetter(name = "SNOWBALL", isStatic = true)
    Object getSnowball();

    @FieldGetter(name = "FIREBALL", isStatic = true)
    Object getFireball();

    @FieldGetter(name = "EYE_OF_ENDER", isStatic = true)
    Object getEyeOfEnder();

    @FieldGetter(name = "FIREWORK_ROCKET", isStatic = true)
    Object getFireworkRocket();

    @FieldGetter(name = "ITEM", isStatic = true)
    Object getItem();

    @FieldGetter(name = "ITEM_FRAME", isStatic = true)
    Object getItemFrame();

    @FieldGetter(name = "GLOW_ITEM_FRAME", isStatic = true)
    Object getGlowItemFrame();

    @FieldGetter(name = "OMINOUS_ITEM_SPAWNER", isStatic = true, activeIf = "min_version=1.20.5")
    default Object getOminousItemSpawner() {
        return null;
    }

    @FieldGetter(name = "SMALL_FIREBALL", isStatic = true)
    Object getSmallFireball();

    @FieldGetter(name = "EGG", isStatic = true)
    Object getEgg();

    @FieldGetter(name = "ENDER_PEARL", isStatic = true)
    Object getEnderPearl();

    @FieldGetter(name = "EXPERIENCE_BOTTLE", isStatic = true)
    Object getExperienceBottle();

    @FieldGetter(name = {"SPLASH_POTION", "POTION"}, isStatic = true)
    Object getPotion();

    @FieldGetter(name = "HAPPY_GHAST", isStatic = true, activeIf = "min_version=1.21.6")
    default Object getHappyGhast() {
        return null;
    }

    @FieldGetter(name = "PLAYER", isStatic = true)
    Object getPlayer();

    @FieldGetter(name = "ENDERMAN", isStatic = true)
    Object getEnderman();

    @FieldGetter(name = "TNT", isStatic = true)
    Object getTnt();

    @FieldGetter(name = "CHEST_MINECART", isStatic = true)
    Object getChestMinecart();

    @FieldGetter(name = "COMMAND_BLOCK_MINECART", isStatic = true)
    Object getCommandBlockMinecart();

    @FieldGetter(name = "FURNACE_MINECART", isStatic = true)
    Object getFurnaceMinecart();

    @FieldGetter(name = "HOPPER_MINECART", isStatic = true)
    Object getHopperMinecart();

    @FieldGetter(name = "MINECART", isStatic = true)
    Object getMinecart();

    @FieldGetter(name = "SPAWNER_MINECART", isStatic = true)
    Object getSpawnerMinecart();

    @FieldGetter(name = "TNT_MINECART", isStatic = true)
    Object getTntMinecart();
}
