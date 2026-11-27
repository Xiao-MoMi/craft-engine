package net.momirealms.craftengine.bedrock.geyser.proxy.text;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;

@ReflectionProxy(name = {
        "org.geysermc.geyser.platform.spigot.shaded.net.kyori.adventure.text.TranslatableComponent",
        "org.geysermc.geyser.platform.bungeecord.shaded.net.kyori.adventure.text.TranslatableComponent",
        "org.geysermc.geyser.platform.velocity.shaded.net.kyori.adventure.text.TranslatableComponent",
        "net.kyori.adventure.text.TranslatableComponent"
})
public interface AdventureTranslatableComponentProxy {
    AdventureTranslatableComponentProxy INSTANCE = ASMProxyFactory.create(AdventureTranslatableComponentProxy.class);
    Class<?> CLASS = SparrowClass.find(AdventureTranslatableComponentProxy.class.getAnnotation(ReflectionProxy.class).name());

    @MethodInvoker(name = "arguments")
    List<?> arguments(Object component);
}
