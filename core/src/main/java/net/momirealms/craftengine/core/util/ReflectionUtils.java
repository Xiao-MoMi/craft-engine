package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReflectionUtils {

    private ReflectionUtils() {}

    public static Class<?> getClazz(String... classes) {
        for (String className : classes) {
            Class<?> clazz = getClazz(className);
            if (clazz != null) {
                return clazz;
            }
        }
        return null;
    }

    public static Class<?> getClazz(String clazz) {
        try {
            return Class.forName(clazz);
        } catch (Throwable e) {
            return null;
        }
    }

    public static boolean classExists(@NotNull final String clazz) {
        try {
            Class.forName(clazz);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean methodExists(@NotNull final Class<?> clazz, @NotNull final String method, @NotNull final Class<?>... parameterTypes) {
        try {
            clazz.getMethod(method, parameterTypes);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @Nullable
    public static Field getDeclaredField(final Class<?> clazz, final String field) {
        try {
            return setAccessible(clazz.getDeclaredField(field));
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    @NotNull
    public static Field getDeclaredField(@NotNull Class<?> clazz, @NotNull String... possibleNames) {
        List<String> possibleNameList = Arrays.asList(possibleNames);
        for (Field field : clazz.getDeclaredFields()) {
            if (possibleNameList.contains(field.getName())) {
                return field;
            }
        }
        throw new RuntimeException("Class " + clazz.getName() + " does not contain a field with possible names " + Arrays.toString(possibleNames));
    }

    @Nullable
    public static Field getDeclaredField(final Class<?> clazz, final int index) {
        int i = 0;
        for (final Field field : clazz.getDeclaredFields()) {
            if (index == i) {
                return setAccessible(field);
            }
            i++;
        }
        return null;
    }

    @Nullable
    public static Field getInstanceDeclaredField(final Class<?> clazz, final int index) {
        int i = 0;
        for (final Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                if (index == i) {
                    return setAccessible(field);
                }
                i++;
            }
        }
        return null;
    }

    @Nullable
    public static Field getDeclaredField(final Class<?> clazz, final Class<?> type, int index) {
        int i = 0;
        for (final Field field : clazz.getDeclaredFields()) {
            if (field.getType() == type) {
                if (index == i) {
                    return setAccessible(field);
                }
                i++;
            }
        }
        return null;
    }

    @Nullable
    public static Field getDeclaredFieldBackwards(final Class<?> clazz, final Class<?> type, int index) {
        int i = 0;
        Field[] fields = clazz.getDeclaredFields();
        for (int j = fields.length - 1; j >= 0; j--) {
            Field field = fields[j];
            if (field.getType() == type) {
                if (index == i) {
                    return setAccessible(field);
                }
                i++;
            }
        }
        return null;
    }

    @Nullable
    public static Field getInstanceDeclaredField(@NotNull Class<?> clazz, final Class<?> type, int index) {
        int i = 0;
        for (final Field field : clazz.getDeclaredFields()) {
            if (field.getType() == type && !Modifier.isStatic(field.getModifiers())) {
                if (index == i) {
                    return setAccessible(field);
                }
                i++;
            }
        }
        return null;
    }

    @NotNull
    public static List<Field> getDeclaredFields(final Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            fields.add(setAccessible(field));
        }
        return fields;
    }

    @NotNull
    public static List<Field> getInstanceDeclaredFields(@NotNull Class<?> clazz) {
        List<Field> list = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                list.add(setAccessible(field));
            }
        }
        return list;
    }

    @NotNull
    public static List<Field> getDeclaredFields(@NotNull final Class<?> clazz, @NotNull final Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType() == type) {
                fields.add(setAccessible(field));
            }
        }
        return fields;
    }

    @NotNull
    public static List<Field> getInstanceDeclaredFields(@NotNull Class<?> clazz, @NotNull Class<?> type) {
        List<Field> list = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType() == type && !Modifier.isStatic(field.getModifiers())) {
                list.add(setAccessible(field));
            }
        }
        return list;
    }

    @Nullable
    public static Method getMethod(final Class<?> clazz, Class<?> returnType, final String[] possibleMethodNames, final Class<?>... parameterTypes) {
        outer:
        for (Method method : clazz.getMethods()) {
            if (method.getParameterCount() != parameterTypes.length) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            for (int i = 0; i < types.length; i++) {
                if (types[i] != parameterTypes[i]) {
                    continue outer;
                }
            }
            for (String name : possibleMethodNames) {
                if (name.equals(method.getName())) {
                    if (returnType.isAssignableFrom(method.getReturnType())) {
                        return method;
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    public static Method getMethod(final Class<?> clazz, final String[] possibleMethodNames, final Class<?>... parameterTypes) {
        outer:
        for (Method method : clazz.getMethods()) {
            if (method.getParameterCount() != parameterTypes.length) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            for (int i = 0; i < types.length; i++) {
                if (types[i] != parameterTypes[i]) {
                    continue outer;
                }
            }
            for (String name : possibleMethodNames) {
                if (name.equals(method.getName())) return method;
            }
        }
        return null;
    }

    @Nullable
    public static Method getMethod(final Class<?> clazz, Class<?> returnType, final Class<?>... parameterTypes) {
        outer:
        for (Method method : clazz.getMethods()) {
            if (method.getParameterCount() != parameterTypes.length) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            for (int i = 0; i < types.length; i++) {
                if (types[i] != parameterTypes[i]) {
                    continue outer;
                }
            }
            if (returnType.isAssignableFrom(method.getReturnType())) return method;
        }
        return null;
    }

    @Nullable
    public static Method getDeclaredMethod(final Class<?> clazz, Class<?> returnType, final String[] possibleMethodNames, final Class<?>... parameterTypes) {
        outer:
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getParameterCount() != parameterTypes.length) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            for (int i = 0; i < types.length; i++) {
                if (types[i] != parameterTypes[i]) {
                    continue outer;
                }
            }
            for (String name : possibleMethodNames) {
                if (name.equals(method.getName())) {
                    if (returnType.isAssignableFrom(method.getReturnType())) {
                        return setAccessible(method);
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    public static Method getDeclaredMethod(final Class<?> clazz, Class<?> returnType, final Class<?>... parameterTypes) {
        outer:
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getParameterCount() != parameterTypes.length) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            for (int i = 0; i < types.length; i++) {
                if (types[i] != parameterTypes[i]) {
                    continue outer;
                }
            }
            if (returnType.isAssignableFrom(method.getReturnType())) return setAccessible(method);
        }
        return null;
    }

    @Nullable
    public static Method getMethod(final Class<?> clazz, Class<?> returnType, int index) {
        int i = 0;
        for (Method method : clazz.getMethods()) {
            if (returnType.isAssignableFrom(method.getReturnType())) {
                if (i == index) {
                    return setAccessible(method);
                }
                i++;
            }
        }
        return null;
    }

    @Nullable
    public static Method getStaticMethod(final Class<?> clazz, Class<?> returnType, final Class<?>... parameterTypes) {
        outer:
        for (Method method : clazz.getMethods()) {
            if (method.getParameterCount() != parameterTypes.length) {
                continue;
            }
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            for (int i = 0; i < types.length; i++) {
                if (types[i] != parameterTypes[i]) {
                    continue outer;
                }
            }
            if (returnType.isAssignableFrom(method.getReturnType()))
                return setAccessible(method);
        }
        return null;
    }

    @Nullable
    public static Method getStaticMethod(final Class<?> clazz, String[] possibleNames, Class<?> returnType, final Class<?>... parameterTypes) {
        outer:
        for (Method method : clazz.getMethods()) {
            if (method.getParameterCount() != parameterTypes.length) {
                continue;
            }
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            for (int i = 0; i < types.length; i++) {
                if (types[i] != parameterTypes[i]) {
                    continue outer;
                }
            }
            if (returnType.isAssignableFrom(method.getReturnType())) {
                for (String name : possibleNames) {
                    if (name.equals(method.getName())) {
                        return setAccessible(method);
                    }
                }
            }
        }
        return null;
    }

    public static Method getStaticMethod(final Class<?> clazz, int index) {
        int i = 0;
        for (Method method : clazz.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                if (i == index) {
                    return setAccessible(method);
                }
                i++;
            }
        }
        return null;
    }

    @Nullable
    public static Method getMethod(final Class<?> clazz, int index) {
        int i = 0;
        for (Method method : clazz.getMethods()) {
            if (i == index) {
                return setAccessible(method);
            }
            i++;
        }
        return null;
    }

    public static Method getMethodOrElseThrow(final Class<?> clazz, final String[] possibleMethodNames, final Class<?>[] parameterTypes) throws NoSuchMethodException {
        Method method = getMethod(clazz, possibleMethodNames, parameterTypes);
        if (method == null) {
            throw new NoSuchMethodException("No method found with possible names " + Arrays.toString(possibleMethodNames) + " with parameters " +
                    Arrays.toString(parameterTypes) + " in class " + clazz.getName());
        }
        return method;
    }

    @NotNull
    public static List<Method> getMethods(@NotNull Class<?> clazz, @NotNull Class<?> returnType, @NotNull Class<?>... parameterTypes) {
        List<Method> list = new ArrayList<>();
        for (Method method : clazz.getMethods()) {
            if (!returnType.isAssignableFrom(method.getReturnType()) // check type
                    || method.getParameterCount() != parameterTypes.length // check length
            ) continue;
            Class<?>[] types = method.getParameterTypes();
            outer: {
                for (int i = 0; i < types.length; i++) {
                    if (types[i] != parameterTypes[i]) {
                        break outer;
                    }
                }
                list.add(method);
            }
        }
        return list;
    }

    @NotNull
    public static <T extends AccessibleObject> T setAccessible(@NotNull final T o) {
        o.setAccessible(true);
        return o;
    }

    @Nullable
    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            return clazz.getConstructor(parameterTypes);
        } catch (NoSuchMethodException | SecurityException ignore) {
            return null;
        }
    }

    @Nullable
    public static Constructor<?> getDeclaredConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            return setAccessible(clazz.getDeclaredConstructor(parameterTypes));
        } catch (NoSuchMethodException | SecurityException ignore) {
            return null;
        }
    }

    @Nullable
    public static Constructor<?> getConstructor(Class<?> clazz, int index) {
        try {
            Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            if (index < 0 || index >= constructors.length) {
                throw new IndexOutOfBoundsException("Invalid constructor index: " + index);
            }
            return setAccessible(constructors[index]);
        } catch (SecurityException e) {
            return null;
        }
    }

    @NotNull
    public static Constructor<?> getTheOnlyConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length != 1) {
            throw new RuntimeException("This class is expected to have only one constructor but it has " + constructors.length);
        }
        return constructors[0];
    }
}
