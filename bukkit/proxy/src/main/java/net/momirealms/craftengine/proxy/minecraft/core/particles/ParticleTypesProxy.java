package net.momirealms.craftengine.proxy.minecraft.core.particles;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.particles.ParticleTypes")
public interface ParticleTypesProxy {
    ParticleTypesProxy INSTANCE = ASMProxyFactory.create(ParticleTypesProxy.class);
    Object STREAM_CODEC = INSTANCE.getStreamCodec();
    Object HAPPY_VILLAGER = INSTANCE.getHappyVillager();
    Object BUBBLE = INSTANCE.getBubble();

    @FieldGetter(name = "HAPPY_VILLAGER", isStatic = true)
    Object getHappyVillager();

    @FieldGetter(name = "BUBBLE", isStatic = true)
    Object getBubble();

    @FieldGetter(name = "STREAM_CODEC", isStatic = true, activeIf = "min_version=1.20.5")
    default Object getStreamCodec() {
        return null;
    }
}
