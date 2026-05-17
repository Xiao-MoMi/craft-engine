package net.momirealms.craftengine.proxy.minecraft.world.level.block.entity;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.entity.CampfireBlockEntity")
public interface CampfireBlockEntityProxy extends BaseContainerBlockEntityProxy {
    AbstractFurnaceBlockEntityProxy INSTANCE = ASMProxyFactory.create(AbstractFurnaceBlockEntityProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.block.entity.CampfireBlockEntity");

    @FieldGetter(name = "quickCheck", activeIf = "max_version=1.21.2")
    Object getQuickCheck(Object target);

    @FieldSetter(name = "quickCheck", activeIf = "max_version=1.21.2")
    void setQuickCheck(Object target, Object value);

}

