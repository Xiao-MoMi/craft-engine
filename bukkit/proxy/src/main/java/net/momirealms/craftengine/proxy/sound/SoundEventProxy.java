package net.momirealms.craftengine.proxy.sound;

import net.momirealms.craftengine.proxy.resource.IdentifierProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Optional;

@ReflectionProxy(name = "net.minecraft.sounds.SoundEvent")
public interface SoundEventProxy {
    SoundEventProxy INSTANCE = ASMProxyFactory.create(SoundEventProxy.class);

    @FieldGetter(name = "location")
    Object getLocation(Object target);

    @FieldGetter(name = "fixedRange")
    Optional<Float> getFixedRange(Object target);

    @MethodInvoker(name = "createVariableRangeEvent", isStatic = true)
    Object createVariableRangeEvent(@Type(clazz = IdentifierProxy.class) Object identifier);
}
