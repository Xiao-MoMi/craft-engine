package net.momirealms.craftengine.bedrock.geyser.proxy.scoreboard;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.geysermc.geyser.scoreboard.ScoreReference")
public interface ScoreReferenceProxy {
    ScoreReferenceProxy INSTANCE = ASMProxyFactory.create(ScoreReferenceProxy.class);

    @FieldSetter(name = "displayName")
    void displayName(Object score, String displayName);
}
