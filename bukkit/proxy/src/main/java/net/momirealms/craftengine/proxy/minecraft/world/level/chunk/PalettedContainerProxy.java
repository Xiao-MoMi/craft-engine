package net.momirealms.craftengine.proxy.minecraft.world.level.chunk;

import net.momirealms.craftengine.proxy.paper.antixray.ChunkPacketInfoProxy;
import net.momirealms.craftengine.proxy.universeprojects.util.io.DirectChunkWriterProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.world.level.chunk.PalettedContainer")
public interface PalettedContainerProxy {
    PalettedContainerProxy INSTANCE = ASMProxyFactory.create(PalettedContainerProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.chunk.PalettedContainer");

    @FieldSetter(name = "data")
    void setData(Object target, Object data);

    @FieldGetter(name = "data")
    Object getData(Object target);

    @FieldGetter(name = "strategy")
    Object getStrategy(Object target);

    @FieldSetter(name = "strategy")
    void setStrategy(Object target, Object strategy);

    @FieldGetter(name = "presetValues", activeIf = "min_version=1.21.9", optional = true)
    default Object[] getPresetValues(Object target) {
        return null;
    }

    @FieldGetter(name = "registry", activeIf = "max_version=1.21.8")
    Object getRegistry(Object target);

    @FieldSetter(name = "registry", activeIf = "max_version=1.21.8")
    void setRegistry(Object target, Object registry);

    @MethodInvoker(name = "get")
    Object getByIndex(Object target, int index);

    @MethodInvoker(name = "getAndSetUnchecked")
    Object getAndSetUnchecked(Object target, int x, int y, int z, Object state);

    @MethodInvoker(name = "getAndSet")
    Object getAndSet(Object target, int x, int y, int z, Object state);

    // universe spigot 26.2+
    @MethodInvoker(name = "write", optional = true)
    void write(Object target,
               @Type(clazz = DirectChunkWriterProxy.class) Object writer,
               @Type(clazz = ChunkPacketInfoProxy.class) Object chunkPacketInfo,
               int chunkSectionIndex);

    @MethodInvoker(name = "leaf$getDataAcquire", optional = true)
    default Object leaf$getDataAcquire(Object target) {
        return null;
    }

    @MethodInvoker(name = "leaf$getFromData", optional = true)
    default Object leaf$getFromData(Object target, @Type(clazz = PalettedContainerProxy.DataProxy.class) Object data, int index) {
        return null;
    }

    @ReflectionProxy(name = "net.minecraft.world.level.chunk.PalettedContainer$Data")
    interface DataProxy {
        DataProxy INSTANCE = ASMProxyFactory.create(DataProxy.class);

        @FieldGetter(name = "palette")
        Object getPalette(Object target);
    }

    @ReflectionProxy(name = "net.minecraft.world.level.chunk.Strategy", activeIf = "min_version=1.21.9")
    interface StrategyProxy {
        Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.chunk.Strategy");
    }
}
