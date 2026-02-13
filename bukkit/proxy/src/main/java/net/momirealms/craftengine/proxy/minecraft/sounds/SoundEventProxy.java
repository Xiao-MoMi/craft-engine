package net.momirealms.craftengine.proxy.minecraft.sounds;

import net.momirealms.craftengine.proxy.minecraft.network.FriendlyByteBufProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Optional;

@ReflectionProxy(name = "net.minecraft.sounds.SoundEvent")
public interface SoundEventProxy {
    SoundEventProxy INSTANCE = ASMProxyFactory.create(SoundEventProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.sounds.SoundEvent");

    @FieldGetter(name = "DIRECT_STREAM_CODEC", isStatic = true, activeIf = "min_version=1.20.5")
    Object getDirectStreamCodec();

    @FieldGetter(name = "location")
    Object getLocation(Object target);

    @MethodInvoker(name = "fixedRange")
    Optional<Float> fixedRange(Object target);

    @MethodInvoker(name = "createVariableRangeEvent", isStatic = true)
    Object createVariableRangeEvent(@Type(clazz = IdentifierProxy.class) Object identifier);

    @MethodInvoker(name = "create", isStatic = true)
    Object create(@Type(clazz = IdentifierProxy.class) Object location, Optional<Float> range);

    @MethodInvoker(name = "writeToNetwork", activeIf = "max_version=1.20.4")
    void writeToNetwork(Object target, @Type(clazz = FriendlyByteBufProxy.class) Object buf);
}
