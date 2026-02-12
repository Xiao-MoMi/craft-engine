package net.momirealms.craftengine.core.world.chunk.storage;

import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.dependency.Dependencies;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Set;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

public final class CompressionMethod {
    public static final int METHOD_COUNT = 5;
    public static final CompressionMethod[] METHODS = new CompressionMethod[METHOD_COUNT +1];
    public static final CompressionMethod NONE = register(new CompressionMethod(1, (stream) -> stream, (stream) -> stream));
    public static final CompressionMethod DEFLATE = register(new CompressionMethod(2, (stream) -> new FastBufferedInputStream(new InflaterInputStream(stream)), (stream) -> new FastBufferedOutputStream(new DeflaterOutputStream(stream))));
    public static final CompressionMethod GZIP = register(new CompressionMethod(3, (stream) -> new FastBufferedInputStream(new GZIPInputStream(stream)), (stream) -> new FastBufferedOutputStream(new GZIPOutputStream(stream))));
    public static final CompressionMethod LZ4 = register(new CompressionMethod(4, is -> LZ4BlockInputStream.newBuilder().build(is), LZ4BlockOutputStream::new));
    public static final CompressionMethod ZSTD;

    static {
        ClassLoader classLoader = CraftEngine.instance().dependencyManager().obtainClassLoaderWith(Set.of(Dependencies.ZSTD));

        try {
            Class<?> inputStreamClass = classLoader.loadClass("com.github.luben.zstd.ZstdInputStream");
            MethodHandle inputStreamConstructor = ReflectionUtils.unreflectConstructor(inputStreamClass.getConstructor(InputStream.class))
                    .asType(MethodType.methodType(InputStream.class, InputStream.class));
            Class<?> outputStreamClass = classLoader.loadClass("com.github.luben.zstd.ZstdOutputStream");
            MethodHandle outputStreamConstructor = ReflectionUtils.unreflectConstructor(outputStreamClass.getConstructor(OutputStream.class))
                    .asType(MethodType.methodType(OutputStream.class, OutputStream.class));
            ZSTD = register(
                    new CompressionMethod(
                            5,
                            rawStream -> {
                                try {
                                    return (InputStream) inputStreamConstructor.invokeExact(rawStream);
                                } catch (Throwable e) {
                                    CraftEngine.instance().logger().warn("Could not instantiate ZstdInputStream", e);
                                    return rawStream;
                                }
                            },
                            rawStream -> {
                                try {
                                    return (OutputStream) outputStreamConstructor.invokeExact(rawStream);
                                } catch (Throwable e) {
                                    CraftEngine.instance().logger().warn("Could not instantiate ZstdOutputStream", e);
                                    return rawStream;
                                }
                            }
                    )
            );
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private final int id;
    private final StreamWrapper<InputStream> inputWrapper;
    private final StreamWrapper<OutputStream> outputWrapper;

    private CompressionMethod(int id, StreamWrapper<InputStream> inputStreamWrapper, StreamWrapper<OutputStream> outputStreamWrapper) {
        this.id = id;
        this.inputWrapper = inputStreamWrapper;
        this.outputWrapper = outputStreamWrapper;
    }

    private static CompressionMethod register(CompressionMethod version) {
        METHODS[version.id] = version;
        return version;
    }

    @Nullable
    public static CompressionMethod fromId(int id) {
        if (!isValid(id)) return null;
        return METHODS[id];
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isValid(int id) {
        return id > 0 && id <= METHOD_COUNT;
    }

    public int getId() {
        return this.id;
    }

    public OutputStream wrap(OutputStream outputStream) throws IOException {
        return this.outputWrapper.wrap(outputStream);
    }

    public InputStream wrap(InputStream inputStream) throws IOException {
        return this.inputWrapper.wrap(inputStream);
    }

    @FunctionalInterface
    interface StreamWrapper<O> {
        O wrap(O object) throws IOException;
    }
}
