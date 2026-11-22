package net.momirealms.craftengine.bukkit.plugin.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BlockIdentityAgent {

    private BlockIdentityAgent() {
    }

    static void install(Instrumentation instrumentation, Method method) {
        Class<?> owner = method.getDeclaringClass();
        AtomicBoolean transformed = new AtomicBoolean();
        new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(new AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Explicit(owner))
                .with(AgentBuilder.RedefinitionStrategy.Listener.ErrorEscalating.FAIL_FAST)
                .with(new AgentBuilder.Listener.Adapter() {
                    @Override
                    public void onTransformation(TypeDescription type, ClassLoader loader, JavaModule module, boolean loaded, DynamicType dynamicType) {
                        transformed.set(true);
                    }
                })
                .type(ElementMatchers.is(owner))
                .transform((builder, type, loader, module, domain) -> builder.visit(Advice.to(BlockIdentityAgent.class).on(ElementMatchers.is(method))))
                .installOn(instrumentation);
        if (!transformed.get()) {
            throw new IllegalStateException("Could not transform " + method);
        }
    }

    @Advice.OnMethodExit
    public static void onExit(@Advice.This Object state, @Advice.Argument(0) Object block, @Advice.Return(readOnly = false) boolean matches) {
        if (state.getClass() == AgentBridge.CUSTOM_BLOCK_STATE) {
            matches = AgentBridge.CUSTOM_BLOCK_MATCH.test(state, block);
        }
    }
}
