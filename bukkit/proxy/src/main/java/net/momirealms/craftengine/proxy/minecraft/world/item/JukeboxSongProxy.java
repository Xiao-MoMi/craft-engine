package net.momirealms.craftengine.proxy.minecraft.world.item;

import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundEventProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.item.JukeboxSong", activeIf = "min_version=1.21")
public interface JukeboxSongProxy {
    JukeboxSongProxy INSTANCE = ASMProxyFactory.create(JukeboxSongProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = HolderProxy.class) Object soundEvent,
                       @Type(clazz = ComponentProxy.class) Object description,
                       float length,
                       int comparatorOutput);

    @FieldSetter(name = "soundEvent")
    void setSoundEvent(Object target, @Type(clazz = SoundEventProxy.class) Object soundEvent);

    @FieldSetter(name = "description")
    void setDescription(Object target, @Type(clazz = ComponentProxy.class) Object description);

    @FieldSetter(name = "lengthInSeconds")
    void setLengthInSeconds(Object target, float length);

    @FieldSetter(name = "comparatorOutput")
    void setComparatorOutput(Object target, int comparatorOutput);
}
