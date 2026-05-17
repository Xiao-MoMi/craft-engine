package net.momirealms.craftengine.bukkit.plugin.classpath;

import net.momirealms.craftengine.core.plugin.classpath.ClassPathAppender;
import net.momirealms.craftengine.core.plugin.classpath.URLClassLoaderAccess;
import net.momirealms.craftengine.core.util.ReflectionUtils;

import java.lang.invoke.MethodHandle;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Optional;

public final class PaperPluginClassPathAppender implements ClassPathAppender {
    public static final Class<?> clazz$PaperPluginClassLoader = ReflectionUtils.getClazz(
            "io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader"
    );
    public static final MethodHandle methodHandle$PaperPluginClassLoader$libraryLoaderGetter = Optional.ofNullable(clazz$PaperPluginClassLoader)
            .map(it -> ReflectionUtils.getDeclaredField(it, URLClassLoader.class, 0))
            .map(ReflectionUtils::unreflectGetter)
            .orElseThrow();
    private final URLClassLoaderAccess libraryClassLoaderAccess;

    public PaperPluginClassPathAppender(ClassLoader classLoader) {
        try {
            // 使用paper自带的classloader去加载依赖，这种情况会发生依赖隔离
            if (clazz$PaperPluginClassLoader != null && clazz$PaperPluginClassLoader.isInstance(classLoader)) {
                URLClassLoader libraryClassLoader = (URLClassLoader) methodHandle$PaperPluginClassLoader$libraryLoaderGetter.invoke(classLoader);
                this.libraryClassLoaderAccess = URLClassLoaderAccess.create(libraryClassLoader);
            } else {
                throw new IllegalStateException("ClassLoader is not instance of URLClassLoader");
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to instantiate PaperPluginClassLoader", e);
        }
    }

    @Override
    public void addJarToClasspath(Path file) {
        try {
            this.libraryClassLoaderAccess.addURL(file.toUri().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
