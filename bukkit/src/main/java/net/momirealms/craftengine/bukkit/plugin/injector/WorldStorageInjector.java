package net.momirealms.craftengine.bukkit.plugin.injector;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.EmptyBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.core.util.SectionPosUtils;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.SectionPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.CESection;
import net.momirealms.craftengine.core.world.chunk.InjectedHolder;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class WorldStorageInjector {
    private static Class<?> clazz$InjectedPalettedContainer;
    private static MethodHandle constructor$InjectedLevelChunkSection;

    public static void init() throws ReflectiveOperationException {
        ByteBuddy byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V17);
        // Paletted Container
        clazz$InjectedPalettedContainer = byteBuddy
                .subclass(CoreReflections.clazz$PalettedContainer)
                .name("net.minecraft.world.level.chunk.InjectedPalettedContainer")
                .implement(InjectedHolder.Palette.class)
                .defineField("target", CoreReflections.clazz$PalettedContainer, Visibility.PUBLIC)
                .defineField("active", boolean.class, Visibility.PUBLIC)
                .defineField("cesection", CESection.class, Visibility.PRIVATE)
                .defineField("cechunk", CEChunk.class, Visibility.PRIVATE)
                .defineField("cepos", SectionPos.class, Visibility.PRIVATE)
                .method(ElementMatchers.any()
                        .and(ElementMatchers.not(ElementMatchers.is(CoreReflections.method$PalettedContainer$getAndSet)))
                        .and(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class)))
                )
                .intercept(MethodDelegation.toField("target"))
                .method(ElementMatchers.is(CoreReflections.method$PalettedContainer$getAndSet))
                .intercept(MethodDelegation.to(GetAndSetInterceptor.INSTANCE))
                .method(ElementMatchers.named("target"))
                .intercept(FieldAccessor.ofField("target"))
                .method(ElementMatchers.named("setTarget"))
                .intercept(FieldAccessor.ofField("target").withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                .method(ElementMatchers.named("isActive").or(ElementMatchers.named("setActive")))
                .intercept(FieldAccessor.ofField("active"))
                .method(ElementMatchers.named("ceSection"))
                .intercept(FieldAccessor.ofField("cesection"))
                .method(ElementMatchers.named("ceChunk"))
                .intercept(FieldAccessor.ofField("cechunk"))
                .method(ElementMatchers.named("cePos"))
                .intercept(FieldAccessor.ofField("cepos"))
                .make()
                .load(WorldStorageInjector.class.getClassLoader())
                .getLoaded();
        // Level Chunk Section
        Class<?> clazz$InjectedLevelChunkSection = byteBuddy
                .subclass(CoreReflections.clazz$LevelChunkSection, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                .name("net.minecraft.world.level.chunk.InjectedLevelChunkSection")
                .implement(InjectedHolder.Section.class)
                .defineField("active", boolean.class, Visibility.PUBLIC)
                .defineField("cesection", CESection.class, Visibility.PRIVATE)
                .defineField("cechunk", CEChunk.class, Visibility.PRIVATE)
                .defineField("cepos", SectionPos.class, Visibility.PRIVATE)
                .method(ElementMatchers.is(CoreReflections.method$LevelChunkSection$setBlockState))
                .intercept(MethodDelegation.to(SetBlockStateInterceptor.INSTANCE))
                .method(ElementMatchers.named("ceSection"))
                .intercept(FieldAccessor.ofField("cesection"))
                .method(ElementMatchers.named("ceChunk"))
                .intercept(FieldAccessor.ofField("cechunk"))
                .method(ElementMatchers.named("cePos"))
                .intercept(FieldAccessor.ofField("cepos"))
                .method(ElementMatchers.named("isActive").or(ElementMatchers.named("setActive")))
                .intercept(FieldAccessor.ofField("active"))
                .make()
                .load(WorldStorageInjector.class.getClassLoader())
                .getLoaded();

        constructor$InjectedLevelChunkSection = MethodHandles.publicLookup().in(clazz$InjectedLevelChunkSection)
                .findConstructor(clazz$InjectedLevelChunkSection, MethodType.methodType(void.class, CoreReflections.clazz$PalettedContainer, CoreReflections.clazz$PalettedContainer))
                .asType(MethodType.methodType(CoreReflections.clazz$LevelChunkSection, CoreReflections.clazz$PalettedContainer, CoreReflections.clazz$PalettedContainer));
    }

    public synchronized static void injectLevelChunkSection(Object targetSection, CESection ceSection, CEChunk chunk, SectionPos pos, Consumer<Object> callback) {
        try {
            if (Config.injectionTarget()) {
                Object container = FastNMS.INSTANCE.field$LevelChunkSection$states(targetSection);
                if (!(container instanceof InjectedHolder.Palette holder)) {
                    InjectedHolder.Palette injectedObject;
                    if (Config.fastInjection()) {
                        injectedObject = FastNMS.INSTANCE.createInjectedPalettedContainerHolder(container);
                    } else {
                        injectedObject = (InjectedHolder.Palette) ReflectionUtils.UNSAFE.allocateInstance(clazz$InjectedPalettedContainer);
                        injectedObject.setTarget(container);
                        //varHandle$InjectedPalettedContainer$target.set(injectedObject, container);
                    }
                    injectedObject.ceChunk(chunk);
                    injectedObject.ceSection(ceSection);
                    injectedObject.cePos(pos);
                    injectedObject.setActive(true);
                    CoreReflections.varHandle$PalettedContainer$data.setVolatile(injectedObject, CoreReflections.varHandle$PalettedContainer$data.get(container));
                    CoreReflections.field$LevelChunkSection$states.set(targetSection, injectedObject);
                } else {
                    holder.ceChunk(chunk);
                    holder.ceSection(ceSection);
                    holder.cePos(pos);
                    holder.setActive(true);
                }
            } else {
                if (!(targetSection instanceof InjectedHolder.Section holder)) {
                    InjectedHolder.Section injectedObject;
                    if (Config.fastInjection()) {
                        injectedObject = FastNMS.INSTANCE.createInjectedLevelChunkSectionHolder(targetSection);
                    } else {
                        injectedObject = (InjectedHolder.Section) constructor$InjectedLevelChunkSection.invoke(
                                FastNMS.INSTANCE.field$LevelChunkSection$states(targetSection), FastNMS.INSTANCE.field$LevelChunkSection$biomes(targetSection));
                    }
                    injectedObject.ceChunk(chunk);
                    injectedObject.ceSection(ceSection);
                    injectedObject.cePos(pos);
                    injectedObject.setActive(true);
                    callback.accept(injectedObject);
                } else {
                    holder.ceChunk(chunk);
                    holder.ceSection(ceSection);
                    holder.cePos(pos);
                    holder.setActive(true);
                }
            }
        } catch (Throwable e) {
            CraftEngine.instance().logger().severe("Failed to inject chunk section " + pos, e);
        }
    }

    public static boolean isSectionInjected(Object section) {
        if (Config.injectionTarget()) {
            Object container = FastNMS.INSTANCE.field$LevelChunkSection$states(section);
            return container instanceof InjectedHolder.Palette;
        } else {
            return section instanceof InjectedHolder.Section;
        }
    }

    public synchronized static Object uninjectLevelChunkSection(Object section) {
        if (Config.injectionTarget()) {
            Object states = FastNMS.INSTANCE.field$LevelChunkSection$states(section);
            if (states instanceof InjectedHolder.Palette holder) {
                holder.setActive(false);
            }
        } else {
            if (section instanceof InjectedHolder.Section holder) {
                holder.setActive(false);
            }
        }
        return section;
    }


    public static class SetBlockStateInterceptor {
        public static final SetBlockStateInterceptor INSTANCE = new SetBlockStateInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) throws Exception {
            InjectedHolder.Section holder = (InjectedHolder.Section) thisObj;
            int x = (int) args[0];
            int y = (int) args[1];
            int z = (int) args[2];
            Object newState = args[3];
            Object previousState = superMethod.call();
            if (holder.isActive()) {
                compareAndUpdateBlockState(x, y, z, newState, previousState, holder);
            }
            return previousState;
        }
    }

    public static class GetAndSetInterceptor {
        public static final GetAndSetInterceptor INSTANCE = new GetAndSetInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args) {
            InjectedHolder.Palette holder = (InjectedHolder.Palette) thisObj;
            Object targetStates = holder.target();
            int x = (int) args[0];
            int y = (int) args[1];
            int z = (int) args[2];
            Object newState = args[3];
            Object previousState = FastNMS.INSTANCE.method$PalettedContainer$getAndSet(targetStates, x, y, z, newState);
            if (holder.isActive()) {
                compareAndUpdateBlockState(x, y, z, newState, previousState, holder);
            }
            return previousState;
        }
    }

    protected static void compareAndUpdateBlockState(int x, int y, int z, Object newState, Object previousState, InjectedHolder holder) {
        try {
            Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(newState);
            CESection section = holder.ceSection();
            // 如果是原版方块
            if (optionalCustomState.isEmpty()) {
                // 那么应该清空自定义块
                ImmutableBlockState previous = section.setBlockState(x, y, z, EmptyBlock.STATE);
                // 处理  自定义块 -> 原版块
                if (!previous.isEmpty()) {
                    holder.ceChunk().setDirty(true);
                    if (Config.enableLightSystem()) {
                        // 自定义块到原版块，只需要判断旧块是否和客户端一直
                        BlockStateWrapper wrapper = previous.vanillaBlockState();
                        if (wrapper != null) {
                            updateLight(holder, wrapper.handle(), previousState, x, y, z);
                        }
                    }
                }
            } else {
                ImmutableBlockState immutableBlockState = optionalCustomState.get();
                ImmutableBlockState previousImmutableBlockState = section.setBlockState(x, y, z, immutableBlockState);
                if (previousImmutableBlockState == immutableBlockState) return;
                // 处理  自定义块到自定义块或原版块到自定义块
                holder.ceChunk().setDirty(true);
                // 不可能！绝对不可能！
                if (immutableBlockState.isEmpty()) return;
                // 如果新方块的光照属性和客户端认为的不同
                if (Config.enableLightSystem()) {
                    if (previousImmutableBlockState.isEmpty()) {
                        // 原版块到自定义块，只需要判断新块是否和客户端视觉一致
                        updateLight(holder, immutableBlockState.vanillaBlockState().handle(), newState, x, y, z);
                    } else {
                        // 自定义块到自定义块
                        updateLight$complex(holder, immutableBlockState.vanillaBlockState().handle(), newState, previousState, x, y, z);
                    }
                }
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to intercept setBlockState", e);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    protected static void updateLight(@This InjectedHolder thisObj, Object clientState, Object serverState, int x, int y, int z) {
        CEWorld world = thisObj.ceChunk().world();
        Object blockPos = LocationUtils.toBlockPos(x, y, z);
        Object serverWorld = world.world().serverWorld();
        if (FastNMS.INSTANCE.method$LightEngine$hasDifferentLightProperties(serverState, clientState, serverWorld, blockPos)) {
            SectionPos sectionPos = thisObj.cePos();
            List<SectionPos> pos = SectionPosUtils.calculateAffectedRegions((sectionPos.x() << 4) + x, (sectionPos.y() << 4) + y, (sectionPos.z() << 4) + z, 15);
            world.sectionLightUpdated(pos);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    protected static void updateLight$complex(@This InjectedHolder thisObj, Object newClientState, Object newServerState, Object oldServerState, int x, int y, int z) {
        CEWorld world = thisObj.ceChunk().world();
        Object blockPos = LocationUtils.toBlockPos(x, y, z);
        Object serverWorld = world.world().serverWorld();
        // 如果客户端新状态和服务端新状态光照属性不同
        if (FastNMS.INSTANCE.method$LightEngine$hasDifferentLightProperties(newClientState, newServerState, serverWorld, blockPos)) {
            SectionPos sectionPos = thisObj.cePos();
            List<SectionPos> pos = SectionPosUtils.calculateAffectedRegions((sectionPos.x() << 4) + x, (sectionPos.y() << 4) + y, (sectionPos.z() << 4) + z, 15);
            world.sectionLightUpdated(pos);
            return;
        }
        if (FastNMS.INSTANCE.method$LightEngine$hasDifferentLightProperties(newServerState, oldServerState, serverWorld, blockPos)) {
            SectionPos sectionPos = thisObj.cePos();
            List<SectionPos> pos = SectionPosUtils.calculateAffectedRegions((sectionPos.x() << 4) + x, (sectionPos.y() << 4) + y, (sectionPos.z() << 4) + z, 15);
            world.sectionLightUpdated(pos);
        }
    }
}
