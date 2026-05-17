package net.momirealms.craftengine.proxy.minecraft.world.phys.shapes;

import net.momirealms.craftengine.proxy.minecraft.world.phys.AABBProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.phys.shapes.Shapes")
public interface ShapesProxy {
    ShapesProxy INSTANCE = ASMProxyFactory.create(ShapesProxy.class);

    @MethodInvoker(name = "create", isStatic = true)
    Object create(@Type(clazz = AABBProxy.class) Object aabb);

    @MethodInvoker(name = "or", isStatic = true)
    Object or(@Type(clazz = VoxelShapeProxy.class) Object first, @Type(clazz = VoxelShapeProxy.class) Object second);

    @MethodInvoker(name = "joinIsNotEmpty", isStatic = true)
    boolean joinIsNotEmpty(@Type(clazz = VoxelShapeProxy.class) Object shape1,
                           @Type(clazz = VoxelShapeProxy.class) Object shape2,
                           @Type(clazz = BooleanOpProxy.class) Object predicate);
}
