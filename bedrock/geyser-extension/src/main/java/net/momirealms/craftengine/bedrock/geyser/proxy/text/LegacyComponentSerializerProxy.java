package net.momirealms.craftengine.bedrock.geyser.proxy.text;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = {
        "org.geysermc.geyser.platform.spigot.shaded.net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer",
        "org.geysermc.geyser.platform.bungeecord.shaded.net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer",
        "org.geysermc.geyser.platform.velocity.shaded.net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer",
        "net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer"
})
public interface LegacyComponentSerializerProxy {
    LegacyComponentSerializerProxy INSTANCE = ASMProxyFactory.create(LegacyComponentSerializerProxy.class);
    Class<?> CLASS = SparrowClass.find(LegacyComponentSerializerProxy.class.getAnnotation(ReflectionProxy.class).name());

    @MethodInvoker(name = "serialize")
    String serialize(Object serializer, @Type(clazz = AdventureComponentProxy.class) Object component);

    @MethodInvoker(name = "deserialize")
    Object deserialize(Object serializer, String input);

    @MethodInvoker(name = "toBuilder")
    Object toBuilder(Object serializer);
}
