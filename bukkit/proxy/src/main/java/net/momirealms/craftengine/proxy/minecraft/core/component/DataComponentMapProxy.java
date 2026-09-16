package net.momirealms.craftengine.proxy.minecraft.core.component;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

import java.util.Set;

@ReflectionProxy(name = "net.minecraft.core.component.DataComponentMap", activeIf = "min_version=1.20.5")
public interface DataComponentMapProxy extends DataComponentGetterProxy {
    DataComponentMapProxy INSTANCE = ASMProxyFactory.create(DataComponentMapProxy.class);

    @MethodInvoker(name = "get", activeIf = "max_version=1.21.4")
    <T> T get(Object target, @Type(clazz = DataComponentTypeProxy.class) Object type);

    @MethodInvoker(name = "keySet")
    Set<Object> keySet(Object target);

    @MethodInvoker(name = "builder", isStatic = true)
    Object builder();

    @ReflectionProxy(name = "net.minecraft.core.component.DataComponentMap$Builder", activeIf = "min_version=1.20.5")
    interface BuilderProxy {
        BuilderProxy INSTANCE = ASMProxyFactory.create(BuilderProxy.class);

        @MethodInvoker(name = "addAll")
        Object addAll(Object target, @Type(clazz = DataComponentMapProxy.class) Object components);

        @MethodInvoker(name = "build")
        Object build(Object target);
    }

    @ReflectionProxy(name = "net.minecraft.core.component.DataComponentMap$Builder$SimpleMap", activeIf = "min_version=1.20.5")
    interface SimpleMapProxy {
        SimpleMapProxy INSTANCE = ASMProxyFactory.create(SimpleMapProxy.class);

        @FieldGetter(name = "map")
        Reference2ObjectMap<Object, Object> getMap(Object target);

        @FieldSetter(name = "map")
        void setMap(Object target, Reference2ObjectMap<Object, Object> values);
    }
}
