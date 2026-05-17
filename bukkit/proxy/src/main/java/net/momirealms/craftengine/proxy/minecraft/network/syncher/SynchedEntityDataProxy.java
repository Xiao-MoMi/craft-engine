package net.momirealms.craftengine.proxy.minecraft.network.syncher;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

import java.util.List;

@ReflectionProxy(name = "net.minecraft.network.syncher.SynchedEntityData")
public interface SynchedEntityDataProxy {
    SynchedEntityDataProxy INSTANCE = ASMProxyFactory.create(SynchedEntityDataProxy.class);

    @MethodInvoker(name = "getNonDefaultValues")
    List<Object> getNonDefaultValues(Object target);

    @MethodInvoker(name = "packAll")
    List<Object> packAll(Object target);

    @MethodInvoker(name = "set")
    void set(Object target, @Type(clazz = EntityDataAccessorProxy.class) Object key, Object value, boolean force);

    @MethodInvoker(name = "get")
    <T> T get(Object target, @Type(clazz = EntityDataAccessorProxy.class) Object key);

    @ReflectionProxy(name = "net.minecraft.network.syncher.SynchedEntityData$DataValue")
    interface DataValueProxy {
        DataValueProxy INSTANCE = ASMProxyFactory.create(DataValueProxy.class);

        @ConstructorInvoker
        Object newInstance(int id, @Type(clazz = EntityDataSerializerProxy.class) Object serializer, Object value);

        @MethodInvoker(name = "create", isStatic = true)
        Object create(@Type(clazz = EntityDataAccessorProxy.class) Object dataAccessor, Object value);

        @FieldGetter(name = "id")
        int getId(Object target);

        @FieldGetter(name = "serializer")
        Object getSerializer(Object target);

        @FieldGetter(name = "value")
        <T> T getValue(Object target);

        @FieldSetter(name = "value")
        void setValue(Object target, Object value);
    }
}
