package net.momirealms.craftengine.core.plugin.classpath;

import net.momirealms.craftengine.core.util.ReflectionUtils;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public final class URLClassPathAppender implements ClassPathAppender {
    private final MethodHandle addURL;

    @SuppressWarnings("resource")
    public URLClassPathAppender(ClassLoader classLoader) {
        try {
            URLClassLoader urlClassLoader = findURLClassLoader(classLoader); // 兼容类似ignite的加载器
            if (urlClassLoader != null) {
                classLoader = urlClassLoader;
            }
            Field field = findURLClassPathField(classLoader.getClass()); // 尝试获取ucp字段
            Object ucp = ReflectionUtils.LOOKUP.unreflectGetter(field).invoke(classLoader);
            this.addURL = ReflectionUtils.LOOKUP.unreflect(ucp.getClass().getDeclaredMethod("addURL", URL.class)).bindTo(ucp);
        } catch (Throwable t) {
            throw new UnsupportedOperationException("Unsupported classloader " + classLoader.getClass(), t);
        }
    }

    private static Field findURLClassPathField(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        try {
            return clazz.getDeclaredField("ucp");
        } catch (NoSuchFieldException ignored) {
        }
        return findURLClassPathField(clazz.getSuperclass());
    }

    private static URLClassLoader findURLClassLoader(ClassLoader classLoader) {
        if (classLoader instanceof URLClassLoader urlClassLoader) {
            return urlClassLoader;
        }
        ClassLoader parent = classLoader.getParent();
        return parent == null ? null : findURLClassLoader(parent);
    }

    @Override
    public void addJarToClasspath(Path file) {
        try {
            URL url = file.toUri().toURL();
            this.addURL.invokeExact(url);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
