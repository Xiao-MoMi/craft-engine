package net.momirealms.craftengine.proxy.minecraft.advancements;

import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.advancements.AdvancementHolder", activeIf = "min_version=1.20.2")
public interface AdvancementHolderProxy {
    AdvancementHolderProxy INSTANCE = ASMProxyFactory.create(AdvancementHolderProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = IdentifierProxy.class) Object id,
                       @Type(clazz = AdvancementProxy.class) Object advancement);
}
