package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;

public final class MobEffectUtils {

    private MobEffectUtils() {}

    public static byte pack(boolean isAmbient, boolean isVisible, boolean showIcon) {
        byte b = 0;
        if (isAmbient) {
            b = (byte) (b | 1);
        }
        if (isVisible) {
            b = (byte) (b | 2);
        }
        if (showIcon) {
            b = (byte) (b | 4);
        }
        return b;
    }

    public static Object createPacket(Object mobEffect, int entityId, byte amplifier, int duration, boolean isAmbient, boolean isVisible, boolean showIcon) {
        try {
            Object packet = NetworkReflections.allocateClientboundUpdateMobEffectPacketInstance();
            NetworkReflections.field$ClientboundUpdateMobEffectPacket$entityId.set(packet, entityId);
            NetworkReflections.field$ClientboundUpdateMobEffectPacket$duration.set(packet, duration);
            NetworkReflections.field$ClientboundUpdateMobEffectPacket$amplifier.set(packet, amplifier);
            NetworkReflections.field$ClientboundUpdateMobEffectPacket$effect.set(packet, mobEffect);
            byte flags = pack(isAmbient, isVisible, showIcon);
            NetworkReflections.field$ClientboundUpdateMobEffectPacket$flags.set(packet, flags);
            return packet;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
