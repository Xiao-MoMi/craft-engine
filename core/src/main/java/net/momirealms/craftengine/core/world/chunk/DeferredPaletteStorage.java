package net.momirealms.craftengine.core.world.chunk;

import io.netty.buffer.ByteBuf;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.function.IntConsumer;

/**
 * 打包数据的惰性存储。
 * <p>
 * 转发区块包时, 每个section的打包数组通常根本不需要解码: 重定向只改调色盘条目, 遮挡和光照的判断也是先看
 * 调色盘, 只有调色盘里同时存在两种结果时才需要逐方块遍历。而写回时如果打包数据没被改过, 原样拷贝字节即可。
 * <p>
 * 所以这里先只记录原始字节的位置, 直到真的有人按下标读写才解码成 {@link PackedIntegerArray}。
 * 没有被解码过就写回时, 直接把原来的字节拷过去, 连编码都省了。
 * <p>
 * 引用的是区块数据切片, 它在整个包处理期间有效: 各section是写进 staging 缓冲的, 而覆盖原缓冲的
 * {@code buf.clear()} 发生在那之后。
 */
public final class DeferredPaletteStorage implements PaletteStorage {
    private final ByteBuf source;
    private final int offset;
    private final int length;
    private final int elementBits;
    private final int size;
    // 非volatile: 每个实例都属于单个数据包的处理过程, 由某一条netty线程独占, 不跨线程共享
    private PackedIntegerArray materialized;

    public DeferredPaletteStorage(ByteBuf source, int offset, int length, int elementBits, int size) {
        this.source = source;
        this.offset = offset;
        this.length = length;
        this.elementBits = elementBits;
        this.size = size;
    }

    public boolean isMaterialized() {
        return this.materialized != null;
    }

    /** 把尚未解码的原始字节原样写出去 */
    public void writeRawTo(FriendlyByteBuf out) {
        if (this.materialized != null) {
            throw new IllegalStateException("Storage has already been materialized");
        }
        out.writeBytes(this.source, this.offset, this.length);
    }

    private PackedIntegerArray delegate() {
        PackedIntegerArray array = this.materialized;
        if (array == null) {
            // 这里读的是记录下来的绝对位置, 不依赖切片当前的 readerIndex
            FriendlyByteBuf view = new FriendlyByteBuf(this.source.slice(this.offset, this.length));
            long[] data = new long[longCount(this.elementBits, this.size)];
            if (!VersionHelper.isOrAbove1_21_5) {
                int declared = view.readVarInt();
                if (declared != data.length) {
                    throw new IllegalStateException("Unexpected packed array length: " + declared + " != " + data.length);
                }
            }
            for (int i = 0; i < data.length; i++) {
                data[i] = view.readLong();
            }
            array = new PackedIntegerArray(this.elementBits, this.size, data);
            this.materialized = array;
        }
        return array;
    }

    /** {@link PackedIntegerArray} 内部的长度计算, 提前算出来免得为了知道长度先分配 */
    public static int longCount(int elementBits, int size) {
        int elementsPerLong = 64 / elementBits;
        return (size + elementsPerLong - 1) / elementsPerLong;
    }

    @Override
    public int swap(int index, int value) {
        return delegate().swap(index, value);
    }

    @Override
    public void set(int index, int value) {
        delegate().set(index, value);
    }

    @Override
    public int getAndSet(int index, int value) {
        return delegate().getAndSet(index, value);
    }

    @Override
    public int get(int index) {
        return delegate().get(index);
    }

    @Override
    public long[] getData() {
        return delegate().getData();
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public int getElementBits() {
        return this.elementBits;
    }

    @Override
    public void forEach(IntConsumer action) {
        delegate().forEach(action);
    }

    @Override
    public void writePaletteIndices(int[] out) {
        delegate().writePaletteIndices(out);
    }

    @Override
    public PaletteStorage copy() {
        return delegate().copy();
    }
}
