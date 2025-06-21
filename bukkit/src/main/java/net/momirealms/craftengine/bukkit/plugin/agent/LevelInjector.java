package net.momirealms.craftengine.bukkit.plugin.agent;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldEvents;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@SuppressWarnings("all")
public class LevelInjector {

    public static void patch() {
        Class<?> holderClass = new ByteBuddy()
                .subclass(Object.class)
                .name("net.momirealms.craftengine.bukkit.plugin.agent.LevelInjectorHolder")
                .defineField("levelInjector", Object.class, Modifier.PUBLIC | Modifier.STATIC)
                .defineMethod("setLevelInjector", void.class, Modifier.PUBLIC | Modifier.STATIC)
                .withParameters(Object.class)
                .intercept(new Implementation() {
                    @Override
                    public @NotNull InstrumentedType prepare(@NotNull InstrumentedType instrumentedType) {
                        return instrumentedType;
                    }

                    @Override
                    public @NotNull ByteCodeAppender appender(@NotNull Target implementationTarget) {
                        return (methodVisitor, implementationContext, instrumentedMethod) -> {
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                            methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC,
                                    "net/momirealms/craftengine/bukkit/plugin/agent/LevelInjectorHolder",
                                    "levelInjector",
                                    "Ljava/lang/Object;");
                            methodVisitor.visitInsn(Opcodes.RETURN);
                            return new ByteCodeAppender.Size(1, 1);
                        };
                    }
                })
                .defineField("destroyBlockEventMethod", MethodHandle.class, Modifier.PUBLIC | Modifier.STATIC)
                .defineMethod("setDestroyBlockEventMethod", void.class, Modifier.PUBLIC | Modifier.STATIC)
                .withParameters(MethodHandle.class)
                .intercept(new Implementation() {
                    @Override
                    public @NotNull InstrumentedType prepare(@NotNull InstrumentedType instrumentedType) {
                        return instrumentedType;
                    }

                    @Override
                    public @NotNull ByteCodeAppender appender(@NotNull Target implementationTarget) {
                        return (methodVisitor, implementationContext, instrumentedMethod) -> {
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                            methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC,
                                    "net/momirealms/craftengine/bukkit/plugin/agent/LevelInjectorHolder",
                                    "destroyBlockEventMethod",
                                    "Ljava/lang/invoke/MethodHandle;");
                            methodVisitor.visitInsn(Opcodes.RETURN);
                            return new ByteCodeAppender.Size(1, 1);
                        };
                    }
                })
                .make()
                .load(Bukkit.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        try {
            Method setLevelInjector = holderClass.getMethod("setLevelInjector", Object.class);
            setLevelInjector.invoke(null, new LevelInjector());
            Method setDestroyBlockEventMethod = holderClass.getMethod("setDestroyBlockEventMethod", MethodHandle.class);
            Method destroyBlockEvent = LevelInjector.class.getMethod("destroyBlockEvent", Object.class, Object[].class);
            MethodHandle destroyBlockEventMethod = MethodHandles.lookup().unreflect(destroyBlockEvent)
                    .asType(MethodType.methodType(void.class, Object.class, Object.class, Object[].class));
            setDestroyBlockEventMethod.invoke(null, destroyBlockEventMethod);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        agentmain();
    }

    public static void agentmain() {
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .type(ElementMatchers.named("net.minecraft.world.level.Level").or(ElementMatchers.named("net.minecraft.world.level.World")))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(Advice.to(DestroyBlockAdvice.class)
                                .on(ElementMatchers.is(CoreReflections.method$Level$destroyBlock))))
                .installOn(ByteBuddyAgent.install());
    }

    public static class DestroyBlockAdvice {

        @Advice.OnMethodEnter
        public static void onEnter(@Advice.This Object level, @Advice.AllArguments Object[] args) {
            try {
                Class<?> holder = Class.forName("net.momirealms.craftengine.bukkit.plugin.agent.LevelInjectorHolder");
                Object instance = holder.getField("levelInjector").get(null);
                MethodHandle destroyBlockEventMethod = (MethodHandle) holder.getField("destroyBlockEventMethod").get(null);
                destroyBlockEventMethod.invokeExact(instance, level, args);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public void destroyBlockEvent(Object level, Object[] args) {
        Object blockPos = args[0];
        boolean dropBlock = (boolean) args[1];
        Object entity = args[2];
        if (!dropBlock || entity != null) return;
        Object state = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, blockPos);
        int stateId = BlockStateUtils.blockStateToId(state);
        ImmutableBlockState blockState = BukkitBlockManager.instance().getImmutableBlockState(stateId);
        if (blockState == null || blockState.isEmpty()) return;
        BlockPos pos = LocationUtils.fromBlockPos(blockPos);
        net.momirealms.craftengine.core.world.World world = new BukkitWorld(FastNMS.INSTANCE.method$Level$getCraftWorld(level));
        WorldPosition position = new WorldPosition(world, Vec3d.atCenterOf(pos));
        ContextHolder.Builder builder = ContextHolder.builder()
                .withParameter(DirectContextParameters.POSITION, position);
        for (Item<Object> item : blockState.getDrops(builder, world, null)) {
            world.dropItemNaturally(position, item);
        }
        world.playBlockSound(position, blockState.sounds().breakSound());
        FastNMS.INSTANCE.method$Level$levelEvent(level, WorldEvents.BLOCK_BREAK_EFFECT, blockPos, stateId);
    }

}
