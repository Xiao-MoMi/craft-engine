package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacketProxy;
import net.momirealms.sparrow.reflection.SReflection;

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
            Object packet = SReflection.UNSAFE.allocateInstance(ClientboundUpdateMobEffectPacketProxy.CLASS);
            ClientboundUpdateMobEffectPacketProxy.INSTANCE.setEntityId(packet, entityId);
            ClientboundUpdateMobEffectPacketProxy.INSTANCE.setEffectDurationTicks(packet, duration);
            if (VersionHelper.isOrAbove1_20_5()) {
                ClientboundUpdateMobEffectPacketProxy.INSTANCE.setEffectAmplifier(packet, amplifier);
                ClientboundUpdateMobEffectPacketProxy.INSTANCE.setEffect(packet, mobEffect);
            } else {
                ClientboundUpdateMobEffectPacketProxy.INSTANCE.setEffectAmplifier$legacy(packet, amplifier);
                ClientboundUpdateMobEffectPacketProxy.INSTANCE.setEffect$legacy(packet, mobEffect);
            }
            byte flags = pack(isAmbient, isVisible, showIcon);
            ClientboundUpdateMobEffectPacketProxy.INSTANCE.setFlags(packet, flags);
            return packet;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
}
