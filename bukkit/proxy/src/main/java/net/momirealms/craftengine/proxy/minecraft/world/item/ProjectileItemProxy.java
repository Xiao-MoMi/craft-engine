package net.momirealms.craftengine.proxy.minecraft.world.item;

import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.core.PositionProxy;
import net.momirealms.craftengine.proxy.minecraft.core.dispenser.BlockSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.projectile.ProjectileProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Optional;
import java.util.OptionalInt;

@ReflectionProxy(name = "net.minecraft.world.item.ProjectileItem", activeIf = "min_version=1.20.5")
public interface ProjectileItemProxy {
    ProjectileItemProxy INSTANCE = ASMProxyFactory.create(ProjectileItemProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.ProjectileItem");

    @MethodInvoker(name = "createDispenseConfig")
    Object createDispenseConfig(Object target);

    @MethodInvoker(name = "asProjectile")
    Object asProjectile(Object target,
                        @Type(clazz = LevelProxy.class) Object level,
                        @Type(clazz = PositionProxy.class) Object position,
                        @Type(clazz = ItemStackProxy.class) Object stack,
                        @Type(clazz = DirectionProxy.class) Object direction);

    @MethodInvoker(name = "shoot", activeIf = "max_version=1.21.1")
    void shoot(Object target, @Type(clazz = ProjectileProxy.class) Object projectile, double x, double y, double z, float power, float uncertainty);

    @ReflectionProxy(name = "net.minecraft.world.item.ProjectileItem$DispenseConfig", activeIf = "min_version=1.20.5")
    interface DispenseConfigProxy {
        DispenseConfigProxy INSTANCE = ASMProxyFactory.create(DispenseConfigProxy.class);

        @MethodInvoker(name = "positionFunction")
        Object positionFunction(Object target);

        @MethodInvoker(name = "power")
        float power(Object target);

        @MethodInvoker(name = "uncertainty")
        float uncertainty(Object target);

        @MethodInvoker(name = "overrideDispenseEvent", activeIf = "max_version=26.2")
        OptionalInt overrideDispenseEvent$legacy(Object target);

        @MethodInvoker(name = "overrideDispenseEvent", activeIf = "min_version=26.3")
        Optional<Integer> overrideDispenseEvent(Object target);
    }

    @ReflectionProxy(name = "net.minecraft.world.item.ProjectileItem$PositionFunction", activeIf = "min_version=1.20.5")
    interface PositionFunctionProxy {
        PositionFunctionProxy INSTANCE = ASMProxyFactory.create(PositionFunctionProxy.class);

        @MethodInvoker(name = "getDispensePosition")
        Object getDispensePosition(Object target, @Type(clazz = BlockSourceProxy.class) Object source, @Type(clazz = DirectionProxy.class) Object direction);
    }
}
