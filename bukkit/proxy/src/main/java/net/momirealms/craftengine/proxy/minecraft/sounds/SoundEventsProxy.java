package net.momirealms.craftengine.proxy.minecraft.sounds;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.sounds.SoundEvents")
public interface SoundEventsProxy {
    SoundEventsProxy INSTANCE = ASMProxyFactory.create(SoundEventsProxy.class);
    Object EMPTY = INSTANCE.getEMPTY();
    Object TOTEM_USE = INSTANCE.getTOTEM_USE();

    @FieldGetter(name = "EMPTY", isStatic = true)
    Object getEMPTY();

    @FieldGetter(name = "TOTEM_USE", isStatic = true)
    Object getTOTEM_USE();
}
