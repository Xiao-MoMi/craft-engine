package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.world.level.block.BonemealSource", activeIf = "min_version=26.3")
public interface BonemealSourceProxy {
    BonemealSourceProxy INSTANCE = ASMProxyFactory.create(BonemealSourceProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.block.BonemealSource");
    Object INTERACTION = INSTANCE.getInteraction();
    @FieldGetter(name = "INTERACTION", isStatic = true)
    Object getInteraction();
}
