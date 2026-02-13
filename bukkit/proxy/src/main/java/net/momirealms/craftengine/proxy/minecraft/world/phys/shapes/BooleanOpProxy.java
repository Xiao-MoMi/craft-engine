package net.momirealms.craftengine.proxy.minecraft.world.phys.shapes;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.phys.shapes.BooleanOp")
public interface BooleanOpProxy {
    BooleanOpProxy INSTANCE = ASMProxyFactory.create(BooleanOpProxy.class);
    Object AND = INSTANCE.getAnd();

    @FieldGetter(name = "AND", isStatic = true)
    Object getAnd();
}
