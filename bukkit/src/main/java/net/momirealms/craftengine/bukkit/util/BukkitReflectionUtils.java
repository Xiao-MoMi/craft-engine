package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.util.ReflectionUtils;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.Objects;

public final class BukkitReflectionUtils {
    private static final String PREFIX_MC = "net.minecraft.";
    private static final String PREFIX_CRAFTBUKKIT = "org.bukkit.craftbukkit";
    private static final String CRAFT_SERVER = "CraftServer";
    private static final String CB_PKG_VERSION;
    public static final int MAJOR_REVISION;

    private BukkitReflectionUtils() {}

    static {
        final Class<?> serverClass;
        if (Bukkit.getServer() == null) {
            // Paper plugin Bootstrapper 1.20.6+
            serverClass = Objects.requireNonNull(ReflectionUtils.getClazz("org.bukkit.craftbukkit.CraftServer"));
        } else {
            serverClass = Bukkit.getServer().getClass();
        }
        final String pkg = serverClass.getPackage().getName();
        final String nmsVersion = pkg.substring(pkg.lastIndexOf(".") + 1);
        if (!nmsVersion.contains("_")) {
            int fallbackVersion = -1;
            if (Bukkit.getServer() != null) {
                try {
                    final Method getMinecraftVersion = serverClass.getDeclaredMethod("getMinecraftVersion");
                    fallbackVersion = Integer.parseInt(getMinecraftVersion.invoke(Bukkit.getServer()).toString().split("\\.")[1]);
                } catch (final Exception ignored) {
                }
            } else {
                // Paper plugin bootstrapper 1.20.6+
                try {
                    final Class<?> sharedConstants = Objects.requireNonNull(ReflectionUtils.getClazz("net.minecraft.SharedConstants"));
                    final Method getCurrentVersion = sharedConstants.getDeclaredMethod("getCurrentVersion");
                    final Object currentVersion = getCurrentVersion.invoke(null);
                    final Method getName = currentVersion.getClass().getDeclaredMethod("getName");
                    final String versionName = (String) getName.invoke(currentVersion);
                    try {
                        fallbackVersion = Integer.parseInt(versionName.split("\\.")[1]);
                    } catch (final Exception ignored) {
                    }
                } catch (final ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }
            MAJOR_REVISION = fallbackVersion;
        } else {
            MAJOR_REVISION = Integer.parseInt(nmsVersion.split("_")[1]);
        }
        String name = serverClass.getName();
        name = name.substring(PREFIX_CRAFTBUKKIT.length());
        name = name.substring(0, name.length() - CRAFT_SERVER.length());
        CB_PKG_VERSION = name;
    }

    public static String assembleCBClass(String className) {
        return PREFIX_CRAFTBUKKIT + CB_PKG_VERSION + className;
    }

    public static String assembleMCClass(String className) {
        return PREFIX_MC + className;
    }
}
