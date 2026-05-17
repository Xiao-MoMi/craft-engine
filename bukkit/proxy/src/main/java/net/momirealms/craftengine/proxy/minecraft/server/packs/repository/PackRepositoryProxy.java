package net.momirealms.craftengine.proxy.minecraft.server.packs.repository;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;

@ReflectionProxy(name = "net.minecraft.server.packs.repository.PackRepository")
public interface PackRepositoryProxy {
    PackRepositoryProxy INSTANCE = ASMProxyFactory.create(PackRepositoryProxy.class);

    @FieldGetter(name = "selected")
    List<Object> getSelected(Object target);
}
