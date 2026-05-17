package net.momirealms.craftengine.proxy.minecraft.sounds;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.sounds.SoundSource")
public interface SoundSourceProxy {
    SoundSourceProxy INSTANCE = ASMProxyFactory.create(SoundSourceProxy.class);
    Enum<?>[] VALUES = INSTANCE.values();
    Enum<?> MASTER = VALUES[0];
    Enum<?> MUSIC = VALUES[1];
    Enum<?> RECORD = VALUES[2];
    Enum<?> WEATHER = VALUES[3];
    Enum<?> BLOCKS = VALUES[4];
    Enum<?> HOSTILE = VALUES[5];
    Enum<?> NEUTRAL = VALUES[6];
    Enum<?> PLAYERS = VALUES[7];
    Enum<?> AMBIENT = VALUES[8];
    Enum<?> VOICE = VALUES[9];
    Enum<?> UI = VALUES.length > 10 ? VALUES[10] : null;

    @MethodInvoker(name = "values", isStatic = true)
    Enum<?>[] values();

    @FieldGetter(name = "name")
    String getName(Object target);
}
