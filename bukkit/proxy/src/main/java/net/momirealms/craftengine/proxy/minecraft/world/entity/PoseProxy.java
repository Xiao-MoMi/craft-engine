package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.Pose")
public interface PoseProxy {
    PoseProxy INSTANCE = ASMProxyFactory.create(PoseProxy.class);
    Object[] VALUES = INSTANCE.values();
    Object STANDING = VALUES[0];
    Object FALL_FLYING = VALUES[1];
    Object SLEEPING = VALUES[2];
    Object SWIMMING = VALUES[3];
    Object SPIN_ATTACK = VALUES[4];
    Object CROUCHING = VALUES[5];
    Object LONG_JUMPING = VALUES[6];
    Object DYING = VALUES[7];
    Object CROAKING = VALUES[8];
    Object USING_TONGUE = VALUES[9];
    Object SITTING = VALUES[10];
    Object ROARING = VALUES[11];
    Object SNIFFING = VALUES[12];
    Object EMERGING = VALUES[13];
    Object DIGGING = VALUES[14];
    Object SLIDING = VALUES.length > 15 ? VALUES[15] : null;
    Object SHOOTING = VALUES.length > 16 ? VALUES[16] : null;
    Object INHALING = VALUES.length > 17 ? VALUES[17] : null;

    @MethodInvoker(name = "values", isStatic = true)
    Object[] values();
}
