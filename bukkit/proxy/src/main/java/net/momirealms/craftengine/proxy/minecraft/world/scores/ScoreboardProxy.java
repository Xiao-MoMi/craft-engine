package net.momirealms.craftengine.proxy.minecraft.world.scores;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.scores.Scoreboard")
public interface ScoreboardProxy {
    ScoreboardProxy INSTANCE = ASMProxyFactory.create(ScoreboardProxy.class);
}
