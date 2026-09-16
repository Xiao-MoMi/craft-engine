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

    @MethodInvoker(name = "leaf$getDataAcquire", activeIf = "has_patch=leaf && min_version=26.2", optional = true)
    default Object leaf$getDataAcquire(Object target) {
        return null;
    }

    @MethodInvoker(name = "leaf$getFromData", activeIf = "has_patch=leaf && min_version=26.2", optional = true)
    default Object leaf$getFromData(Object target, @Type(clazz = PalettedContainerProxy.DataProxy.class) Object data, int index) {
        return null;
    }

    @ReflectionProxy(name = "net.minecraft.world.level.chunk.PalettedContainer$Data")
    interface DataProxy {
        DataProxy INSTANCE = ASMProxyFactory.create(DataProxy.class);

        @FieldGetter(name = "palette")
        Object getPalette(Object target);
    }
}
