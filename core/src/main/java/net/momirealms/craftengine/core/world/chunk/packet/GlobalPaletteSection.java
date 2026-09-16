package net.momirealms.craftengine.core.world.chunk.packet;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.IntIdentityList;
import org.jetbrains.annotations.Nullable;

public final class GlobalPaletteSection extends PacketSection {
    private final int bits;
    private final int outputBits;
    private final int elementsPerLong;
    private final int mask;
    private final int packedStart;
    private final int packedLength;

    GlobalPaletteSection(FriendlyByteBuf source, IntIdentityList biomeList, int headerLength, boolean hasArrayLength, int bits, int outputBits) {
        super(source, biomeList, headerLength, hasArrayLength);
        source.readByte();
        this.bits = bits;
        this.outputBits = outputBits;
        this.elementsPerLong = 64 / bits;
        this.mask = (1 << bits) - 1;
        this.packedLength = this.readStorageLength(bits, 4096);
        this.packedStart = source.readerIndex();
        source.skipBytes(this.packedLength);
    }

    @Override
    public int sourceBlockState(int index) {
        int longIndex = index / this.elementsPerLong;
        int shift = (index - longIndex * this.elementsPerLong) * this.bits;
        return (int) (this.source.getLong(this.packedStart + longIndex * Long.BYTES) >>> shift) & this.mask;
    }

    @Override
    public void forEachBlockState(BlockStateConsumer consumer) {
        this.scan(null, consumer);
    }

    // Check the output mapping while visiting source states in the same pass.
    public boolean remap(int[] mappings, BlockStateConsumer consumer) {
        boolean changed = this.scan(mappings, consumer);
        this.blockStateMapper = changed ? mappings : null;
        return changed;
    }

    @Override
    protected boolean hasRemappedBlockStates(int[] mappings) {
        return this.scan(mappings, null);
    }

    private boolean scan(int @Nullable [] mappings, @Nullable BlockStateConsumer consumer) {
        boolean changed = false;
        int index = 0;
        int offset = this.packedStart;
        while (index < 4096) {
            long packed = this.source.getLong(offset);
            int end = Math.min(4096, index + this.elementsPerLong);
            while (index < end) {
                int state = (int) packed & this.mask;
                if (!changed && mappings != null && state != mappings[state]) {
                    if (consumer == null) return true;
                    changed = true;
                }
                if (consumer != null) consumer.accept(index, state);
                index++;
                packed >>>= this.bits;
            }
            offset += Long.BYTES;
        }
        return changed;
    }

    @Override
    protected boolean needsBlockStateRewrite() {
        return super.needsBlockStateRewrite() || this.outputBits != this.bits;
    }

    @Override
    protected void writeBlockStates(FriendlyByteBuf output) {
        int outputElementsPerLong = 64 / this.outputBits;
        int outputLongs = (4096 + outputElementsPerLong - 1) / outputElementsPerLong;
        output.writeByte(this.outputBits);
        if (this.hasArrayLength) output.writeVarInt(outputLongs);
        if (this.outputBits == this.bits) {
            if (this.blockStateMapper == null) {
                output.writeBytes(this.source, this.packedStart, this.packedLength);
            } else {
                this.writeMappedBlockStates(output);
            }
            return;
        }
        long outputMask = (1L << this.outputBits) - 1;
        long input = 0;
        int inputRemaining = 0;
        int inputOffset = this.packedStart;
        int remaining = 4096;
        while (remaining > 0) {
            long packed = 0;
            int entries = Math.min(remaining, outputElementsPerLong);
            for (int i = 0; i < entries; i++) {
                if (inputRemaining == 0) {
                    input = this.source.getLong(inputOffset);
                    inputOffset += Long.BYTES;
                    inputRemaining = this.elementsPerLong;
                }
                int state = (int) input & this.mask;
                int mapped = this.blockStateMapper == null ? state : this.blockStateMapper[state];
                packed |= ((long) mapped & outputMask) << (i * this.outputBits);
                input >>>= this.bits;
                inputRemaining--;
            }
            output.writeLong(packed);
            remaining -= entries;
        }
    }

    private void writeMappedBlockStates(FriendlyByteBuf output) {
        int remaining = 4096;
        int offset = this.packedStart;
        while (remaining > 0) {
            long packed = this.source.getLong(offset);
            long input = packed;
            int entries = Math.min(remaining, this.elementsPerLong);
            for (int i = 0; i < entries; i++) {
                int state = (int) input & this.mask;
                int mapped = this.blockStateMapper[state];
                // Unchanged IDs produce a zero delta, preserving their slots without a branch.
                packed ^= ((long) (state ^ mapped) & this.mask) << (i * this.bits);
                input >>>= this.bits;
            }
            output.writeLong(packed);
            offset += Long.BYTES;
            remaining -= entries;
        }
    }
}
