package net.momirealms.craftengine.bedrock.geyser.proxy.text;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.Map;

@ReflectionProxy(name = {
        "org.geysermc.geyser.platform.spigot.shaded.net.kyori.adventure.text.format.Style",
        "org.geysermc.geyser.platform.bungeecord.shaded.net.kyori.adventure.text.format.Style",
        "org.geysermc.geyser.platform.velocity.shaded.net.kyori.adventure.text.format.Style",
        "net.kyori.adventure.text.format.Style"
})
public interface AdventureStyleProxy {
    AdventureStyleProxy INSTANCE = ASMProxyFactory.create(AdventureStyleProxy.class);

    @MethodInvoker(name = "color")
    Object color(Object style);

    @MethodInvoker(name = "decorations")
    Map<?, ?> decorations(Object style);
}
