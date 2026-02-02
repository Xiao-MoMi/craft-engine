package net.momirealms.craftengine.bukkit.reflection;

import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.reflection.SReflection;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.remapper.Remapper;
import net.momirealms.sparrow.reflection.util.MinecraftVersionPredicate;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Objects;

public final class ReflectionHelper {
    public static final ProxyFactory PROXY_FACTORY;

    static {
        final ClassLoader classLoader = ReflectionUtils.class.getClassLoader();
        final String reflectionPath = "net/momirealms/sparrow/reflection/";
        final MethodHandles.Lookup lookup = SReflection.LOOKUP;
        try {
            try (InputStream is = classLoader.getResourceAsStream(reflectionPath + "field/SField.class")) {
                SReflection.defineClass(lookup, Objects.requireNonNull(is).readAllBytes());
            }
            try (InputStream is = classLoader.getResourceAsStream(reflectionPath + "field/SShortField.class")) {
                SReflection.defineClass(lookup, Objects.requireNonNull(is).readAllBytes());
            }
            try (InputStream is = classLoader.getResourceAsStream(reflectionPath + "field/SLongField.class")) {
                SReflection.defineClass(lookup, Objects.requireNonNull(is).readAllBytes());
            }
            try (InputStream is = classLoader.getResourceAsStream(reflectionPath + "field/SIntField.class")) {
                SReflection.defineClass(lookup, Objects.requireNonNull(is).readAllBytes());
            }
            try (InputStream is = classLoader.getResourceAsStream(reflectionPath + "field/SFloatField.class")) {
                SReflection.defineClass(lookup, Objects.requireNonNull(is).readAllBytes());
            }
            try (InputStream is = classLoader.getResourceAsStream(reflectionPath + "field/SDoubleField.class")) {
                SReflection.defineClass(lookup, Objects.requireNonNull(is).readAllBytes());
            }
            try (InputStream is = classLoader.getResourceAsStream(reflectionPath + "field/SCharField.class")) {
                SReflection.defineClass(lookup, Objects.requireNonNull(is).readAllBytes());
            }
            try (InputStream is = classLoader.getResourceAsStream(reflectionPath + "field/SByteField.class")) {
                SReflection.defineClass(lookup, Objects.requireNonNull(is).readAllBytes());
            }
            try (InputStream is = classLoader.getResourceAsStream(reflectionPath + "field/SBooleanField.class")) {
                SReflection.defineClass(lookup, Objects.requireNonNull(is).readAllBytes());
            }
            try (InputStream is = classLoader.getResourceAsStream(reflectionPath + "method/SMethod.class")) {
                SReflection.defineClass(lookup, Objects.requireNonNull(is).readAllBytes());
            }
            try (InputStream is = classLoader.getResourceAsStream(reflectionPath + "constructor/SConstructor.class")) {
                SReflection.defineClass(lookup, Objects.requireNonNull(is).readAllBytes());
            }
            for (int i = 0; i < 11; i++) {
                try (InputStream is = classLoader.getResourceAsStream(reflectionPath + "method/SMethod" + i + ".class")) {
                    SReflection.defineClass(lookup, Objects.requireNonNull(is).readAllBytes());
                }
                try (InputStream is = classLoader.getResourceAsStream(reflectionPath + "constructor/SConstructor" + i + ".class")) {
                    SReflection.defineClass(lookup, Objects.requireNonNull(is).readAllBytes());
                }
            }
        } catch (Throwable e) {
            throw new ReflectionInitException(e);
        }
        SReflection.setAsmClassPrefix("CraftEngine");
        SReflection.setRemapper(VersionHelper.isMojmap() ? Remapper.createFromPaperJar() : WithCraftBukkitClassNameRemapper.INSTANCE);
        SReflection.setVersionMatcher(new MinecraftVersionPredicate(VersionHelper.MINECRAFT_VERSION.version()));
        PROXY_FACTORY = ProxyFactory.create(ReflectionHelper.class.getClassLoader());
    }

    private static final class WithCraftBukkitClassNameRemapper implements Remapper {
        private static final WithCraftBukkitClassNameRemapper INSTANCE = new WithCraftBukkitClassNameRemapper();
        private static final String PREFIX_CRAFTBUKKIT = "org.bukkit.craftbukkit";
        private static final String CB_PKG_VERSION;
        private final Remapper remapper = Remapper.createFromPaperJar();

        static {
            if (VersionHelper.isMojmap()) {
                CB_PKG_VERSION = ".";
            } else {
                String name;
                label: {
                    for (int i = 0; i <= VersionHelper.minorVersion(); i++) {
                        try {
                            name = ".v1_" + VersionHelper.majorVersion() + "_R" + i + ".";
                            Class.forName(PREFIX_CRAFTBUKKIT + name + "CraftServer");
                            break label;
                        } catch (ClassNotFoundException ignored) {
                        }
                    }
                    throw new RuntimeException("Could not find CraftServer version");
                }
                CB_PKG_VERSION = name;
            }
        }

        @Override
        public String remapClassName(String className) {
            if (PREFIX_CRAFTBUKKIT.startsWith(className)) {
                return PREFIX_CRAFTBUKKIT + CB_PKG_VERSION + className.substring(PREFIX_CRAFTBUKKIT.length());
            }
            return this.remapper.remapClassName(className);
        }

        @Override
        public String remapFieldName(Class<?> clazz, String fieldName) {
            return this.remapper.remapFieldName(clazz, fieldName);
        }

        @Override
        public String remapMethodName(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
            return this.remapper.remapMethodName(clazz, methodName, parameterTypes);
        }
    }

    public static void init() {
    }

    public static <T> T getProxy(Class<T> proxyClass) {
        return PROXY_FACTORY.newJavaProxy(proxyClass);
    }

    @SuppressWarnings("DataFlowIssue")
    @NotNull // 停止npe警告，如果需要警告在对应代理类中特别标注即可
    public static Class<?> getClass(Class<?> clazz) {
        ReflectionProxy proxy = clazz.getDeclaredAnnotation(ReflectionProxy.class);
        if (proxy == null) {
            throw new IllegalArgumentException("Class " + clazz + " has no @ReflectionProxy annotation");
        }
        if (SReflection.getVersionMatcher().test(proxy.version())) {
            if (proxy.clazz() == Object.class && proxy.name().isEmpty() && proxy.names().length == 0) {
                throw new IllegalArgumentException("ReflectionProxy doesn't have value or class name set for class " + clazz);
            }
            if (proxy.clazz() != Object.class) {
                return proxy.clazz();
            }
            if (!proxy.name().isEmpty()) {
                if (proxy.ignoreRelocation()) {
                    return SparrowClass.find(proxy.name().replace("{}", "."));
                } else {
                    return SparrowClass.find(proxy.name());
                }
            }
            if (proxy.ignoreRelocation()) {
                return SparrowClass.find(Arrays.stream(proxy.names()).map(it -> it.replace("{}", ".")).toArray(String[]::new));
            } else {
                return SparrowClass.find(proxy.names());
            }
        } else {
            return null;
        }
    }
}
