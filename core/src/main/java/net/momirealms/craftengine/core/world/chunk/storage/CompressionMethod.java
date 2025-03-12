package net.momirealms.craftengine.core.world.chunk.storage;

import it.unimi.dsi.fastutil.io.FastBufferedInputStream;

import javax.annotation.Nullable;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

public class CompressionMethod {
    private static final int METHOD_COUNT = 3;
    public static final CompressionMethod[] METHODS = new CompressionMethod[METHOD_COUNT +1];
    public static final CompressionMethod NONE = register(new CompressionMethod(1, (stream) -> stream, (stream) -> stream));
    public static final CompressionMethod DEFLATE = register(new CompressionMethod(2, (stream) -> new FastBufferedInputStream(new InflaterInputStream(stream)), (stream) -> new BufferedOutputStream(new DeflaterOutputStream(stream))));
    public static final CompressionMethod GZIP = register(new CompressionMethod(3, (stream) -> new FastBufferedInputStream(new GZIPInputStream(stream)), (stream) -> new BufferedOutputStream(new GZIPOutputStream(stream))));
//    public static final CompressionMethod LZ4 = register(new CompressionMethod(4, LZ4BlockInputStream::new, LZ4BlockOutputStream::new));

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
