package net.momirealms.craftengine.core.world.chunk;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.momirealms.craftengine.core.block.EmptyBlock;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.IndexedIterable;
import net.momirealms.craftengine.core.util.MCUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.LongStream;

public class PalettedContainer<T> implements PaletteResizeListener<T>, ReadableContainer<T> {
    public static boolean NEED_DOWNGRADE = true;
    private static final BiConsumer<FriendlyByteBuf, long[]> RAW_DATA_WRITER = VersionHelper.isOrAbove1_21_5() ?
            (FriendlyByteBuf::writeFixedSizeLongArray) : (FriendlyByteBuf::writeLongArray);
    private static final BiConsumer<FriendlyByteBuf, long[]> RAW_DATA_READER = VersionHelper.isOrAbove1_21_5() ?
            (FriendlyByteBuf::readFixedSizeLongArray) : (FriendlyByteBuf::readLongArray);
    private final PaletteResizeListener<T> dummyListener = (newSize, added) -> 0;
    private final IndexedIterable<T> idList;
    private Data<T> data;
    private final PaletteProvider paletteProvider;
    private final Lock lock = new ReentrantLock();

    public void lock() {
        this.lock.lock();
    }

    public void unlock() {
        this.lock.unlock();
    }

    public PalettedContainer(IndexedIterable<T> idList, PaletteProvider paletteProvider, DataProvider<T> dataProvider, PaletteStorage storage, List<T> paletteEntries) {
        this.idList = idList;
        this.paletteProvider = paletteProvider;
        this.data = new Data<>(dataProvider, storage, dataProvider.factory().create(dataProvider.bits(), idList, this, paletteEntries));
    }

    private PalettedContainer(IndexedIterable<T> idList, PaletteProvider paletteProvider, Data<T> data) {
        this.idList = idList;
        this.paletteProvider = paletteProvider;
        this.data = data;
    }

    private PalettedContainer(PalettedContainer<T> container) {
        this.idList = container.idList;
        this.paletteProvider = container.paletteProvider;
        this.data = container.data.copy(this);
    }

    public PalettedContainer(IndexedIterable<T> idList, T object, PaletteProvider paletteProvider) {
        this.paletteProvider = paletteProvider;
        this.idList = idList;
        this.data = this.getCompatibleData(null, 0);
        this.data.palette.index(object);
    }

    public boolean isEmpty() {
        Data<T> data = this.data;
        if (data.palette instanceof SingularPalette<T> singularPalette) {
            return singularPalette.get(0) == EmptyBlock.STATE;
        }
        return false;
    }

    public PalettedContainer<T> downgradeTo(IndexedIterable<T> idList) {
        if (!NEED_DOWNGRADE) {
            return this;
        }
        Palette<T> palette = this.data.palette;
        if (!(palette instanceof IdListPalette<T> idListPalette)) {
            return this;
        }
        Data<T> newData = getCompatibleData(this.data, idList, 128);
        newData.importFrom(idListPalette, this.data.storage);
        return new PalettedContainer<>(idList, PaletteProvider.BLOCK_STATE, newData);
    }

    public Data<T> data() {
        return data;
    }

    public void readPacket(FriendlyByteBuf buf) {
        int i = buf.readByte();
        Data<T> data = this.getCompatibleData(this.data, i);
        data.palette.readPacket(buf);
        RAW_DATA_READER.accept(buf, data.storage.getData());
        this.data = data;
    }

    @Override
    public void writePacket(FriendlyByteBuf buf) {
        this.data.writePacket(buf);
    }

    private Data<T> getCompatibleData(@Nullable Data<T> previousData, int bits) {
        DataProvider<T> dataProvider = this.paletteProvider.createDataProvider(this.idList, bits);
        return previousData != null && dataProvider.equals(previousData.configuration()) ? previousData : dataProvider.createData(this.idList, this, this.paletteProvider.getContainerSize());
    }

    private Data<T> getCompatibleData(@Nullable Data<T> previousData, IndexedIterable<T> idList, int bits) {
        DataProvider<T> dataProvider = this.paletteProvider.createDataProvider(idList, bits);
        return previousData != null && dataProvider.equals(previousData.configuration()) ? previousData : dataProvider.createData(this.idList, this, this.paletteProvider.getContainerSize());
    }

    @Override
    public int onResize(int i, T object) {
        Data<T> oldData = this.data;
        Data<T> newData = this.getCompatibleData(oldData, i);
        newData.importFrom(oldData.palette, oldData.storage);
        this.data = newData;
        return newData.palette.index(object);
    }

    @Override
    public T get(int x, int y, int z) {
        return this.get(this.paletteProvider.computeIndex(x, y, z));
    }

    public T get(int index) {
        Data<T> data = this.data;
        return data.palette.get(data.storage.get(index));
    }

    public T getAndSet(int index, T state) {
        this.lock();
        try {
            int i = this.data.palette.index(state);
            int preIndex = this.data.storage.getAndSet(index, i);
            return this.data.palette.get(preIndex);
        } finally {
            this.unlock();
        }
    }

    public void set(int x, int y, int z, T value) {
        this.lock();
        try {
            this.set(this.paletteProvider.computeIndex(x, y, z), value);
        } finally {
            this.unlock();
        }
    }

    public void set(int index, T value) {
        int i = this.data.palette.index(value);
        this.data.storage.set(index, i);
    }

    public T swapUnsafe(int x, int y, int z, T value) {
        return this.swap(this.paletteProvider.computeIndex(x, y, z), value);
    }

    public T swap(int x, int y, int z, T value) {
        this.lock();
        T previous;
        try {
            previous = this.swap(this.paletteProvider.computeIndex(x, y, z), value);
        } finally {
            this.unlock();
        }
        return previous;
    }

    private T swap(int index, T value) {
        int i = this.data.palette.index(value);
        int j = this.data.storage.swap(index, i);
        return this.data.palette.get(j);
    }

    @Override
    public void forEachValue(Consumer<T> action) {
        Palette<T> palette = this.data.palette();
        IntSet intSet = new IntArraySet();
        this.data.storage.forEach(intSet::add);
        intSet.forEach((id) -> action.accept(palette.get(id)));
    }

    @Override
    public boolean hasAny(Predicate<T> predicate) {
        return this.data.palette.hasAny(predicate);
    }

    @Override
    public void count(Counter<T> counter) {
        int paletteSize = this.data.palette.getSize();
        if (paletteSize == 1) {
            counter.accept(this.data.palette.get(0), this.data.storage.size());
        } else {
            Int2IntOpenHashMap frequencyMap = new Int2IntOpenHashMap();
            this.data.storage.forEach(key -> frequencyMap.addTo(key, 1));
            frequencyMap.int2IntEntrySet().forEach(entry ->
                    counter.accept(this.data.palette.get(entry.getIntKey()), entry.getIntValue())
            );
        }
    }

    @Override
    public PalettedContainer<T> copy() {
        return new PalettedContainer<>(this);
    }

    @Override
    public PalettedContainer<T> slice() {
        return new PalettedContainer<>(this.idList, this.data.palette.get(0), this.paletteProvider);
    }

    @Override
    public Serialized<T> serialize(IndexedIterable<T> idList, PaletteProvider paletteProvider) {
        this.lock();
        try {
            BiMapPalette<T> biMapPalette = new BiMapPalette<>(idList, this.data.storage.getElementBits(), this.dummyListener);
            int containerSize = paletteProvider.getContainerSize();
            int[] paletteIndices = new int[containerSize];
            this.data.storage.writePaletteIndices(paletteIndices);
            applyEach(paletteIndices, (id) -> biMapPalette.index(this.data.palette.get(id)));
            int bitsRequired = paletteProvider.getBits(idList, biMapPalette.getSize());
            Optional<LongStream> packedData = (bitsRequired != 0)
                    ? Optional.of(Arrays.stream(new PackedIntegerArray(bitsRequired, containerSize, paletteIndices).getData()))
                    : Optional.empty();
            return new Serialized<>(biMapPalette.getElements(), packedData);
        } finally {
            this.unlock();
        }
    }

    private static void applyEach(int[] values, IntUnaryOperator applier) {
        int previousValue = -1;
        int transformedValue = -1;
        for (int index = 0; index < values.length; ++index) {
            int currentValue = values[index];
            if (currentValue != previousValue) {
                previousValue = currentValue;
                transformedValue = applier.applyAsInt(currentValue);
            }
            values[index] = transformedValue;
        }
    }

    public abstract static class PaletteProvider {
        public static final Palette.Factory SINGULAR = SingularPalette::create;
        public static final Palette.Factory ARRAY = ArrayPalette::create;
        public static final Palette.Factory BI_MAP = BiMapPalette::create;
        public static final Palette.Factory ID_LIST = IdListPalette::create;
        public static final PaletteProvider CUSTOM_BLOCK_STATE = new PaletteProvider(4) {
            public <A> DataProvider<A> createDataProvider(IndexedIterable<A> idList, int bits) {
                return switch (bits) {
                    case 0 -> new DataProvider<>(SINGULAR, bits);
                    case 1, 2, 3, 4 -> new DataProvider<>(ARRAY, 4);
                    default -> new DataProvider<>(BI_MAP, bits);
                };
            }
        };
        public static final PaletteProvider BLOCK_STATE = new PaletteProvider(4) {
            public <A> DataProvider<A> createDataProvider(IndexedIterable<A> idList, int bits) {
                return switch (bits) {
                    case 0 -> new DataProvider<>(SINGULAR, bits);
                    case 1, 2, 3, 4 -> new DataProvider<>(ARRAY, 4);
                    case 5, 6, 7, 8 -> new DataProvider<>(BI_MAP, bits);
                    default -> new DataProvider<>(PaletteProvider.ID_LIST, MCUtils.ceilLog2(idList.size()));
                };
            }
        };
        public static final PaletteProvider BIOME = new PaletteProvider(2) {
            public <A> DataProvider<A> createDataProvider(IndexedIterable<A> idList, int bits) {
                return switch (bits) {
                    case 0 -> new DataProvider<>(SINGULAR, bits);
                    case 1, 2, 3 -> new DataProvider<>(ARRAY, bits);
                    default -> new DataProvider<>(PaletteProvider.ID_LIST, MCUtils.ceilLog2(idList.size()));
                };
            }
        };

        private final int edgeBits;

        private PaletteProvider(int edgeBits) {
            this.edgeBits = edgeBits;
        }

        public int getContainerSize() {
            return 1 << this.edgeBits * 3;
        }

        public int computeIndex(int x, int y, int z) {
            return (y << this.edgeBits | z) << this.edgeBits | x;
        }

        public abstract <A> DataProvider<A> createDataProvider(IndexedIterable<A> idList, int bits);

        <A> int getBits(IndexedIterable<A> idList, int size) {
            int i = MCUtils.ceilLog2(size);
            DataProvider<A> dataProvider = this.createDataProvider(idList, i);
            return dataProvider.factory() == ID_LIST ? i : dataProvider.bits();
        }
    }

    public record DataProvider<T>(Palette.Factory factory, int bits) {
        public Data<T> createData(IndexedIterable<T> idList, PaletteResizeListener<T> listener, int size) {
            PaletteStorage paletteStorage = this.bits == 0 ? new EmptyPaletteStorage(size) : new PackedIntegerArray(this.bits, size);
            Palette<T> palette = this.factory.create(this.bits, idList, listener, List.of());
            return new Data<>(this, paletteStorage, palette);
        }
    }

    public record Data<T>(DataProvider<T> configuration, PaletteStorage storage, Palette<T> palette) {
        public void importFrom(Palette<T> palette, PaletteStorage storage) {
            for (int i = 0; i < storage.size(); ++i) {
                T object = palette.get(storage.get(i));
                this.storage.set(i, this.palette.index(object));
            }
        }

        public Data<T> copy(PaletteResizeListener<T> resizeListener) {
            return new Data<>(this.configuration, this.storage.copy(), this.palette.copy(resizeListener));
        }

        public void writePacket(FriendlyByteBuf buf) {
            buf.writeByte(this.storage.getElementBits());
            this.palette.writePacket(buf);
            RAW_DATA_WRITER.accept(buf, this.storage.getData());
        }
    }

    @FunctionalInterface
    public interface Counter<T> {
        void accept(T object, int count);
    }

    public static <T> PalettedContainer<T> read(IndexedIterable<T> idList, PaletteProvider paletteProvider, ReadableContainer.Serialized<T> serialized) {
        List<T> list = serialized.paletteEntries();
        int containerSize = paletteProvider.getContainerSize();
        int bits = paletteProvider.getBits(idList, list.size());
        DataProvider<T> dataProvider = paletteProvider.createDataProvider(idList, bits);
        PaletteStorage paletteStorage;
        if (bits == 0) {
            paletteStorage = new EmptyPaletteStorage(containerSize);
        } else {
            Optional<LongStream> optional = serialized.storage();
            if (optional.isEmpty()) {
                return null;
            }
            long[] ls = optional.get().toArray();
            try {
                if (dataProvider.factory() == PalettedContainer.PaletteProvider.ID_LIST) {
                    Palette<T> palette = new BiMapPalette<>(idList, bits, (id, value) -> 0, list);
                    PackedIntegerArray packedIntegerArray = new PackedIntegerArray(bits, containerSize, ls);
                    int[] is = new int[containerSize];
                    packedIntegerArray.writePaletteIndices(is);
                    applyEach(is, (id) -> idList.getRawId(palette.get(id)));
                    paletteStorage = new PackedIntegerArray(dataProvider.bits(), containerSize, is);
                } else {
                    paletteStorage = new PackedIntegerArray(dataProvider.bits(), containerSize, ls);
                }
            } catch (PackedIntegerArray.InvalidLengthException e) {
                CraftEngine.instance().logger().warn("Failed to read PalettedContainer", e);
                return null;
            }
        }
        return new PalettedContainer<>(idList, paletteProvider, dataProvider, paletteStorage, list);
    }
}
