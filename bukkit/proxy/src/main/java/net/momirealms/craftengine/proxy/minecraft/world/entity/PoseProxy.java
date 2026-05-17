package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.Pose")
public interface PoseProxy {
    PoseProxy INSTANCE = ASMProxyFactory.create(PoseProxy.class);
    Enum<?>[] VALUES = INSTANCE.values();
    Enum<?> STANDING = VALUES[0];
    Enum<?> FALL_FLYING = VALUES[1];
    Enum<?> SLEEPING = VALUES[2];
    Enum<?> SWIMMING = VALUES[3];
    Enum<?> SPIN_ATTACK = VALUES[4];
    Enum<?> CROUCHING = VALUES[5];
    Enum<?> LONG_JUMPING = VALUES[6];
    Enum<?> DYING = VALUES[7];
    Enum<?> CROAKING = VALUES[8];
    Enum<?> USING_TONGUE = VALUES[9];
    Enum<?> SITTING = VALUES[10];
    Enum<?> ROARING = VALUES[11];
    Enum<?> SNIFFING = VALUES[12];
    Enum<?> EMERGING = VALUES[13];
    Enum<?> DIGGING = VALUES[14];
    Enum<?> SLIDING = VALUES.length > 15 ? VALUES[15] : null;
    Enum<?> SHOOTING = VALUES.length > 16 ? VALUES[16] : null;
    Enum<?> INHALING = VALUES.length > 17 ? VALUES[17] : null;

    @MethodInvoker(name = "values", isStatic = true)
    Enum<?>[] values();
}
