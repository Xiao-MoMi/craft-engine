package net.momirealms.craftengine.proxy;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.remapper.Remapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class CraftBukkitRemapper implements Remapper {
    private static final String PREFIX_CRAFTBUKKIT = "org.bukkit.craftbukkit.";
    private static final String CB_PKG_VERSION;
    private static final boolean NEED_REMAP;
    private final Remapper delegate;

    private CraftBukkitRemapper(Remapper delegate) {
        this.delegate = delegate;
    }

    static {
        if (SparrowClass.existsNoRemap("net.neoforged.art.internal.RenamerImpl")) {
            CB_PKG_VERSION = "";
            NEED_REMAP = false;
        } else {
            Class<?> minecraftClass = SparrowClass.find(
                    "net.minecraft.obfuscate.DontObfuscate",
                    "net.minecraft.server.Main"
            );
            int major;
            int minor;
            try (InputStream inputStream = minecraftClass.getResourceAsStream("/version.json")) {
                if (inputStream == null) {
                    throw new IOException("Failed to load version.json");
                }
                JsonObject json = new Gson().fromJson(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
                String versionString = json.getAsJsonPrimitive("id").getAsString()
                        .split("-", 2)[0]
                        .split("_", 2)[0];
                String[] split = versionString.split("\\.");
                major = Integer.parseInt(split[1]);
                minor = split.length == 3 ? Integer.parseInt(split[2]) : 0;
            } catch (Exception e) {
                throw new RuntimeException("Failed to init WithCraftBukkitClassNameRemapper", e);
            }
            String name;
            label: {
                for (int i = 0; i <= minor; i++) {
                    try {
                        name = "v1_" + major + "_R" + i + ".";
                        Class.forName(PREFIX_CRAFTBUKKIT + name + "CraftServer");
                        break label;
                    } catch (ClassNotFoundException ignored) {
                    }
                }
                throw new RuntimeException("Could not find CraftServer version");
            }
            CB_PKG_VERSION = name;
            NEED_REMAP = true;
        }
    }

    public static Remapper create(Remapper delegate) {
        if (NEED_REMAP) {
            return new CraftBukkitRemapper(delegate);
        } else {
            return delegate;
        }
    }

    @Override
    public String remapClassName(String className) {
        if (!className.startsWith(PREFIX_CRAFTBUKKIT)) {
            return this.delegate.remapClassName(className);
        }
        return PREFIX_CRAFTBUKKIT + CB_PKG_VERSION + className.substring(PREFIX_CRAFTBUKKIT.length());
    }

    @Override
    public String remapFieldName(Class<?> clazz, String fieldName) {
        return this.delegate.remapFieldName(clazz, fieldName);
    }

    @Override
    public String remapMethodName(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return this.delegate.remapMethodName(clazz, methodName, parameterTypes);
    }
}
