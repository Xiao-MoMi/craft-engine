package net.momirealms.craftengine.core.util;

import com.google.common.collect.Maps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ByteProcessor;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.NBT;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

public class FriendlyByteBuf extends ByteBuf {

    private final ByteBuf source;

    public FriendlyByteBuf(ByteBuf parent) {
        this.source = parent;
    }

    public ByteBuf source() {
        return source;
    }

    public BlockPos readBlockPos(ByteBuf buf) {
        return BlockPos.of(buf.readLong());
    }

    public FriendlyByteBuf writeBlockPos(BlockPos pos) {
        this.writeLong(pos.asLong());
        return this;
    }

    public static int getVarIntSize(int value) {
        for (int shift = 1; shift < 5; ++shift) {
            if ((value & -1 << shift * 7) == 0) {
                return shift;
            }
        }
        return 5;
    }

    public static int getVarLongSize(long value) {
        for (int shift = 1; shift < 10; ++shift) {
            if ((value & -1L << shift * 7) == 0L) {
                return shift;
            }
        }
        return 10;
    }

    public IntList readIntIdList() {
        int listSize = this.readVarInt();
        IntArrayList idList = new IntArrayList();
        for (int i = 0; i < listSize; ++i) {
            idList.add(this.readVarInt());
        }
        return idList;
    }

    public void writeIntIdList(IntList idList) {
        this.writeVarInt(idList.size());
        idList.forEach(this::writeVarInt);
    }

    public <K, V, M extends Map<K, V>> M readMap(IntFunction<M> mapFactory, FriendlyByteBuf.Reader<K> keyReader, FriendlyByteBuf.Reader<V> valueReader) {
        int mapSize = this.readVarInt();
        M map = mapFactory.apply(mapSize);
        for (int i = 0; i < mapSize; ++i) {
            K key = keyReader.apply(this);
            V value = valueReader.apply(this);
            map.put(key, value);
        }
        return map;
    }

    public <K, V> Map<K, V> readMap(FriendlyByteBuf.Reader<K> keyReader, FriendlyByteBuf.Reader<V> valueReader) {
        return this.readMap(Maps::newHashMapWithExpectedSize, keyReader, valueReader);
    }

    public <K, V> void writeMap(Map<K, V> map, FriendlyByteBuf.Writer<K> keyWriter, FriendlyByteBuf.Writer<V> valueWriter) {
        this.writeVarInt(map.size());
        map.forEach((key, value) -> {
            keyWriter.accept(this, key);
            valueWriter.accept(this, value);
        });
    }

    public void readWithCount(Consumer<FriendlyByteBuf> consumer) {
        int count = this.readVarInt();
        for (int i = 0; i < count; ++i) {
            consumer.accept(this);
        }
    }

    public <E extends Enum<E>> void writeEnumSet(EnumSet<E> enumSet, Class<E> enumType) {
        E[] enumConstants = enumType.getEnumConstants();
        BitSet bitSet = new BitSet(enumConstants.length);
        for (int i = 0; i < enumConstants.length; ++i) {
            bitSet.set(i, enumSet.contains(enumConstants[i]));
        }
        this.writeFixedBitSet(bitSet, enumConstants.length);
    }

    public <E extends Enum<E>> EnumSet<E> readEnumSet(Class<E> enumType) {
        E[] enumConstants = enumType.getEnumConstants();
        BitSet bitSet = this.readFixedBitSet(enumConstants.length);
        EnumSet<E> enumSet = EnumSet.noneOf(enumType);
        for (int i = 0; i < enumConstants.length; ++i) {
            if (bitSet.get(i)) {
                enumSet.add(enumConstants[i]);
            }
        }
        return enumSet;
    }

    public <T> void writeOptional(Optional<T> value, FriendlyByteBuf.Writer<T> writer) {
        if (value.isPresent()) {
            this.writeBoolean(true);
            writer.accept(this, value.get());
        } else {
            this.writeBoolean(false);
        }
    }

    public <T> Optional<T> readOptional(FriendlyByteBuf.Reader<T> reader) {
        return this.readBoolean() ? Optional.of(reader.apply(this)) : Optional.empty();
    }

    @Nullable
    public <T> T readNullable(FriendlyByteBuf.Reader<T> reader) {
        return this.readBoolean() ? reader.apply(this) : null;
    }

    public <T> void writeNullable(@Nullable T value, FriendlyByteBuf.Writer<T> writer) {
        if (value != null) {
            this.writeBoolean(true);
            writer.accept(this, value);
        } else {
            this.writeBoolean(false);
        }
    }

    public byte[] readByteArray() {
        return this.readByteArray(this.readableBytes());
    }

    public FriendlyByteBuf writeByteArray(byte[] array) {
        this.writeVarInt(array.length);
        this.writeBytes(array);
        return this;
    }

    public byte[] readByteArray(int maxSize) {
        int arraySize = this.readVarInt();
        if (arraySize > maxSize) {
            throw new DecoderException("ByteArray with size " + arraySize + " is bigger than allowed " + maxSize);
        } else {
            byte[] byteArray = new byte[arraySize];
            this.readBytes(byteArray);
            return byteArray;
        }
    }

    public FriendlyByteBuf writeVarIntArray(int[] array) {
        this.writeVarInt(array.length);
        for (int value : array) {
            this.writeVarInt(value);
        }
        return this;
    }

    public int[] readVarIntArray() {
        return this.readVarIntArray(this.readableBytes());
    }

    public int[] readVarIntArray(int maxSize) {
        int arraySize = this.readVarInt();
        if (arraySize > maxSize) {
            throw new DecoderException("VarIntArray with size " + arraySize + " is bigger than allowed " + maxSize);
        } else {
            int[] array = new int[arraySize];
            for (int i = 0; i < array.length; ++i) {
                array[i] = this.readVarInt();
            }
            return array;
        }
    }

    public FriendlyByteBuf writeLongArray(long[] array) {
        this.writeVarInt(array.length);
        for (long value : array) {
            this.writeLong(value);
        }
        return this;
    }

    public long[] readLongArray() {
        return this.readLongArray(null);
    }

    public long[] readLongArray(long @Nullable [] toArray) {
        return this.readLongArray(toArray, this.readableBytes() / 8);
    }

    public long[] readLongArray(long @Nullable [] toArray, int maxSize) {
        int arraySize = this.readVarInt();
        if (toArray == null || toArray.length != arraySize) {
            if (arraySize > maxSize) {
                throw new DecoderException("LongArray with size " + arraySize + " is bigger than allowed " + maxSize);
            }
            toArray = new long[arraySize];
        }
        for (int i = 0; i < toArray.length; ++i) {
            toArray[i] = this.readLong();
        }
        return toArray;
    }

    public byte[] extractByteBufContents() {
        int size = this.writerIndex();
        byte[] byteArray = new byte[size];
        this.getBytes(0, byteArray);
        return byteArray;
    }

    public int readVarInt() {
        int value = 0;
        int shift = 0;
        byte byteValue;
        do {
            byteValue = this.readByte();
            value |= (byteValue & 127) << shift++ * 7;
            if (shift > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((byteValue & 128) == 128);
        return value;
    }

    public long readVarLong() {
        long value = 0L;
        int shift = 0;
        byte byteValue;
        do {
            byteValue = this.readByte();
            value |= (long) (byteValue & 127) << shift++ * 7;
            if (shift > 10) {
                throw new RuntimeException("VarLong too big");
            }
        } while ((byteValue & 128) == 128);
        return value;
    }

    public FriendlyByteBuf writeUUID(UUID uuid) {
        this.writeLong(uuid.getMostSignificantBits());
        this.writeLong(uuid.getLeastSignificantBits());
        return this;
    }

    public UUID readUUID() {
        return new UUID(this.readLong(), this.readLong());
    }

    public FriendlyByteBuf writeVarInt(int value) {
        while ((value & -128) != 0) {
            this.writeByte((value & 127) | 128);
            value >>>= 7;
        }
        this.writeByte(value & 127);
        return this;
    }

    public FriendlyByteBuf writeVarLong(long value) {
        while ((value & -128L) != 0L) {
            this.writeByte((int) (value & 127L) | 128);
            value >>>= 7;
        }
        this.writeByte((int) (value & 127L));
        return this;
    }

    public FriendlyByteBuf writeNbt(@Nullable CompoundTag compound) {
        if (compound == null) {
            this.writeByte(0);
        } else {
            try {
                NBT.writeCompound(compound, new ByteBufOutputStream(this));
            } catch (IOException e) {
                throw new EncoderException("Failed to write NBT compound: " + e.getMessage(), e);
            }
        }
        return this;
    }

    @Nullable
    public CompoundTag readNbt() {
        int initialIndex = this.readerIndex();
        byte marker = this.readByte();
        if (marker == 0) {
            return null;
        } else {
            this.readerIndex(initialIndex);
            try {
                return NBT.readCompound(new ByteBufInputStream(this));
            } catch (IOException e) {
                throw new EncoderException("Failed to read NBT compound: " + e.getMessage(), e);
            }
        }
    }

    public String readUtf() {
        return this.readUtf(32767);
    }

    public String readUtf(int maxLength) {
        int maxEncodedLength = getMaxEncodedUtfLength(maxLength);
        int length = this.readVarInt();
        if (length > maxEncodedLength) {
            throw new DecoderException("Encoded string length exceeds maximum allowed: " + length + " > " + maxEncodedLength);
        }
        if (length < 0) {
            throw new DecoderException("Encoded string length is negative: " + length);
        }
        String result = this.toString(this.readerIndex(), length, StandardCharsets.UTF_8);
        this.readerIndex(this.readerIndex() + length);
        if (result.length() > maxLength) {
            throw new DecoderException("Decoded string length exceeds maximum allowed: " + result.length() + " > " + maxLength);
        }
        return result;
    }

    public FriendlyByteBuf writeUtf(String string) {
        return this.writeUtf(string, 32767);
    }

    public FriendlyByteBuf writeUtf(String string, int maxLength) {
        if (string.length() > maxLength) {
            throw new EncoderException("String too large (was " + string.length() + " characters, max " + maxLength + ")");
        }
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        int maxEncodedLength = getMaxEncodedUtfLength(maxLength);
        if (bytes.length > maxEncodedLength) {
            throw new EncoderException("Encoded string too large (was " + bytes.length + " bytes, max " + maxEncodedLength + ")");
        }
        this.writeVarInt(bytes.length);
        this.writeBytes(bytes);
        return this;
    }

    private static int getMaxEncodedUtfLength(int decodedLength) {
        return decodedLength * 3;
    }

    public Key readKey() {
        return Key.of(this.readUtf(32767));
    }

    public FriendlyByteBuf writeKey(Key id) {
        this.writeUtf(id.toString());
        return this;
    }

    public BitSet readBitSet() {
        return BitSet.valueOf(this.readLongArray());
    }

    public void writeBitSet(BitSet bitSet) {
        this.writeLongArray(bitSet.toLongArray());
    }

    public BitSet readFixedBitSet(int size) {
        byte[] byteArray = new byte[MCUtils.positiveCeilDiv(size, 8)];
        this.readBytes(byteArray);
        return BitSet.valueOf(byteArray);
    }

    public void writeFixedBitSet(BitSet bitSet, int size) {
        if (bitSet.length() > size) {
            throw new EncoderException("BitSet length exceeds expected size (" + bitSet.length() + " > " + size + ")");
        }
        byte[] byteArray = bitSet.toByteArray();
        this.writeBytes(Arrays.copyOf(byteArray, MCUtils.positiveCeilDiv(size, 8)));
    }

    @FunctionalInterface
    public interface Writer<T> extends BiConsumer<FriendlyByteBuf, T> {

        default Writer<Optional<T>> asOptional() {
            return (buf, optional) -> buf.writeOptional(optional, this);
        }
    }

    @FunctionalInterface
    public interface Reader<T> extends Function<FriendlyByteBuf, T> {

        default Reader<Optional<T>> asOptional() {
            return buf -> buf.readOptional(this);
        }
    }

    @Override
    public int capacity() {
        return this.source.capacity();
    }

    @Override
    public ByteBuf capacity(int i) {
        return this.source.capacity(i);
    }

    @Override
    public int maxCapacity() {
        return this.source.maxCapacity();
    }

    @Override
    public ByteBufAllocator alloc() {
        return this.source.alloc();
    }

    @Override
    @SuppressWarnings("deprecation")
    public ByteOrder order() {
        return this.source.order();
    }

    @Override
    @SuppressWarnings("deprecation")
    public ByteBuf order(ByteOrder byteorder) {
        return this.source.order(byteorder);
    }

    @Override
    public ByteBuf unwrap() {
        return this.source.unwrap();
    }

    @Override
    public boolean isDirect() {
        return this.source.isDirect();
    }

    @Override
    public boolean isReadOnly() {
        return this.source.isReadOnly();
    }

    @Override
    public ByteBuf asReadOnly() {
        return this.source.asReadOnly();
    }

    @Override
    public int readerIndex() {
        return this.source.readerIndex();
    }

    @Override
    public ByteBuf readerIndex(int i) {
        return this.source.readerIndex(i);
    }

    @Override
    public int writerIndex() {
        return this.source.writerIndex();
    }

    @Override
    public ByteBuf writerIndex(int i) {
        return this.source.writerIndex(i);
    }

    @Override
    public ByteBuf setIndex(int i, int j) {
        return this.source.setIndex(i, j);
    }

    @Override
    public int readableBytes() {
        return this.source.readableBytes();
    }

    @Override
    public int writableBytes() {
        return this.source.writableBytes();
    }

    @Override
    public int maxWritableBytes() {
        return this.source.maxWritableBytes();
    }

    @Override
    public boolean isReadable() {
        return this.source.isReadable();
    }

    @Override
    public boolean isReadable(int i) {
        return this.source.isReadable(i);
    }

    @Override
    public boolean isWritable() {
        return this.source.isWritable();
    }

    @Override
    public boolean isWritable(int i) {
        return this.source.isWritable(i);
    }

    @Override
    public ByteBuf clear() {
        return this.source.clear();
    }

    @Override
    public ByteBuf markReaderIndex() {
        return this.source.markReaderIndex();
    }

    @Override
    public ByteBuf resetReaderIndex() {
        return this.source.resetReaderIndex();
    }

    @Override
    public ByteBuf markWriterIndex() {
        return this.source.markWriterIndex();
    }

    @Override
    public ByteBuf resetWriterIndex() {
        return this.source.resetWriterIndex();
    }

    @Override
    public ByteBuf discardReadBytes() {
        return this.source.discardReadBytes();
    }

    @Override
    public ByteBuf discardSomeReadBytes() {
        return this.source.discardSomeReadBytes();
    }

    @Override
    public ByteBuf ensureWritable(int i) {
        return this.source.ensureWritable(i);
    }

    @Override
    public int ensureWritable(int i, boolean flag) {
        return this.source.ensureWritable(i, flag);
    }

    @Override
    public boolean getBoolean(int i) {
        return this.source.getBoolean(i);
    }

    @Override
    public byte getByte(int i) {
        return this.source.getByte(i);
    }

    @Override
    public short getUnsignedByte(int i) {
        return this.source.getUnsignedByte(i);
    }

    @Override
    public short getShort(int i) {
        return this.source.getShort(i);
    }

    @Override
    public short getShortLE(int i) {
        return this.source.getShortLE(i);
    }

    @Override
    public int getUnsignedShort(int i) {
        return this.source.getUnsignedShort(i);
    }

    @Override
    public int getUnsignedShortLE(int i) {
        return this.source.getUnsignedShortLE(i);
    }

    @Override
    public int getMedium(int i) {
        return this.source.getMedium(i);
    }

    @Override
    public int getMediumLE(int i) {
        return this.source.getMediumLE(i);
    }

    @Override
    public int getUnsignedMedium(int i) {
        return this.source.getUnsignedMedium(i);
    }

    @Override
    public int getUnsignedMediumLE(int i) {
        return this.source.getUnsignedMediumLE(i);
    }

    @Override
    public int getInt(int i) {
        return this.source.getInt(i);
    }

    @Override
    public int getIntLE(int i) {
        return this.source.getIntLE(i);
    }

    @Override
    public long getUnsignedInt(int i) {
        return this.source.getUnsignedInt(i);
    }

    @Override
    public long getUnsignedIntLE(int i) {
        return this.source.getUnsignedIntLE(i);
    }

    @Override
    public long getLong(int i) {
        return this.source.getLong(i);
    }

    @Override
    public long getLongLE(int i) {
        return this.source.getLongLE(i);
    }

    @Override
    public char getChar(int i) {
        return this.source.getChar(i);
    }

    @Override
    public float getFloat(int i) {
        return this.source.getFloat(i);
    }

    @Override
    public double getDouble(int i) {
        return this.source.getDouble(i);
    }

    @Override
    public ByteBuf getBytes(int i, ByteBuf bytebuf) {
        return this.source.getBytes(i, bytebuf);
    }

    @Override
    public ByteBuf getBytes(int i, ByteBuf bytebuf, int j) {
        return this.source.getBytes(i, bytebuf, j);
    }

    @Override
    public ByteBuf getBytes(int i, ByteBuf bytebuf, int j, int k) {
        return this.source.getBytes(i, bytebuf, j, k);
    }

    @Override
    public ByteBuf getBytes(int i, byte[] bytes) {
        return this.source.getBytes(i, bytes);
    }

    @Override
    public ByteBuf getBytes(int i, byte[] bytes, int j, int k) {
        return this.source.getBytes(i, bytes, j, k);
    }

    @Override
    public ByteBuf getBytes(int i, ByteBuffer bytebuffer) {
        return this.source.getBytes(i, bytebuffer);
    }

    @Override
    public ByteBuf getBytes(int i, OutputStream outputstream, int j) throws IOException {
        return this.source.getBytes(i, outputstream, j);
    }

    @Override
    public int getBytes(int i, GatheringByteChannel gatheringbytechannel, int j) throws IOException {
        return this.source.getBytes(i, gatheringbytechannel, j);
    }

    @Override
    public int getBytes(int i, FileChannel filechannel, long j, int k) throws IOException {
        return this.source.getBytes(i, filechannel, j, k);
    }

    @Override
    public CharSequence getCharSequence(int i, int j, Charset charset) {
        return this.source.getCharSequence(i, j, charset);
    }

    @Override
    public ByteBuf setBoolean(int i, boolean flag) {
        return this.source.setBoolean(i, flag);
    }

    @Override
    public ByteBuf setByte(int i, int j) {
        return this.source.setByte(i, j);
    }

    @Override
    public ByteBuf setShort(int i, int j) {
        return this.source.setShort(i, j);
    }

    @Override
    public ByteBuf setShortLE(int i, int j) {
        return this.source.setShortLE(i, j);
    }

    @Override
    public ByteBuf setMedium(int i, int j) {
        return this.source.setMedium(i, j);
    }

    @Override
    public ByteBuf setMediumLE(int i, int j) {
        return this.source.setMediumLE(i, j);
    }

    @Override
    public ByteBuf setInt(int i, int j) {
        return this.source.setInt(i, j);
    }

    @Override
    public ByteBuf setIntLE(int i, int j) {
        return this.source.setIntLE(i, j);
    }

    @Override
    public ByteBuf setLong(int i, long j) {
        return this.source.setLong(i, j);
    }

    @Override
    public ByteBuf setLongLE(int i, long j) {
        return this.source.setLongLE(i, j);
    }

    @Override
    public ByteBuf setChar(int i, int j) {
        return this.source.setChar(i, j);
    }

    @Override
    public ByteBuf setFloat(int i, float f) {
        return this.source.setFloat(i, f);
    }

    @Override
    public ByteBuf setDouble(int i, double d0) {
        return this.source.setDouble(i, d0);
    }

    @Override
    public ByteBuf setBytes(int i, ByteBuf bytebuf) {
        return this.source.setBytes(i, bytebuf);
    }

    @Override
    public ByteBuf setBytes(int i, ByteBuf bytebuf, int j) {
        return this.source.setBytes(i, bytebuf, j);
    }

    @Override
    public ByteBuf setBytes(int i, ByteBuf bytebuf, int j, int k) {
        return this.source.setBytes(i, bytebuf, j, k);
    }

    @Override
    public ByteBuf setBytes(int i, byte[] bytes) {
        return this.source.setBytes(i, bytes);
    }

    @Override
    public ByteBuf setBytes(int i, byte[] bytes, int j, int k) {
        return this.source.setBytes(i, bytes, j, k);
    }

    @Override
    public ByteBuf setBytes(int i, ByteBuffer bytebuffer) {
        return this.source.setBytes(i, bytebuffer);
    }

    @Override
    public int setBytes(int i, InputStream inputstream, int j) throws IOException {
        return this.source.setBytes(i, inputstream, j);
    }

    @Override
    public int setBytes(int i, ScatteringByteChannel scatteringbytechannel, int j) throws IOException {
        return this.source.setBytes(i, scatteringbytechannel, j);
    }

    @Override
    public int setBytes(int i, FileChannel filechannel, long j, int k) throws IOException {
        return this.source.setBytes(i, filechannel, j, k);
    }

    @Override
    public ByteBuf setZero(int i, int j) {
        return this.source.setZero(i, j);
    }

    @Override
    public int setCharSequence(int i, CharSequence charsequence, Charset charset) {
        return this.source.setCharSequence(i, charsequence, charset);
    }

    @Override
    public boolean readBoolean() {
        return this.source.readBoolean();
    }

    @Override
    public byte readByte() {
        return this.source.readByte();
    }

    @Override
    public short readUnsignedByte() {
        return this.source.readUnsignedByte();
    }

    @Override
    public short readShort() {
        return this.source.readShort();
    }

    @Override
    public short readShortLE() {
        return this.source.readShortLE();
    }

    @Override
    public int readUnsignedShort() {
        return this.source.readUnsignedShort();
    }

    @Override
    public int readUnsignedShortLE() {
        return this.source.readUnsignedShortLE();
    }

    @Override
    public int readMedium() {
        return this.source.readMedium();
    }

    @Override
    public int readMediumLE() {
        return this.source.readMediumLE();
    }

    @Override
    public int readUnsignedMedium() {
        return this.source.readUnsignedMedium();
    }

    @Override
    public int readUnsignedMediumLE() {
        return this.source.readUnsignedMediumLE();
    }

    @Override
    public int readInt() {
        return this.source.readInt();
    }

    @Override
    public int readIntLE() {
        return this.source.readIntLE();
    }

    @Override
    public long readUnsignedInt() {
        return this.source.readUnsignedInt();
    }

    @Override
    public long readUnsignedIntLE() {
        return this.source.readUnsignedIntLE();
    }

    @Override
    public long readLong() {
        return this.source.readLong();
    }

    @Override
    public long readLongLE() {
        return this.source.readLongLE();
    }

    @Override
    public char readChar() {
        return this.source.readChar();
    }

    @Override
    public float readFloat() {
        return this.source.readFloat();
    }

    @Override
    public double readDouble() {
        return this.source.readDouble();
    }

    @Override
    public ByteBuf readBytes(int i) {
        return this.source.readBytes(i);
    }

    @Override
    public ByteBuf readSlice(int i) {
        return this.source.readSlice(i);
    }

    @Override
    public ByteBuf readRetainedSlice(int i) {
        return this.source.readRetainedSlice(i);
    }

    @Override
    public ByteBuf readBytes(ByteBuf bytebuf) {
        return this.source.readBytes(bytebuf);
    }

    @Override
    public ByteBuf readBytes(ByteBuf bytebuf, int i) {
        return this.source.readBytes(bytebuf, i);
    }

    @Override
    public ByteBuf readBytes(ByteBuf bytebuf, int i, int j) {
        return this.source.readBytes(bytebuf, i, j);
    }

    @Override
    public ByteBuf readBytes(byte[] bytes) {
        return this.source.readBytes(bytes);
    }

    @Override
    public ByteBuf readBytes(byte[] bytes, int i, int j) {
        return this.source.readBytes(bytes, i, j);
    }

    @Override
    public ByteBuf readBytes(ByteBuffer bytebuffer) {
        return this.source.readBytes(bytebuffer);
    }

    @Override
    public ByteBuf readBytes(OutputStream outputstream, int i) throws IOException {
        return this.source.readBytes(outputstream, i);
    }

    @Override
    public int readBytes(GatheringByteChannel gatheringbytechannel, int i) throws IOException {
        return this.source.readBytes(gatheringbytechannel, i);
    }

    @Override
    public CharSequence readCharSequence(int i, Charset charset) {
        return this.source.readCharSequence(i, charset);
    }

    @Override
    public int readBytes(FileChannel filechannel, long i, int j) throws IOException {
        return this.source.readBytes(filechannel, i, j);
    }

    @Override
    public ByteBuf skipBytes(int i) {
        return this.source.skipBytes(i);
    }

    @Override
    public ByteBuf writeBoolean(boolean flag) {
        return this.source.writeBoolean(flag);
    }

    @Override
    public ByteBuf writeByte(int i) {
        return this.source.writeByte(i);
    }

    @Override
    public ByteBuf writeShort(int i) {
        return this.source.writeShort(i);
    }

    @Override
    public ByteBuf writeShortLE(int i) {
        return this.source.writeShortLE(i);
    }

    @Override
    public ByteBuf writeMedium(int i) {
        return this.source.writeMedium(i);
    }

    @Override
    public ByteBuf writeMediumLE(int i) {
        return this.source.writeMediumLE(i);
    }

    @Override
    public ByteBuf writeInt(int i) {
        return this.source.writeInt(i);
    }

    @Override
    public ByteBuf writeIntLE(int i) {
        return this.source.writeIntLE(i);
    }

    @Override
    public ByteBuf writeLong(long i) {
        return this.source.writeLong(i);
    }

    @Override
    public ByteBuf writeLongLE(long i) {
        return this.source.writeLongLE(i);
    }

    @Override
    public ByteBuf writeChar(int i) {
        return this.source.writeChar(i);
    }

    @Override
    public ByteBuf writeFloat(float f) {
        return this.source.writeFloat(f);
    }

    @Override
    public ByteBuf writeDouble(double d0) {
        return this.source.writeDouble(d0);
    }

    @Override
    public ByteBuf writeBytes(ByteBuf bytebuf) {
        return this.source.writeBytes(bytebuf);
    }

    @Override
    public ByteBuf writeBytes(ByteBuf bytebuf, int i) {
        return this.source.writeBytes(bytebuf, i);
    }

    @Override
    public ByteBuf writeBytes(ByteBuf bytebuf, int i, int j) {
        return this.source.writeBytes(bytebuf, i, j);
    }

    @Override
    public ByteBuf writeBytes(byte[] bytes) {
        return this.source.writeBytes(bytes);
    }

    @Override
    public ByteBuf writeBytes(byte[] bytes, int i, int j) {
        return this.source.writeBytes(bytes, i, j);
    }

    @Override
    public ByteBuf writeBytes(ByteBuffer bytebuffer) {
        return this.source.writeBytes(bytebuffer);
    }

    @Override
    public int writeBytes(InputStream inputstream, int i) throws IOException {
        return this.source.writeBytes(inputstream, i);
    }

    @Override
    public int writeBytes(ScatteringByteChannel scatteringbytechannel, int i) throws IOException {
        return this.source.writeBytes(scatteringbytechannel, i);
    }

    @Override
    public int writeBytes(FileChannel filechannel, long i, int j) throws IOException {
        return this.source.writeBytes(filechannel, i, j);
    }

    @Override
    public ByteBuf writeZero(int i) {
        return this.source.writeZero(i);
    }

    @Override
    public int writeCharSequence(CharSequence charsequence, Charset charset) {
        return this.source.writeCharSequence(charsequence, charset);
    }

    @Override
    public int indexOf(int i, int j, byte b) {
        return this.source.indexOf(i, j, b);
    }

    @Override
    public int bytesBefore(byte b) {
        return this.source.bytesBefore(b);
    }

    @Override
    public int bytesBefore(int i, byte b) {
        return this.source.bytesBefore(i, b);
    }

    @Override
    public int bytesBefore(int i, int j, byte b) {
        return this.source.bytesBefore(i, j, b);
    }

    @Override
    public int forEachByte(ByteProcessor byteprocessor) {
        return this.source.forEachByte(byteprocessor);
    }

    @Override
    public int forEachByte(int i, int j, ByteProcessor byteprocessor) {
        return this.source.forEachByte(i, j, byteprocessor);
    }

    @Override
    public int forEachByteDesc(ByteProcessor byteprocessor) {
        return this.source.forEachByteDesc(byteprocessor);
    }

    @Override
    public int forEachByteDesc(int i, int j, ByteProcessor byteprocessor) {
        return this.source.forEachByteDesc(i, j, byteprocessor);
    }

    @Override
    public ByteBuf copy() {
        return this.source.copy();
    }

    @Override
    public ByteBuf copy(int i, int j) {
        return this.source.copy(i, j);
    }

    @Override
    public ByteBuf slice() {
        return this.source.slice();
    }

    @Override
    public ByteBuf retainedSlice() {
        return this.source.retainedSlice();
    }

    @Override
    public ByteBuf slice(int i, int j) {
        return this.source.slice(i, j);
    }

    @Override
    public ByteBuf retainedSlice(int i, int j) {
        return this.source.retainedSlice(i, j);
    }

    @Override
    public ByteBuf duplicate() {
        return this.source.duplicate();
    }

    @Override
    public ByteBuf retainedDuplicate() {
        return this.source.retainedDuplicate();
    }

    @Override
    public int nioBufferCount() {
        return this.source.nioBufferCount();
    }

    @Override
    public ByteBuffer nioBuffer() {
        return this.source.nioBuffer();
    }

    @Override
    public ByteBuffer nioBuffer(int i, int j) {
        return this.source.nioBuffer(i, j);
    }

    @Override
    public ByteBuffer internalNioBuffer(int i, int j) {
        return this.source.internalNioBuffer(i, j);
    }

    @Override
    public ByteBuffer[] nioBuffers() {
        return this.source.nioBuffers();
    }

    @Override
    public ByteBuffer[] nioBuffers(int i, int j) {
        return this.source.nioBuffers(i, j);
    }

    @Override
    public boolean hasArray() {
        return this.source.hasArray();
    }

    @Override
    public byte[] array() {
        return this.source.array();
    }

    @Override
    public int arrayOffset() {
        return this.source.arrayOffset();
    }

    @Override
    public boolean hasMemoryAddress() {
        return this.source.hasMemoryAddress();
    }

    @Override
    public long memoryAddress() {
        return this.source.memoryAddress();
    }

    @Override
    public String toString(Charset charset) {
        return this.source.toString(charset);
    }

    @Override
    public @NotNull String toString(int i, int j, Charset charset) {
        return this.source.toString(i, j, charset);
    }

    @Override
    public int hashCode() {
        return this.source.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof FriendlyByteBuf friendlyByteBuf)) return false;
        return this.source.equals(friendlyByteBuf.source);
    }

    @Override
    public int compareTo(ByteBuf bytebuf) {
        return this.source.compareTo(bytebuf);
    }

    @Override
    public String toString() {
        return this.source.toString();
    }

    @Override
    public ByteBuf retain(int i) {
        return this.source.retain(i);
    }

    @Override
    public ByteBuf retain() {
        return this.source.retain();
    }

    @Override
    public ByteBuf touch() {
        return this.source.touch();
    }

    @Override
    public ByteBuf touch(Object object) {
        return this.source.touch(object);
    }

    @Override
    public int refCnt() {
        return this.source.refCnt();
    }

    @Override
    public boolean release() {
        return this.source.release();
    }

    @Override
    public boolean release(int i) {
        return this.source.release(i);
    }
}
