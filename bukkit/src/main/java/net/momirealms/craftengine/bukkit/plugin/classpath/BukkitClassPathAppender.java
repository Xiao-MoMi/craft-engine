package net.momirealms.craftengine.bukkit.plugin.classpath;

import net.momirealms.craftengine.core.plugin.classpath.ClassPathAppender;
import net.momirealms.craftengine.core.plugin.classpath.URLClassLoaderAccess;
import org.bukkit.Bukkit;

import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class BukkitClassPathAppender implements ClassPathAppender {
    private final URLClassLoaderAccess libraryClassLoaderAccess;

    public BukkitClassPathAppender() {
        // 这个类加载器用于加载重定位后的依赖库，这样所有插件都能访问到
        ClassLoader bukkitClassLoader = Bukkit.class.getClassLoader();
        URLClassLoader urlClassLoader = findURLClassLoader(bukkitClassLoader);
        if (urlClassLoader != null) {
            this.libraryClassLoaderAccess = URLClassLoaderAccess.create(urlClassLoader);
        } else {
            throw new UnsupportedOperationException("Unsupported classloader " + bukkitClassLoader.getClass());
        }
    }

    private static URLClassLoader findURLClassLoader(ClassLoader classLoader) {
        if (classLoader instanceof URLClassLoader urlClassLoader) return urlClassLoader;
        ClassLoader parent = classLoader.getParent();
        return parent == null ? null : findURLClassLoader(parent);
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
