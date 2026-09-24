package net.momirealms.craftengine.bukkit.plugin.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

public final class BlocksAgent {
    public static ClassFileTransformer transformer;

    private BlocksAgent() {}

    public static void install(Instrumentation instrumentation) {
        transformer = new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .type(ElementMatchers.named("net.minecraft.server.Bootstrap")
                        .or(ElementMatchers.named("net.minecraft.server.DispenserRegistry")))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(Advice.to(BlocksAdvice.class)
                                .on(ElementMatchers.named("validate").or(ElementMatchers.named("c")))))
                .installOn(instrumentation);
    }

    public static class BlocksAdvice {

        @Advice.OnMethodExit
        public static void onExit() {
            try {
                Runnable runnable = AgentBridge.REGISTRY_INJECTION;
                if (runnable != null) {
                    runnable.run();
                }
            } catch (Throwable t) {
                t.printStackTrace(System.err);
            }
        }
    }
}
