package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.craftengine.proxy.minecraft.sounds.SoundEventProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.field.matcher.FieldMatcher;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

import java.lang.reflect.Field;

@ReflectionProxy(name = "net.minecraft.world.level.block.SoundType")
public interface SoundTypeProxy {
    SoundTypeProxy INSTANCE = ASMProxyFactory.create(SoundTypeProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.block.SoundType");

    default Object getByName(String name) {
        Field field = SparrowClass.of(CLASS).getField(FieldMatcher.named(name).and(FieldMatcher.staticField()).and(FieldMatcher.type(CLASS)));
        if (field == null) {
            return null;
        }
        try {
            return field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to access sound type '" + name + "'", e);
        }
    }

    @ConstructorInvoker
    Object newInstance(float volume,
                       float pitch,
                       @Type(clazz = SoundEventProxy.class) Object breakSound,
                       @Type(clazz = SoundEventProxy.class) Object stepSound,
                       @Type(clazz = SoundEventProxy.class) Object placeSound,
                       @Type(clazz = SoundEventProxy.class) Object hitSound,
                       @Type(clazz = SoundEventProxy.class) Object fallSound);

    @FieldGetter(name = "volume")
    float getVolume(Object target);

    @FieldSetter(name = "volume")
    void setVolume(Object target, float volume);

    @FieldGetter(name = "pitch")
    float getPitch(Object target);

    @FieldSetter(name = "pitch")
    void setPitch(Object target, float pitch);

    @FieldGetter(name = "breakSound")
    Object getBreakSound(Object target);

    @FieldSetter(name = "breakSound")
    void setBreakSound(Object target, Object breakSound);

    @FieldGetter(name = "stepSound")
    Object getStepSound(Object target);

    @FieldSetter(name = "stepSound")
    void setStepSound(Object target, Object stepSound);

    @FieldGetter(name = "placeSound")
    Object getPlaceSound(Object target);

    @FieldSetter(name = "placeSound")
    void setPlaceSound(Object target, Object placeSound);

    @FieldGetter(name = "hitSound")
    Object getHitSound(Object target);

    @FieldSetter(name = "hitSound")
    void setHitSound(Object target, Object hitSound);

    @FieldGetter(name = "fallSound")
    Object getFallSound(Object target);

    @FieldSetter(name = "fallSound")
    void setFallSound(Object target, Object fallSound);
}
