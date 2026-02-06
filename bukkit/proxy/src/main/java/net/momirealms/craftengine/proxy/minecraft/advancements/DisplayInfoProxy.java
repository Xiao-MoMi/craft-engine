package net.momirealms.craftengine.proxy.minecraft.advancements;

import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Optional;

@ReflectionProxy(name = "net.minecraft.advancements.DisplayInfo")
public interface DisplayInfoProxy {
    DisplayInfoProxy INSTANCE = ASMProxyFactory.create(DisplayInfoProxy.class);

    @ConstructorInvoker(activeIf = "min_version=1.20.3")
    Object newInstance(@Type(clazz = ItemStackProxy.class) Object icon,
                       @Type(clazz = ComponentProxy.class) Object title,
                       @Type(clazz = ComponentProxy.class) Object description,
                       Optional<Object> background,
                       @Type(clazz = AdvancementTypeProxy.class) Object type,
                       boolean showToast,
                       boolean announceChat,
                       boolean hidden);

    @ConstructorInvoker(activeIf = "max_version=1.20.2")
    Object newInstance$legacy(@Type(clazz = ItemStackProxy.class) Object icon,
                              @Type(clazz = ComponentProxy.class) Object title,
                              @Type(clazz = ComponentProxy.class) Object description,
                              @Type(clazz = IdentifierProxy.class) Object background,
                              @Type(clazz = AdvancementTypeProxy.class) Object type,
                              boolean showToast,
                              boolean announceChat,
                              boolean hidden);
}
