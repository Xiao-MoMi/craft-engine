package net.momirealms.craftengine.proxy.minecraft.commands.arguments;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

public interface EntityAnchorArgumentProxy {

    @ReflectionProxy(name = "net.minecraft.commands.arguments.EntityAnchorArgument$Anchor")
    interface AnchorProxy {
        AnchorProxy INSTANCE = ASMProxyFactory.create(AnchorProxy.class);
        Class<?> CLASS = SparrowClass.find("net.minecraft.commands.arguments.EntityAnchorArgument$Anchor");
        Object EYES = INSTANCE.getEyes();

        @FieldGetter(name = "EYES", isStatic = true)
        Object getEyes();
    }
}
