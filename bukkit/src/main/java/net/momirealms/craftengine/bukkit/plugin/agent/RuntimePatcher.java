package net.momirealms.craftengine.bukkit.plugin.agent;

import cn.gtemc.reflection.ImplLookupGetter;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import org.bukkit.Bukkit;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class RuntimePatcher {
    private RuntimePatcher() {}

    public static void patch(BukkitCraftEngine plugin) throws Exception {
        Class<?> holderClass = new ByteBuddy()
                .subclass(Object.class)
                .name("net.momirealms.craftengine.bukkit.plugin.agent.PluginHolder")
                .defineField("runnable", Runnable.class, Modifier.PUBLIC | Modifier.STATIC)
                .make()
                .load(Bukkit.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        Field field = holderClass.getField("runnable");
        Instrumentation inst = ReflectionUtils.JNI_IS_AVAILABLE ? ImplLookupGetter.INSTRUMENTATION : ByteBuddyAgent.install();
        field.set(null, (Runnable) () -> {
            try {
                plugin.injectRegistries();
                inst.removeTransformer(BlocksAgent.transformer);
            } catch (Throwable t) {
                plugin.logger().warn("Failed to inject registries", t);
            }
        });
        BlocksAgent.agentmain(null, inst);
    }
}
