package net.momirealms.craftengine.proxy.minecraft.world.scores;

import net.momirealms.craftengine.proxy.minecraft.ChatFormattingProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.scores.PlayerTeam")
public interface PlayerTeamProxy {
    PlayerTeamProxy INSTANCE = ASMProxyFactory.create(PlayerTeamProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = ScoreboardProxy.class) Object scoreboard, String name);

    @FieldSetter(name = "color")
    void setColor(Object target, @Type(clazz = ChatFormattingProxy.class) Object color);
}
