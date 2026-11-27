package net.momirealms.craftengine.bedrock.geyser.proxy.text;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = {
        "org.geysermc.geyser.platform.spigot.shaded.net.kyori.adventure.text.renderer.AbstractComponentRenderer",
        "org.geysermc.geyser.platform.bungeecord.shaded.net.kyori.adventure.text.renderer.AbstractComponentRenderer",
        "org.geysermc.geyser.platform.velocity.shaded.net.kyori.adventure.text.renderer.AbstractComponentRenderer",
        "net.kyori.adventure.text.renderer.AbstractComponentRenderer"
})
public interface AdventureTranslationRendererProxy {
    AdventureTranslationRendererProxy INSTANCE = ASMProxyFactory.create(AdventureTranslationRendererProxy.class);

    @MethodInvoker(name = "render")
    Object render(Object renderer, @Type(clazz = AdventureComponentProxy.class) Object component, Object locale);
}
