package net.momirealms.craftengine.proxy.minecraft.world.phys;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.phys.Vec3")
public interface Vec3Proxy {
    Vec3Proxy INSTANCE = ASMProxyFactory.create(Vec3Proxy.class);
    Object ZERO = INSTANCE.getZERO();

    @FieldGetter(name = "ZERO", isStatic = true)
    Object getZERO();
}
