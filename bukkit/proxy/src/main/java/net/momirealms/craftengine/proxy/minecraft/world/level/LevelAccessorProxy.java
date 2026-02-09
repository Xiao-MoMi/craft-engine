package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundEventProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.gameevent.GameEventProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import javax.annotation.Nullable;

@ReflectionProxy(name = "net.minecraft.world.level.LevelAccessor")
public interface LevelAccessorProxy extends LevelReaderProxy {
    LevelAccessorProxy INSTANCE = ASMProxyFactory.create(LevelAccessorProxy.class);

    @MethodInvoker(name = "gameEvent", activeIf = "min_version=1.20.5")
    void gameEvent$0(Object target, @Nullable @Type(clazz = EntityProxy.class) Object entity, @Type(clazz = HolderProxy.class) Object event, @Type(clazz = BlockPosProxy.class) Object pos);

    @MethodInvoker(name = "gameEvent", activeIf = "max_version=1.20.4")
    void gameEvent$1(Object target, @Nullable @Type(clazz = EntityProxy.class) Object entity, @Type(clazz = GameEventProxy.class) Object event, @Type(clazz = BlockPosProxy.class) Object pos);

    @MethodInvoker(name = "playSound", activeIf = "min_version=1.21.5")
    void playSound$0(Object target, @Nullable @Type(clazz = EntityProxy.class) Object source, @Type(clazz = BlockPosProxy.class) Object pos, @Type(clazz = SoundEventProxy.class) Object sound, @Type(clazz = SoundSourceProxy.class) Object category, float volume, float pitch);

    @MethodInvoker(name = "playSound", activeIf = "max_version=1.21.4")
    void playSound$1(Object target, @Nullable @Type(clazz = PlayerProxy.class) Object source, @Type(clazz = BlockPosProxy.class) Object pos, @Type(clazz = SoundEventProxy.class) Object sound, @Type(clazz = SoundSourceProxy.class) Object category, float volume, float pitch);
}
