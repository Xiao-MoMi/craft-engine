package net.momirealms.craftengine.proxy.sound;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.sounds.SoundSource")
public interface SoundSourceProxy {
    SoundSourceProxy INSTANCE = ASMProxyFactory.create(SoundSourceProxy.class);
    Object[] VALUES = INSTANCE.values();
    Object MASTER = VALUES[0];
    Object MUSIC = VALUES[1];
    Object RECORD = VALUES[2];
    Object WEATHER = VALUES[3];
    Object BLOCKS = VALUES[4];
    Object HOSTILE = VALUES[5];
    Object NEUTRAL = VALUES[6];
    Object PLAYERS = VALUES[7];
    Object AMBIENT = VALUES[8];
    Object VOICE = VALUES[9];
    Object UI = VALUES.length > 10 ? VALUES[10] : null;

    @MethodInvoker(name = "values", isStatic = true)
    Object[] values();

    @FieldGetter(name = "name")
    String getName(Object target);
}
