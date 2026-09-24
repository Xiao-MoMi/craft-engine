package net.momirealms.craftengine.bukkit.plugin.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static net.bytebuddy.matcher.ElementMatchers.*;

public final class ChunkLifecycleAgent {
    private static final String LOAD = "ca.spottedleaf.moonrise.patches.chunk_system.scheduling.task.ChunkLoadTask";
    private static final String DATA = LOAD + "$ChunkDataLoadTask";
    private static final String HOLDER = "ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder";
    private static volatile boolean installed;

    private ChunkLifecycleAgent() {
    }

    public static boolean installed() {
        return installed;
    }

    public static void install(Instrumentation instrumentation, ClassLoader loader) throws ClassNotFoundException {
        // Load all targets before retransformation so installation is verified synchronously.
        for (String name : Set.of(LOAD, DATA, HOLDER)) Class.forName(name, false, loader);
        Set<String> transformed = new HashSet<>();
        var transformer = new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(new AgentBuilder.Listener.Adapter() {
                    @Override
                    public void onTransformation(TypeDescription type, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
                        transformed.add(type.getName());
                    }
                })
                .type(namedOneOf(LOAD, DATA, HOLDER))
                .transform((builder, type, classLoader, module, domain) -> transform(builder, type))
                .installOn(instrumentation);
        instrumentation.removeTransformer(transformer);
        if (!transformed.containsAll(Set.of(LOAD, DATA, HOLDER))) {
            throw new IllegalStateException("Could not install all Moonrise lifecycle hooks; transformed " + transformed);
        }
        installed = true;
    }

    static DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription type) {
        return switch (type.getName()) {
            case LOAD -> {
                require(type, isConstructor().and(takesArguments(6)));
                require(type, named("tryCompleteLoad").and(takesArguments(0)));
                yield builder.visit(Advice.to(Start.class).on(isConstructor().and(takesArguments(6))))
                        .visit(Advice.to(Complete.class).on(named("tryCompleteLoad").and(takesArguments(0))));
            }
            case DATA -> {
                require(type, named("runOffMain").and(takesArguments(2)).and(not(isBridge())));
                require(type, named("getEmptyChunk").and(takesArguments(0)));
                yield builder.visit(Advice.to(Read.class).on(named("runOffMain").and(takesArguments(2)).and(not(isBridge()))))
                        .visit(Advice.to(Empty.class).on(named("getEmptyChunk").and(takesArguments(0))));
            }
            case HOLDER -> {
                require(type, named("onUnload").and(takesArguments(0)));
                yield builder.visit(Advice.to(Release.class).on(named("onUnload").and(takesArguments(0))));
            }
            default -> throw new IllegalArgumentException(type.getName());
        };
    }

    private static void require(TypeDescription type, net.bytebuddy.matcher.ElementMatcher<? super net.bytebuddy.description.method.MethodDescription.InDefinedShape> matcher) {
        if (type.getDeclaredMethods().filter(matcher).size() != 1) {
            throw new IllegalStateException("Unsupported Moonrise lifecycle method in " + type.getName());
        }
    }

    public static class Start {
        @Advice.OnMethodExit
        public static void exit(@Advice.FieldValue("world") Object world,
                                @Advice.FieldValue("chunkX") int x, @Advice.FieldValue("chunkZ") int z,
                                @Advice.FieldValue("chunkHolder") Object holder, @Advice.FieldValue("loadTask") Object task) {
            AgentBridge.CHUNK_LIFECYCLE_START.accept(new Object[]{world, x, z, holder, task});
        }
    }

    public static class Complete {
        @Advice.OnMethodExit
        public static void exit(@Advice.FieldValue("taskCountToComplete") AtomicInteger remaining,
                                @Advice.FieldValue("loadTask") Object task) {
            if (remaining.get() == 0) AgentBridge.CHUNK_LIFECYCLE_COMPLETE.accept(task);
        }
    }

    public static class Read {
        @Advice.OnMethodEnter
        public static Object enter(@Advice.This Object task) {
            return AgentBridge.CHUNK_LIFECYCLE_CONTEXT.apply(task);
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.Enter Object context, @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object result) {
            AgentBridge.CHUNK_LIFECYCLE_READ.accept(context, result);
        }
    }

    public static class Empty {
        @Advice.OnMethodExit
        public static void exit(@Advice.This Object task, @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object chunk) {
            AgentBridge.CHUNK_LIFECYCLE_EMPTY.accept(task, chunk);
        }
    }

    public static class Release {
        @Advice.OnMethodExit
        public static void exit(@Advice.This Object holder, @Advice.FieldValue("world") Object world,
                                @Advice.FieldValue("chunkX") int x, @Advice.FieldValue("chunkZ") int z) {
            AgentBridge.CHUNK_LIFECYCLE_RELEASE.accept(new Object[]{world, x, z, holder});
        }
    }
}

