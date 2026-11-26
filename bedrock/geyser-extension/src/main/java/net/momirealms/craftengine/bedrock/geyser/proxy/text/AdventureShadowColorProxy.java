package net.momirealms.craftengine.bedrock.geyser.proxy.text;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = {
        "org.geysermc.geyser.platform.spigot.shaded.net.kyori.adventure.text.format.ShadowColor",
        "org.geysermc.geyser.platform.bungeecord.shaded.net.kyori.adventure.text.format.ShadowColor",
        "org.geysermc.geyser.platform.velocity.shaded.net.kyori.adventure.text.format.ShadowColor",
        "net.kyori.adventure.text.format.ShadowColor"
})
public interface AdventureShadowColorProxy {
    AdventureShadowColorProxy INSTANCE = ASMProxyFactory.create(AdventureShadowColorProxy.class);

    @MethodInvoker(name = "value")
    int value(Object shadowColor);
}
