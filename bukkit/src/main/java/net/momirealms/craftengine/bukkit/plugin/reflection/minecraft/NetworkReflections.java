package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import io.netty.buffer.ByteBuf;
import net.momirealms.craftengine.bukkit.util.BukkitReflectionUtils;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public final class NetworkReflections {
    private NetworkReflections() {}

    // 1.20.2+
    public static final Class<?> clazz$CustomPacketPayload =
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("network.protocol.common.custom.CustomPacketPayload"));

    // 1.20.5+
    public static final Class<?> clazz$CustomPacketPayload$Type = BukkitReflectionUtils.findReobfOrMojmapClass(
            "network.protocol.common.custom.CustomPacketPayload$b",
            "network.protocol.common.custom.CustomPacketPayload$Type"
    );

    // 1.20.2+
    public static final Class<?> clazz$DiscardedPayload =
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("network.protocol.common.custom.DiscardedPayload"));

    // 1.20.5+
    public static final Method method$CustomPacketPayload$type = Optional.ofNullable(clazz$CustomPacketPayload$Type)
            .map(it -> ReflectionUtils.getMethod(clazz$CustomPacketPayload, it))
            .orElse(null);

    // 1.20.5+
    public static final Method method$CustomPacketPayload$Type$id = Optional.ofNullable(clazz$CustomPacketPayload$Type)
            .map(it -> ReflectionUtils.getMethod(it, IdentifierProxy.CLASS))
            .orElse(null);

    // 1.20.5~1.21.4#221
    public static final Method method$DiscardedPayload$data = Optional.ofNullable(clazz$DiscardedPayload)
            .map(it -> ReflectionUtils.getMethod(it, ByteBuf.class))
            .orElse(null);

    // 1.21.4#222+
    public static final Method method$DiscardedPayload$dataByteArray = Optional.ofNullable(method$DiscardedPayload$data)
            .map(m -> (Method) null)
            .orElseGet(() -> Optional.ofNullable(clazz$DiscardedPayload)
                    .map(clazz -> ReflectionUtils.getMethod(clazz, byte[].class))
                    .orElse(null)
            );

    // 1.20.2+
    public static final Constructor<?> constructor$DiscardedPayload = Optional.ofNullable(clazz$DiscardedPayload)
            .map(it -> {
                if (VersionHelper.isOrAbove1_20_5()) {
                    Constructor<?> constructor1 = ReflectionUtils.getConstructor(it, IdentifierProxy.CLASS, ByteBuf.class);
                    if (constructor1 != null) {
                        return constructor1;
                    }
                    return ReflectionUtils.getConstructor(it, IdentifierProxy.CLASS, byte[].class);
                } else {
                    return ReflectionUtils.getConstructor(it, IdentifierProxy.CLASS);
                }
            })
            .orElse(null);

    // 1.20.2~1.20.4
    public static final Class<?> clazz$ServerboundCustomPayloadPacket$UnknownPayload = MiscUtils.requireNonNullIf(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.common.ServerboundCustomPayloadPacket$UnknownPayload")
            ),
            VersionHelper.isOrAbove1_20_2() && !VersionHelper.isOrAbove1_20_5()
    );

    // 1.20.2~1.20.4
    public static final Field field$ServerboundCustomPayloadPacket$UnknownPayload$id = Optional.ofNullable(clazz$ServerboundCustomPayloadPacket$UnknownPayload)
            .map(it -> ReflectionUtils.getDeclaredField(it, IdentifierProxy.CLASS, 0))
            .orElse(null);

    // 1.20.2~1.20.4
    public static final Field field$ServerboundCustomPayloadPacket$UnknownPayload$data = Optional.ofNullable(clazz$ServerboundCustomPayloadPacket$UnknownPayload)
            .map(it -> ReflectionUtils.getDeclaredField(it, ByteBuf.class, 0))
            .orElse(null);

    // 1.20.2~1.20.4 可能打了 https://github.com/PaperMC/Paper/commit/9b1798d.patch 补丁
    public static final Field field$ServerboundCustomPayloadPacket$UnknownPayload$dataByteArray = Optional.ofNullable(clazz$ServerboundCustomPayloadPacket$UnknownPayload)
            .map(it -> ReflectionUtils.getDeclaredField(it, byte[].class, 0))
            .orElse(null);

    // 1.20.2~1.20.4
    public static final Constructor<?> constructor$ServerboundCustomPayloadPacket$UnknownPayload = Optional.ofNullable(clazz$ServerboundCustomPayloadPacket$UnknownPayload)
            .map(it -> ReflectionUtils.getConstructor(it, IdentifierProxy.CLASS, ByteBuf.class))
            .orElse(null);

}
