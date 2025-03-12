package net.momirealms.craftengine.core.world.chunk;

import java.util.function.IntConsumer;

public class PackedIntegerArray implements PaletteStorage {
    private static final int[] INDEX_PARAMETERS = new int[]{-1, -1, 0, Integer.MIN_VALUE, 0, 0, 1431655765, 1431655765, 0, Integer.MIN_VALUE, 0, 1, 858993459, 858993459, 0, 715827882, 715827882, 0, 613566756, 613566756, 0, Integer.MIN_VALUE, 0, 2, 477218588, 477218588, 0, 429496729, 429496729, 0, 390451572, 390451572, 0, 357913941, 357913941, 0, 330382099, 330382099, 0, 306783378, 306783378, 0, 286331153, 286331153, 0, Integer.MIN_VALUE, 0, 3, 252645135, 252645135, 0, 238609294, 238609294, 0, 226050910, 226050910, 0, 214748364, 214748364, 0, 204522252, 204522252, 0, 195225786, 195225786, 0, 186737708, 186737708, 0, 178956970, 178956970, 0, 171798691, 171798691, 0, 165191049, 165191049, 0, 159072862, 159072862, 0, 153391689, 153391689, 0, 148102320, 148102320, 0, 143165576, 143165576, 0, 138547332, 138547332, 0, Integer.MIN_VALUE, 0, 4, 130150524, 130150524, 0, 126322567, 126322567, 0, 122713351, 122713351, 0, 119304647, 119304647, 0, 116080197, 116080197, 0, 113025455, 113025455, 0, 110127366, 110127366, 0, 107374182, 107374182, 0, 104755299, 104755299, 0, 102261126, 102261126, 0, 99882960, 99882960, 0, 97612893, 97612893, 0, 95443717, 95443717, 0, 93368854, 93368854, 0, 91382282, 91382282, 0, 89478485, 89478485, 0, 87652393, 87652393, 0, 85899345, 85899345, 0, 84215045, 84215045, 0, 82595524, 82595524, 0, 81037118, 81037118, 0, 79536431, 79536431, 0, 78090314, 78090314, 0, 76695844, 76695844, 0, 75350303, 75350303, 0, 74051160, 74051160, 0, 72796055, 72796055, 0, 71582788, 71582788, 0, 70409299, 70409299, 0, 69273666, 69273666, 0, 68174084, 68174084, 0, Integer.MIN_VALUE, 0, 5};
    private final long[] data;
    private final int elementBits;
    private final long maxValue;
    private final int size;
    private final int elementsPerLong;
    private final int indexScale;
    private final int indexOffset;
    private final int indexShift;

    public PackedIntegerArray(int elementBits, int size, int[] data) {
        this(elementBits, size);
        int i = 0;
        int j;
        for (j = 0; j <= size - this.elementsPerLong; j += this.elementsPerLong) {
            long l = 0L;
            for(int k = this.elementsPerLong - 1; k >= 0; --k) {
                l <<= elementBits;
                l |= (long)data[j + k] & this.maxValue;
            }
            this.data[i++] = l;
        }
        int m = size - j;
        if (m > 0) {
            long n = 0L;
            for (int o = m - 1; o >= 0; --o) {
                n <<= elementBits;
                n |= (long)data[j + o] & this.maxValue;
            }
            this.data[i] = n;
        }
    }

    public PackedIntegerArray(int elementBits, int size) {
        this(elementBits, size, (long[]) null);
    }

    public PackedIntegerArray(int elementBits, int size, long[] data) {
        this.size = size;
        this.elementBits = elementBits;
        this.maxValue = (1L << elementBits) - 1L;
        this.elementsPerLong = (char)(64 / elementBits);
        int i = 3 * (this.elementsPerLong - 1);
        this.indexScale = INDEX_PARAMETERS[i + 0];
        this.indexOffset = INDEX_PARAMETERS[i + 1];
        this.indexShift = INDEX_PARAMETERS[i + 2];
        int j = (size + this.elementsPerLong - 1) / this.elementsPerLong;
        if (data != null) {
            if (data.length != j) {
                throw new RuntimeException("Invalid length given for storage, got: " + data.length + " but expected: " + j);
            }
            this.data = data;
        } else {
            this.data = new long[j];
        }
    }

    private int getStorageIndex(int index) {
        long l = Integer.toUnsignedLong(this.indexScale);
        long m = Integer.toUnsignedLong(this.indexOffset);
        return (int)((long)index * l + m >> 32 >> this.indexShift);
    }

    @Override
    public int swap(int index, int value) {
        int i = this.getStorageIndex(index);
        long l = this.data[i];
        int j = (index - i * this.elementsPerLong) * this.elementBits;
        int k = (int)(l >> j & this.maxValue);
        this.data[i] = l & ~(this.maxValue << j) | ((long)value & this.maxValue) << j;
        return k;
    }

    @Override
    public void set(int index, int value) {
        int i = this.getStorageIndex(index);
        long l = this.data[i];
        int j = (index - i * this.elementsPerLong) * this.elementBits;
        this.data[i] = l & ~(this.maxValue << j) | ((long)value & this.maxValue) << j;
    }

    @Override
    public int get(int index) {
        int i = this.getStorageIndex(index);
        long l = this.data[i];
        int j = (index - i * this.elementsPerLong) * this.elementBits;
        return (int)(l >> j & this.maxValue);
    }

    @Override
    public long[] getData() {
        return this.data;
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
        int i = 0;
        long[] var3 = this.data;
        int var4 = var3.length;

        for (int var5 = 0; var5 < var4; ++var5) {
            long l = var3[var5];

            for(int j = 0; j < this.elementsPerLong; ++j) {
                action.accept((int)(l & this.maxValue));
                l >>= this.elementBits;
                ++i;
                if (i >= this.size) {
                    return;
                }
            }
        }

    }

    @Override
    public void writePaletteIndices(int[] out) {
        int i = this.data.length;
        int j = 0;

        int k;
        long l;
        int m;
        for(k = 0; k < i - 1; ++k) {
            l = this.data[k];

            for(m = 0; m < this.elementsPerLong; ++m) {
                out[j + m] = (int)(l & this.maxValue);
                l >>= this.elementBits;
            }

            j += this.elementsPerLong;
        }

        k = this.size - j;
        if (k > 0) {
            l = this.data[i - 1];

            for(m = 0; m < k; ++m) {
                out[j + m] = (int)(l & this.maxValue);
                l >>= this.elementBits;
            }
        }

    }

    @Override
    public PaletteStorage copy() {
        return new PackedIntegerArray(this.elementBits, this.size, this.data.clone());
    }

    public static class InvalidLengthException extends RuntimeException {
        InvalidLengthException(String message) {
            super(message);
        }
    }
}
