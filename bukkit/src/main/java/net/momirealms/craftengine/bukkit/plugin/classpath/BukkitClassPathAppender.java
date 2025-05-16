package net.momirealms.craftengine.bukkit.plugin.classpath;

import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.plugin.Plugin;
import net.momirealms.craftengine.core.plugin.classpath.ClassPathAppender;
import net.momirealms.craftengine.core.plugin.classpath.URLClassLoaderAccess;

import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class BukkitClassPathAppender implements ClassPathAppender {
    private final URLClassLoaderAccess classLoaderAccess;

    public BukkitClassPathAppender(ClassLoader classLoader) throws IllegalAccessException {
        if (Reflections.clazz$PaperPluginClassLoader != null && Reflections.clazz$PaperPluginClassLoader.isInstance(classLoader)) {
            URLClassLoader libraryClassLoader = (URLClassLoader) Reflections.field$PaperPluginClassLoader$libraryLoader.get(classLoader);
            this.classLoaderAccess = URLClassLoaderAccess.create(libraryClassLoader);
        } else if (classLoader instanceof URLClassLoader) {
            this.classLoaderAccess = URLClassLoaderAccess.create((URLClassLoader) classLoader);
        } else {
            throw new IllegalStateException("ClassLoader is not instance of URLClassLoader");
        }
    }

    public BukkitClassPathAppender(Plugin plugin) throws IllegalAccessException {
        this(plugin.getClass().getClassLoader());
    }

    @Override
    public void addJarToClasspath(Path file) {
        try {
            this.classLoaderAccess.addURL(file.toUri().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
