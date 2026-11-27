package net.momirealms.craftengine.bedrock.geyser.proxy.item.custom;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponent;

@ReflectionProxy(name = "org.geysermc.geyser.item.custom.GeyserCustomItemDefinition$Builder")
public interface GeyserCustomItemDefinitionBuilderProxy {
    GeyserCustomItemDefinitionBuilderProxy INSTANCE = ASMProxyFactory.create(GeyserCustomItemDefinitionBuilderProxy.class);

    @MethodInvoker(name = "geyserComponent")
    void geyserComponent(Object target, ItemDataComponent<?> component, Object value);
}
