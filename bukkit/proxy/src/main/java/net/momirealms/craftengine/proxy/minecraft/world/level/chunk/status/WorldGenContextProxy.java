package net.momirealms.craftengine.proxy.minecraft.world.level.chunk.status;

import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ThreadedLevelLightEngineProxy;
import net.momirealms.craftengine.proxy.minecraft.util.thread.ProcessorHandleProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.ChunkGeneratorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.LevelChunkProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManagerProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.concurrent.Executor;

@ReflectionProxy(name = "net.minecraft.world.level.chunk.status.WorldGenContext", activeIf = "min_version=1.20.5")
public interface WorldGenContextProxy {
    WorldGenContextProxy INSTANCE = ASMProxyFactory.create(WorldGenContextProxy.class);

    @ConstructorInvoker(activeIf = "max_version=1.20.6")
    Object newInstance(@Type(clazz = ServerLevelProxy.class) Object level,
                       @Type(clazz = ChunkGeneratorProxy.class) Object generator,
                       @Type(clazz = StructureTemplateManagerProxy.class) Object structureManager,
                       @Type(clazz = ThreadedLevelLightEngineProxy.class) Object lightEngine);

    @ConstructorInvoker(activeIf = "min_version=1.21 && max_version=1.21.1")
    Object newInstance(@Type(clazz = ServerLevelProxy.class) Object level,
                       @Type(clazz = ChunkGeneratorProxy.class) Object generator,
                       @Type(clazz = StructureTemplateManagerProxy.class) Object structureManager,
                       @Type(clazz = ThreadedLevelLightEngineProxy.class) Object lightEngine,
                       @Type(clazz = ProcessorHandleProxy.class) Object mainThreadMailBox);

    @ConstructorInvoker(activeIf = "min_version=1.21.2")
    Object newInstance(@Type(clazz = ServerLevelProxy.class) Object level,
                       @Type(clazz = ChunkGeneratorProxy.class) Object generator,
                       @Type(clazz = StructureTemplateManagerProxy.class) Object structureManager,
                       @Type(clazz = ThreadedLevelLightEngineProxy.class) Object lightEngine,
                       Executor mainThreadExecutor,
                       @Type(clazz = LevelChunkProxy.UnsavedListenerProxy.class) Object unsavedListener);

    @FieldGetter(name = "level")
    Object getLevel(Object target);

    @FieldGetter(name = "generator")
    Object getGenerator(Object target);

    @FieldGetter(name = "structureManager")
    Object getStructureManager(Object target);

    @FieldGetter(name = "lightEngine")
    Object getLightEngine(Object target);

    @FieldGetter(name = "mainThreadMailBox", activeIf = "min_version=1.21 && max_version=1.21.1")
    Object getMainThreadMailBox(Object target);

    @FieldGetter(name = "mainThreadExecutor", activeIf = "min_version=1.21.2")
    Executor getMainThreadExecutor(Object target);

    @FieldGetter(name = "unsavedListener", activeIf = "min_version=1.21.2")
    Object getUnsavedListener(Object target);
}
