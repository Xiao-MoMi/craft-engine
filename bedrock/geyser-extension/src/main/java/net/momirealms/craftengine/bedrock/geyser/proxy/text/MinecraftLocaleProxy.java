package net.momirealms.craftengine.bedrock.geyser.proxy.text;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.Map;

@ReflectionProxy(name = "org.geysermc.geyser.text.MinecraftLocale")
public interface MinecraftLocaleProxy {
    MinecraftLocaleProxy INSTANCE = ASMProxyFactory.create(MinecraftLocaleProxy.class);

    @FieldGetter(name = "LOCALE_MAPPINGS", isStatic = true)
    Map<String, Map<String, String>> localeMappings();

    @FieldSetter(name = "LOCALE_MAPPINGS", isStatic = true)
    void localeMappings(Map<String, Map<String, String>> mappings);
}
