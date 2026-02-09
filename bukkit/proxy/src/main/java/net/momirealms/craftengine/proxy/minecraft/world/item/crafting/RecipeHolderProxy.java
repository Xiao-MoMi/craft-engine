package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.RecipeHolder", activeIf = "min_version=1.20.2")
public interface RecipeHolderProxy {
    RecipeHolderProxy INSTANCE = ASMProxyFactory.create(RecipeHolderProxy.class);

    @ConstructorInvoker(activeIf = "min_version=1.21.2")
    Object newInstance$0(@Type(clazz = ResourceKeyProxy.class) Object id, @Type(clazz = RecipeProxy.class) Object value);

    @ConstructorInvoker(activeIf = "max_version=1.21.1")
    Object newInstance$1(@Type(clazz = IdentifierProxy.class) Object id, @Type(clazz = RecipeProxy.class) Object value);
}
