package net.momirealms.craftengine.bukkit.plugin.classpath;

import net.momirealms.craftengine.core.plugin.classpath.ClassPathAppender;
import net.momirealms.craftengine.core.plugin.classpath.URLClassLoaderAccess;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Optional;

public class PaperClassPathAppender implements ClassPathAppender {
    public static final Class<?> clazz$PaperPluginClassLoader = ReflectionUtils.getClazz(
            "io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader"
    );
    public static final Field field$PaperPluginClassLoader$libraryLoader = Optional.ofNullable(clazz$PaperPluginClassLoader)
            .map(it -> ReflectionUtils.getDeclaredField(it, URLClassLoader.class, 0))
            .orElse(null);
    private final URLClassLoaderAccess libraryClassLoaderAccess;

    // todo 是否有更好的方法让库被其他插件共享
    public PaperClassPathAppender(ClassLoader classLoader) {
        // 这个类加载器用于加载重定位后的依赖库，这样所有插件都能访问到
        ClassLoader bukkitClassLoader = Bukkit.class.getClassLoader();
        if (bukkitClassLoader instanceof URLClassLoader urlClassLoader) {
            this.libraryClassLoaderAccess = URLClassLoaderAccess.create(urlClassLoader);
        } else {
            // ignite会把Bukkit放置于EmberClassLoader中，获取其父DynamicClassLoader
            if (bukkitClassLoader.getClass().getName().equals("space.vectrix.ignite.launch.ember.EmberClassLoader") && bukkitClassLoader.getParent() instanceof URLClassLoader urlClassLoader) {
                this.libraryClassLoaderAccess = URLClassLoaderAccess.create(urlClassLoader);
                return;
            }
            try {
                // 最次的方案，使用paper自带的classloader去加载依赖，这种情况会发生依赖隔离
                if (clazz$PaperPluginClassLoader != null && clazz$PaperPluginClassLoader.isInstance(classLoader)) {
                    URLClassLoader libraryClassLoader = (URLClassLoader) field$PaperPluginClassLoader$libraryLoader.get(classLoader);
                    this.libraryClassLoaderAccess = URLClassLoaderAccess.create(libraryClassLoader);
                } else {
                    throw new IllegalStateException("ClassLoader is not instance of URLClassLoader");
                }
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to instantiate PaperPluginClassLoader", e);
            }
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
