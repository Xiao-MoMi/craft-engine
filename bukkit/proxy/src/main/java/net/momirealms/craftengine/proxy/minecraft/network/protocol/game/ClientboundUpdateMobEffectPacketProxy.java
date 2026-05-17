package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.world.effect.MobEffectInstanceProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket")
public interface ClientboundUpdateMobEffectPacketProxy {
    ClientboundUpdateMobEffectPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundUpdateMobEffectPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket");

    @ConstructorInvoker(activeIf = "max_version=1.20.4")
    Object newInstance(int entityId, @Type(clazz = MobEffectInstanceProxy.class) Object effect);

    @ConstructorInvoker(activeIf = "min_version=1.20.5")
    Object newInstance(int entityId, @Type(clazz = MobEffectInstanceProxy.class) Object effect, boolean blend);

    @FieldSetter(name = "entityId")
    void setEntityId(Object target, int id);

    @FieldSetter(name = "effectAmplifier", activeIf = "min_version=1.20.5")
    void setEffectAmplifier(Object target, int amplifier);

    @FieldSetter(name = "effectAmplifier", activeIf = "max_version=1.20.4")
    void setEffectAmplifier$legacy(Object target, byte amplifier);

    @FieldSetter(name = "effectDurationTicks")
    void setEffectDurationTicks(Object target, int ticks);

    @FieldSetter(name = "effect", activeIf = "min_version=1.20.5")
    void setEffect(Object target, Object effect);

    @FieldSetter(name = "effect", activeIf = "max_version=1.20.4")
    void setEffect$legacy(Object target, Object effect);

    @FieldSetter(name = "flags")
    void setFlags(Object target, byte flags);
}
