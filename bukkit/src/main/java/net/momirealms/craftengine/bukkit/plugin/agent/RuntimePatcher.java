package net.momirealms.craftengine.bukkit.plugin.agent;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.proxy.PluginHolder;

public final class RuntimePatcher {
    private RuntimePatcher() {}

    public static void patch(BukkitCraftEngine plugin) {
        PluginHolder.injectRegistries = plugin::injectRegistries;
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .type(ElementMatchers.named("net.minecraft.server.Bootstrap")
                        .or(ElementMatchers.named("net.minecraft.server.DispenserRegistry")))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(Advice.to(BlocksAdvice.class)
                                .on(ElementMatchers.named("validate")
                                        .or(ElementMatchers.named("c")))))
                .installOn(ByteBuddyAgent.install());
    }

    public static final class BlocksAdvice {

        @Advice.OnMethodExit
        public static void onExit() {
            try {
                PluginHolder.injectRegistries.run();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }
}
