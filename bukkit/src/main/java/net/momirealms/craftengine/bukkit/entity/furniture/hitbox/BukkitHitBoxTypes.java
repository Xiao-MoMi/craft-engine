package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import net.momirealms.craftengine.core.entity.furniture.HitBoxTypes;

public class BukkitHitBoxTypes extends HitBoxTypes {

    public static void init() {
        register(INTERACTION, InteractionHitBox.FACTORY);
        register(SHULKER, ShulkerHitBox.FACTORY);
        register(HAPPY_GHAST, HappyGhastHitBox.FACTORY);
        register(CUSTOM, CustomHitBox.FACTORY);
    }

    static {
        //register(INTERACTION, InteractionHitBox.FACTORY);
        //register(SHULKER, ShulkerHitBox.FACTORY);
        //register(HAPPY_GHAST, HappyGhastHitBox.FACTORY);
        //register(CUSTOM, CustomHitBox.FACTORY);
    }
}
