package net.momirealms.craftengine.core.world.chunk;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.momirealms.craftengine.core.block.EmptyBlockDefinition;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.IndexedIterable;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.LongStream;

public final class PalettedContainer<T> implements PaletteResizeListener<T>, ReadableContainer<T> {
    private static final VarHandle DATA_HANDLE;

    static {
        try {
            DATA_HANDLE = MethodHandles.lookup().findVarHandle(PalettedContainer.class, "data", Data.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final BiConsumer<FriendlyByteBuf, long[]> RAW_DATA_WRITER = VersionHelper.isOrAbove1_21_5 ?
            (FriendlyByteBuf::writeFixedSizeLongArray) : (FriendlyByteBuf::writeLongArray);
    private static final BiConsumer<FriendlyByteBuf, long[]> RAW_DATA_READER = VersionHelper.isOrAbove1_21_5 ?
            (FriendlyByteBuf::readFixedSizeLongArray) : (FriendlyByteBuf::readLongArray);
    private static final ThreadLocal<PaletteReadoutCache> READOUT_CACHE = ThreadLocal.withInitial(PaletteReadoutCache::new);
    private final IndexedIterable<T> idList;
    // Publish replacements with volatile writes; hot-path reads use Leaf's acquire access.
    private volatile Data<T> data;
    private final PaletteProvider paletteProvider;

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
            return singularPalette.get(0) == EmptyBlockDefinition.STATE;
        }
        return false;
    }

    public synchronized PalettedContainer<T> getClientCompatiblePalettedContainer(IndexedIterable<T> idList) {
        Palette<T> palette = this.data.palette;
        if (!(palette instanceof IdListPalette<T> idListPalette)) {
            return this;
        }
        if (this.data.storage.getElementBits() == MiscUtils.ceilLog2(idList.size())) {
            return this;
        }
        Data<T> newData = getCompatibleData(this.data, idList, 128);
        newData.importFrom(idListPalette, this.data.storage);
        return new PalettedContainer<>(idList, PaletteProvider.BLOCK_STATE, newData);
    }

    public Data<T> data() {
        return data;
    }

    public synchronized void readPacket(FriendlyByteBuf buf) {
        int i = buf.readByte();
        Data<T> data = this.getCompatibleData(this.data, i);
        data.palette.readPacket(buf);
        RAW_DATA_READER.accept(buf, data.storage.getData());
        this.data = data;
    }

    @Override
    public synchronized void writePacket(FriendlyByteBuf buf) {
        this.data.writePacket(buf);
    }

    private Data<T> getCompatibleData(@Nullable Data<T> previousData, int bits) {
        DataProvider<T> dataProvider = this.paletteProvider.createDataProvider(this.idList, bits);
        if (previousData != null && dataProvider.equals(previousData.configuration())) {
            return previousData;
        } else {
            return dataProvider.createData(this.idList, this, this.paletteProvider.getContainerSize());
        }
    }

    private Data<T> getCompatibleData(@Nullable Data<T> previousData, IndexedIterable<T> idList, int bits) {
        DataProvider<T> dataProvider = this.paletteProvider.createDataProvider(idList, bits);
        return previousData != null && dataProvider.equals(previousData.configuration()) ? previousData : dataProvider.createData(this.idList, this, this.paletteProvider.getContainerSize());
    }

    @Override
    public synchronized int onResize(int i, T object) {
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

    @SuppressWarnings("unchecked")
    public T get(int index) {
        Data<T> data = (Data<T>) DATA_HANDLE.getAcquire(this);
        return data.palette.get(data.storage.get(index));
    }

    public synchronized T getAndSet(int index, T state) {
        int i = this.data.palette.index(state);
        // index() can resize the palette and replace data.
        Data<T> data = this.data;
        int preIndex = data.storage.getAndSet(index, i);
        return data.palette.get(preIndex);
    }

    public void set(int x, int y, int z, T value) {
        this.set(this.paletteProvider.computeIndex(x, y, z), value);
    }

    public synchronized void set(int index, T value) {
        int i = this.data.palette.index(value);
        this.data.storage.set(index, i);
    }

    // Caller must hold this container's monitor or have exclusive ownership.
    public T swapUnsafe(int x, int y, int z, T value) {
        return this.swap(this.paletteProvider.computeIndex(x, y, z), value);
    }

    public synchronized T swap(int x, int y, int z, T value) {
        return this.swap(this.paletteProvider.computeIndex(x, y, z), value);
    }

    private T swap(int index, T value) {
        int i = this.data.palette.index(value);
        Data<T> data = this.data;
        int j = data.storage.swap(index, i);
        return data.palette.get(j);
    }

    @Override
    public void forEachValue(Consumer<T> action) {
        Data<T> data = this.data;
        Palette<T> palette = data.palette();
        IntSet intSet = new IntArraySet();
        data.storage.forEach(intSet::add);
        intSet.forEach((id) -> action.accept(palette.get(id)));
    }

    @Override
    public boolean hasAny(Predicate<T> predicate) {
        return this.data.palette.hasAny(predicate);
    }

    @Override
    public void count(Counter<T> counter) {
        Data<T> data = this.data;
        int paletteSize = data.palette.getSize();
        if (paletteSize == 1) {
            counter.accept(data.palette.get(0), data.storage.size());
        } else if (paletteSize <= 4096) {
            int[] counts = new int[paletteSize];
            data.storage.forEach(id -> ++counts[id]);
            for (int id = 0; id < paletteSize; ++id) {
                if (counts[id] != 0) {
                    counter.accept(data.palette.get(id), counts[id]);
                }
            }
        } else {
            Int2IntOpenHashMap frequencyMap = new Int2IntOpenHashMap();
            data.storage.forEach(key -> frequencyMap.addTo(key, 1));
            frequencyMap.int2IntEntrySet().forEach(entry ->
                    counter.accept(data.palette.get(entry.getIntKey()), entry.getIntValue())
            );
        }
    }

    @Override
    public synchronized PalettedContainer<T> copy() {
        return new PalettedContainer<>(this);
    }

    @Override
    public PalettedContainer<T> slice() {
        return new PalettedContainer<>(this.idList, this.data.palette.get(0), this.paletteProvider);
    }

    @Override
    public synchronized Serialized<T> serialize(IndexedIterable<T> idList, PaletteProvider paletteProvider) {
        Data<T> data = this.data;
        Palette<T> palette = data.palette;
        PaletteStorage storage = data.storage;
        int containerSize = paletteProvider.getContainerSize();
        if (storage.getElementBits() == 0 || palette.getSize() == 1) {
            int bits = paletteProvider.getBits(idList, 1);
            Optional<LongStream> packed = bits == 0 ? Optional.empty()
                    : Optional.of(Arrays.stream(new PackedIntegerArray(bits, containerSize).getData()));
            return new Serialized<>(List.of(palette.get(0)), packed);
        }

        PaletteReadoutCache cache = READOUT_CACHE.get();
        List<T> entries = cache.read(storage, palette);
        int bits = paletteProvider.getBits(idList, entries.size());
        if (bits == 0) {
            return new Serialized<>(entries, Optional.empty());
        }

        long[] packed;
        if (entries.size() == palette.getSize() && bits == storage.getElementBits()) {
            // Every entry is used: retain the original IDs and copy the packed words.
            for (int i = 0; i < entries.size(); i++) {
                entries.set(i, palette.get(i));
            }
            packed = storage.getData().clone();
        } else {
            packed = cache.repack(bits, containerSize);
        }
        return new Serialized<>(entries, Optional.of(Arrays.stream(packed)));
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
                    default -> new DataProvider<>(PaletteProvider.ID_LIST, MiscUtils.ceilLog2(idList.size()));
                };
            }
        };
        public static final PaletteProvider BIOME = new PaletteProvider(2) {
            public <A> DataProvider<A> createDataProvider(IndexedIterable<A> idList, int bits) {
                return switch (bits) {
                    case 0 -> new DataProvider<>(SINGULAR, bits);
                    case 1, 2, 3 -> new DataProvider<>(ARRAY, bits);
                    default -> new DataProvider<>(PaletteProvider.ID_LIST, MiscUtils.ceilLog2(idList.size()));
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
            int i = MiscUtils.ceilLog2(size);
            DataProvider<A> dataProvider = this.createDataProvider(idList, i);
            return dataProvider.factory() == ID_LIST ? i : dataProvider.bits();
        }
    }

    public record DataProvider<T>(Palette.Factory factory, int bits) {
        public Data<T> createData(IndexedIterable<T> idList, PaletteResizeListener<T> listener, int size) {
            PaletteStorage paletteStorage = this.bits == 0 ? EmptyPaletteStorage.forSize(size) : new PackedIntegerArray(this.bits, size);
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
        long[] storage = paletteProvider.getBits(idList, list.size()) == 0 ? null : serialized.storage().map(LongStream::toArray).orElse(null);
        return read(idList, paletteProvider, list, storage);
    }

    public static <T> PalettedContainer<T> read(IndexedIterable<T> idList, PaletteProvider paletteProvider, List<T> list, @Nullable long[] storage) {
        int containerSize = paletteProvider.getContainerSize();
        int bits = paletteProvider.getBits(idList, list.size());
        DataProvider<T> dataProvider = paletteProvider.createDataProvider(idList, bits);
        PaletteStorage paletteStorage;
        if (bits == 0) {
            paletteStorage = EmptyPaletteStorage.forSize(containerSize);
        } else {
            if (storage == null) {
                return null;
            }
            try {
                if (dataProvider.factory() == PalettedContainer.PaletteProvider.ID_LIST) {
                    Palette<T> palette = new BiMapPalette<>(idList, bits, (id, value) -> 0, list);
                    PackedIntegerArray packedIntegerArray = new PackedIntegerArray(bits, containerSize, storage);
                    int[] is = new int[containerSize];
                    packedIntegerArray.writePaletteIndices(is);
                    applyEach(is, (id) -> idList.getRawId(palette.get(id)));
                    paletteStorage = new PackedIntegerArray(dataProvider.bits(), containerSize, is);
                } else {
                    paletteStorage = new PackedIntegerArray(dataProvider.bits(), containerSize, storage);
                }
            } catch (PackedIntegerArray.InvalidLengthException e) {
                CraftEngine.instance().logger().warn("Failed to read PalettedContainer", e);
                return null;
            }
        }
        return new PalettedContainer<>(idList, paletteProvider, dataProvider, paletteStorage, list);
    }
}
