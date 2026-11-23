package net.momirealms.craftengine.bukkit.plugin.injector;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import java.lang.invoke.MethodHandles;

final class InjectorClassLoading {
    private InjectorClassLoading() {
    }

    static Class<?> load(DynamicType.Builder<?> builder) {
        return builder.make()
                .load(InjectorClassLoading.class.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(MethodHandles.lookup()))
                .getLoaded();
    }
}
