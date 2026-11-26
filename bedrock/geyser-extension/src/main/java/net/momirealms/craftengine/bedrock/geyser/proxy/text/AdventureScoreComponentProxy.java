package net.momirealms.craftengine.bedrock.geyser.proxy.text;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = {
        "org.geysermc.geyser.platform.spigot.shaded.net.kyori.adventure.text.ScoreComponent",
        "org.geysermc.geyser.platform.bungeecord.shaded.net.kyori.adventure.text.ScoreComponent",
        "org.geysermc.geyser.platform.velocity.shaded.net.kyori.adventure.text.ScoreComponent",
        "net.kyori.adventure.text.ScoreComponent"
})
public interface AdventureScoreComponentProxy {
    AdventureScoreComponentProxy INSTANCE = ASMProxyFactory.create(AdventureScoreComponentProxy.class);
    Class<?> CLASS = SparrowClass.find(AdventureScoreComponentProxy.class.getAnnotation(ReflectionProxy.class).name());

    @MethodInvoker(name = "value")
    String value(Object component);
}
