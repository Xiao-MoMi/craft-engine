package net.momirealms.craftengine.bedrock.geyser.proxy.text;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.List;

@ReflectionProxy(name = {
        "org.geysermc.geyser.platform.spigot.shaded.net.kyori.adventure.text.Component",
        "org.geysermc.geyser.platform.bungeecord.shaded.net.kyori.adventure.text.Component",
        "org.geysermc.geyser.platform.velocity.shaded.net.kyori.adventure.text.Component",
        "net.kyori.adventure.text.Component"
})
public interface AdventureComponentProxy {
    AdventureComponentProxy INSTANCE = ASMProxyFactory.create(AdventureComponentProxy.class);

    @MethodInvoker(name = "text", isStatic = true)
    Object text(String content);

    @MethodInvoker(name = "color")
    Object color(Object component, @Type(clazz = AdventureTextColorProxy.class) Object color);

    @MethodInvoker(name = "decorate")
    Object decorate(Object component, @Type(clazz = AdventureTextDecorationProxy.class) Object decoration);

    @MethodInvoker(name = "style")
    Object style(Object component);

    @MethodInvoker(name = "font")
    Object font(Object component);

    @MethodInvoker(name = "font")
    Object font(Object component, @Type(clazz = AdventureKeyProxy.class) Object font);

    @MethodInvoker(name = "children")
    List<Object> children(Object component);

    @MethodInvoker(name = "children")
    Object children(Object component, List<?> children);
}
