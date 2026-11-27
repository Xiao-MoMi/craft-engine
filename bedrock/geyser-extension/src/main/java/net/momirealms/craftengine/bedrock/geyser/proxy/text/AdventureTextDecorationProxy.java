package net.momirealms.craftengine.bedrock.geyser.proxy.text;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = {
        "org.geysermc.geyser.platform.spigot.shaded.net.kyori.adventure.text.format.TextDecoration",
        "org.geysermc.geyser.platform.bungeecord.shaded.net.kyori.adventure.text.format.TextDecoration",
        "org.geysermc.geyser.platform.velocity.shaded.net.kyori.adventure.text.format.TextDecoration",
        "net.kyori.adventure.text.format.TextDecoration"
})
public interface AdventureTextDecorationProxy {
    Class<?> CLASS = SparrowClass.find(AdventureTextDecorationProxy.class.getAnnotation(ReflectionProxy.class).name());
    Object[] VALUES = CLASS.getEnumConstants();
}
