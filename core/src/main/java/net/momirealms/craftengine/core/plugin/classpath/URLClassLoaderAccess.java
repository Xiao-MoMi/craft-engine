package net.momirealms.craftengine.core.plugin.classpath;

import net.momirealms.craftengine.core.util.ReflectionUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

public abstract class URLClassLoaderAccess {

    public static URLClassLoaderAccess create(URLClassLoader classLoader) {
        if (Reflection.isSupported()) {
            return new Reflection(classLoader);
        } else if (Lookup.isSupported()) {
            return new Lookup(classLoader);
        } else {
            return Noop.INSTANCE;
        }
    }

    private final URLClassLoader classLoader;

    protected URLClassLoaderAccess(URLClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public abstract void addURL(@NotNull URL url);

    private static void throwError(Throwable cause) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("""
                Plugin is unable to inject into the plugin URLClassLoader.
                You may be able to fix this problem by adding the following command-line argument \
                directly after the 'java' command in your start script:\s
                '--add-opens java.base/java.lang=ALL-UNNAMED'""", cause);
    }

    private static class Reflection extends URLClassLoaderAccess {
        private static final Method ADD_URL_METHOD;

        static {
            Method addUrlMethod;
            try {
                addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                addUrlMethod.setAccessible(true);
            } catch (Exception e) {
                addUrlMethod = null;
            }
            ADD_URL_METHOD = addUrlMethod;
        }

        private static boolean isSupported() {
            return ADD_URL_METHOD != null;
        }

        Reflection(URLClassLoader classLoader) {
            super(classLoader);
        }

        @Override
        public void addURL(@NotNull URL url) {
            try {
                ADD_URL_METHOD.invoke(super.classLoader, url);
            } catch (ReflectiveOperationException e) {
                URLClassLoaderAccess.throwError(e);
            }
        }
    }

    private static class Lookup extends URLClassLoaderAccess {
        private static final MethodHandles.Lookup LOOKUP;

        static {
            MethodHandles.Lookup lookup;
            try {
                lookup = ReflectionUtils.LOOKUP;
            } catch (Throwable t) {
                lookup = null;
            }
            LOOKUP = lookup;
        }

        private static boolean isSupported() {
            return LOOKUP != null;
        }

        private final Collection<URL> unopenedURLs;
        private final Collection<URL> pathURLs;

        @SuppressWarnings("unchecked")
        Lookup(URLClassLoader classLoader) {
            super(classLoader);

            Collection<URL> unopenedURLs;
            Collection<URL> pathURLs;
            try {
                Object ucp = fetchField(URLClassLoader.class, classLoader, "ucp");
                unopenedURLs = (Collection<URL>) fetchField(ucp.getClass(), ucp, "unopenedUrls");
                pathURLs = (Collection<URL>) fetchField(ucp.getClass(), ucp, "path");
            } catch (Throwable e) {
                unopenedURLs = null;
                pathURLs = null;
            }

            this.unopenedURLs = unopenedURLs;
            this.pathURLs = pathURLs;
        }

        private static Object fetchField(final Class<?> clazz, final Object object, final String name) throws Throwable {
            Field field = clazz.getDeclaredField(name);
            return LOOKUP.unreflectGetter(field).invoke(object);
        }

        @Override
        public void addURL(@NotNull URL url) {
            if (this.unopenedURLs == null || this.pathURLs == null) {
                URLClassLoaderAccess.throwError(new NullPointerException("unopenedURLs or pathURLs"));
            }

            synchronized (this.unopenedURLs)  {
                this.unopenedURLs.add(url);
                this.pathURLs.add(url);
            }
        }
    }

    private static class Noop extends URLClassLoaderAccess {
        private static final Noop INSTANCE = new Noop();

        private Noop() {
            super(null);
        }

        @Override
        public void addURL(@NotNull URL url) {
            URLClassLoaderAccess.throwError(null);
        }
    }
}
