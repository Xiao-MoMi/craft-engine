package net.momirealms.craftengine.bukkit.util;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;

import java.util.Objects;

public class LegacyAttributeUtils {

    public static void setMaxHealth(ArmorStand entity) {
        Objects.requireNonNull(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(0.01);
    }
}
