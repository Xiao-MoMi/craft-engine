package net.momirealms.craftengine.bedrock.geyser.proxy.text;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = {
        "org.geysermc.geyser.platform.spigot.shaded.net.kyori.adventure.text.TextComponent",
        "org.geysermc.geyser.platform.bungeecord.shaded.net.kyori.adventure.text.TextComponent",
        "org.geysermc.geyser.platform.velocity.shaded.net.kyori.adventure.text.TextComponent",
        "net.kyori.adventure.text.TextComponent"
})
public interface AdventureTextComponentProxy {
    AdventureTextComponentProxy INSTANCE = ASMProxyFactory.create(AdventureTextComponentProxy.class);
    Class<?> CLASS = SparrowClass.find(AdventureTextComponentProxy.class.getAnnotation(ReflectionProxy.class).name());

    @MethodInvoker(name = "content")
    String content(Object component);

    @MethodInvoker(name = "content")
    Object content(Object component, String content);
}
