package net.momirealms.craftengine.bukkit.util;

public class MobEffectUtils {

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
            Object packet = Reflections.allocateClientboundUpdateMobEffectPacketInstance();
            Reflections.field$ClientboundUpdateMobEffectPacket$entityId.set(packet, entityId);
            Reflections.field$ClientboundUpdateMobEffectPacket$duration.set(packet, duration);
            Reflections.field$ClientboundUpdateMobEffectPacket$amplifier.set(packet, amplifier);
            Reflections.field$ClientboundUpdateMobEffectPacket$effect.set(packet, mobEffect);
            byte flags = pack(isAmbient, isVisible, showIcon);
            Reflections.field$ClientboundUpdateMobEffectPacket$flags.set(packet, flags);
            return packet;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
