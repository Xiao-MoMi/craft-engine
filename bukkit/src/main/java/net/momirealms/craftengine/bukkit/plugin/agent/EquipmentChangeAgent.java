package net.momirealms.craftengine.bukkit.plugin.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.jetbrains.annotations.NotNull;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public final class EquipmentChangeAgent {
    private static final String MOJMAP_LIVING_ENTITY = "net.minecraft.world.entity.LivingEntity";
    private static final String SPIGOT_LIVING_ENTITY = "net.minecraft.world.entity.EntityLiving";

    private EquipmentChangeAgent() {
    }

    public static boolean install(Instrumentation instrumentation) {
        AtomicBoolean transformed = new AtomicBoolean();
        AtomicBoolean ambiguous = new AtomicBoolean();
        AtomicBoolean failed = new AtomicBoolean();
        ClassFileTransformer transformer = new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(new AgentBuilder.Listener.Adapter() {
                    @Override
                    public void onError(@NotNull String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, @NotNull Throwable throwable) {
                        failed.set(true);
                    }
                })
                .type(ElementMatchers.named(MOJMAP_LIVING_ENTITY).or(ElementMatchers.named(SPIGOT_LIVING_ENTITY)))
                .transform((builder, type, classLoader, module, protectionDomain) -> {
                    ElementMatcher.Junction<MethodDescription> matcher = equipmentChangeMethod(type.getName());
                    int candidates = type.getDeclaredMethods().filter(matcher).size();
                    if (candidates != 1) {
                        ambiguous.set(true);
                        return builder;
                    }
                    transformed.set(true);
                    return builder.visit(Advice.to(EquipmentChangeAdvice.class).on(matcher));
                })
                .installOn(instrumentation);
        instrumentation.removeTransformer(transformer);
        return transformed.get() && !ambiguous.get() && !failed.get();
    }

    private static ElementMatcher.Junction<MethodDescription> equipmentChangeMethod(String owner) {
        ElementMatcher.Junction<MethodDescription> signature = ElementMatchers.returns(Map.class)
                .and(ElementMatchers.takesNoArguments().or(ElementMatchers.takesArguments(Map.class)));
        if (MOJMAP_LIVING_ENTITY.equals(owner)) {
            return ElementMatchers.named("collectEquipmentChanges").and(signature);
        }
        return ElementMatchers.isPrivate()
                .or(ElementMatchers.isProtected())
                .and(signature);
    }

    public static final class EquipmentChangeAdvice {

        private EquipmentChangeAdvice() {
        }

        @Advice.OnMethodExit
        public static void onExit(@Advice.This Object entity, @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object changes) {
            try {
                if (!(changes instanceof Map<?, ?> map) || map.isEmpty()) return;
                BiConsumer<Object, Object> callback = AgentBridge.EQUIPMENT_CHANGE;
                if (callback != null) {
                    callback.accept(entity, map);
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
