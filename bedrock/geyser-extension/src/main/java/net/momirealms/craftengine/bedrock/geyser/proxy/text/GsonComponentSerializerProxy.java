package net.momirealms.craftengine.bedrock.geyser.proxy.text;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = {
        "org.geysermc.geyser.platform.spigot.shaded.net.kyori.adventure.text.serializer.gson.GsonComponentSerializer",
        "org.geysermc.geyser.platform.bungeecord.shaded.net.kyori.adventure.text.serializer.gson.GsonComponentSerializer",
        "org.geysermc.geyser.platform.velocity.shaded.net.kyori.adventure.text.serializer.gson.GsonComponentSerializer",
        "net.kyori.adventure.text.serializer.gson.GsonComponentSerializer"
})
public interface GsonComponentSerializerProxy {
    GsonComponentSerializerProxy INSTANCE = ASMProxyFactory.create(GsonComponentSerializerProxy.class);

    @MethodInvoker(name = "gson", isStatic = true)
    Object gson();

    static Object deserialize(String json) {
        return Decoder.INSTANCE.deserialize(INSTANCE.gson(), json);
    }

    @ReflectionProxy(name = {
            "org.geysermc.geyser.platform.spigot.shaded.net.kyori.adventure.text.serializer.ComponentSerializer",
            "org.geysermc.geyser.platform.bungeecord.shaded.net.kyori.adventure.text.serializer.ComponentSerializer",
            "org.geysermc.geyser.platform.velocity.shaded.net.kyori.adventure.text.serializer.ComponentSerializer",
            "net.kyori.adventure.text.serializer.ComponentSerializer"
    })
    interface Decoder {
        Decoder INSTANCE = ASMProxyFactory.create(Decoder.class);

        @MethodInvoker(name = "deserialize")
        Object deserialize(Object serializer, Object json);
    }
}
