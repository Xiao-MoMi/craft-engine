package net.momirealms.craftengine.proxy.minecraft.world.scores;

import net.momirealms.craftengine.proxy.minecraft.ChatFormattingProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Optional;

@ReflectionProxy(name = "net.minecraft.world.scores.PlayerTeam")
public interface PlayerTeamProxy {
    PlayerTeamProxy INSTANCE = ASMProxyFactory.create(PlayerTeamProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = ScoreboardProxy.class) Object scoreboard, String name);

    @MethodInvoker(name = "setColor", activeIf = "max_version=26.1.2")
    void setColor(Object target, @Type(clazz = ChatFormattingProxy.class) Object color);

    @MethodInvoker(name = "setColor", activeIf = "min_version=26.2")
    void setColor$modern(Object target, Optional<?> color);
}
