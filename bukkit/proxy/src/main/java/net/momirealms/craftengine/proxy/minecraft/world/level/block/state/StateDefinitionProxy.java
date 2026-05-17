package net.momirealms.craftengine.proxy.minecraft.world.level.block.state;

import com.google.common.collect.ImmutableList;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

import java.util.function.Function;

@ReflectionProxy(name = "net.minecraft.world.level.block.state.StateDefinition")
public interface StateDefinitionProxy {
    StateDefinitionProxy INSTANCE = ASMProxyFactory.create(StateDefinitionProxy.class);

    @FieldGetter(name = "states")
    ImmutableList<Object> getStates(Object target);

    @FieldSetter(name = "states")
    void setStates(Object target, ImmutableList<Object> states);

    @ReflectionProxy(name = "net.minecraft.world.level.block.state.StateDefinition$Builder")
    interface BuilderProxy {
        BuilderProxy INSTANCE = ASMProxyFactory.create(BuilderProxy.class);

        @ConstructorInvoker
        Object newInstance(Object owner);

        @MethodInvoker(name = "create")
        Object create(Object target, Function<Object, Object> defaultStateGetter, @Type(clazz = FactoryProxy.class) Object factory);
    }

    @ReflectionProxy(name = "net.minecraft.world.level.block.state.StateDefinition$Factory")
    interface FactoryProxy {
        FactoryProxy INSTANCE = ASMProxyFactory.create(FactoryProxy.class);
        Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.block.state.StateDefinition$Factory");
    }
}
