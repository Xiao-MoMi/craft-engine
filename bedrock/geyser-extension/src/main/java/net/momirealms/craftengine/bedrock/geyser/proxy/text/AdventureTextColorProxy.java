package net.momirealms.craftengine.bedrock.geyser.proxy.text;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = {
        "org.geysermc.geyser.platform.spigot.shaded.net.kyori.adventure.text.format.TextColor",
        "org.geysermc.geyser.platform.bungeecord.shaded.net.kyori.adventure.text.format.TextColor",
        "org.geysermc.geyser.platform.velocity.shaded.net.kyori.adventure.text.format.TextColor",
        "net.kyori.adventure.text.format.TextColor"
})
public interface AdventureTextColorProxy {
    AdventureTextColorProxy INSTANCE = ASMProxyFactory.create(AdventureTextColorProxy.class);

    @MethodInvoker(name = "value")
    int value(Object color);

    @MethodInvoker(name = "color", isStatic = true)
    Object color(int value);
}
