package net.momirealms.craftengine.bedrock.geyser.proxy.text;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = {
        "org.geysermc.geyser.platform.spigot.shaded.net.kyori.adventure.key.Key",
        "org.geysermc.geyser.platform.bungeecord.shaded.net.kyori.adventure.key.Key",
        "org.geysermc.geyser.platform.velocity.shaded.net.kyori.adventure.key.Key",
        "net.kyori.adventure.key.Key"
})
public interface AdventureKeyProxy {
    AdventureKeyProxy INSTANCE = ASMProxyFactory.create(AdventureKeyProxy.class);

    @MethodInvoker(name = "key", isStatic = true)
    Object key(String value);
}
