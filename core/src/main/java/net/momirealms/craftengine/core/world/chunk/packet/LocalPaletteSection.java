package net.momirealms.craftengine.core.world.chunk.packet;

import io.netty.handler.codec.DecoderException;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.IntIdentityList;

public final class LocalPaletteSection extends PacketSection {
    private final int bits;
    private final int[] palette;
    private final int elementsPerLong;
    private final int packedStart;
    private final int packedLength;

    LocalPaletteSection(FriendlyByteBuf source, IntIdentityList biomeList, int headerLength, boolean hasArrayLength) {
        super(source, biomeList, headerLength, hasArrayLength);
        this.bits = Math.max(4, source.readUnsignedByte());
        this.elementsPerLong = 64 / this.bits;
        int paletteSize = source.readVarInt();
        if (paletteSize < 1 || paletteSize > 1 << this.bits) {
            throw new DecoderException("Invalid local block palette size: " + paletteSize);
        }
        this.palette = new int[paletteSize];
        for (int i = 0; i < paletteSize; i++) {
            this.palette[i] = source.readVarInt();
        }
        this.packedLength = this.readStorageLength(this.bits, 4096);
        this.packedStart = source.readerIndex();
        source.skipBytes(this.packedLength);
    }

    @Override
    protected boolean hasRemappedBlockStates(int[] mappings) {
        for (int state : this.palette) {
            if (state != mappings[state]) return true;
        }
        return false;
    }

    public int paletteSize() {
        return this.palette.length;
    }

    public int sourcePaletteState(int index) {
        return this.palette[index];
    }

    @Override
    public int sourceBlockState(int index) {
        int longIndex = index / this.elementsPerLong;
        int shift = (index - longIndex * this.elementsPerLong) * this.bits;
        long packed = this.source.getLong(this.packedStart + longIndex * Long.BYTES);
        return this.palette[(int) (packed >>> shift) & ((1 << this.bits) - 1)];
    }

    @Override
    public void forEachBlockState(BlockStateConsumer consumer) {
        int mask = (1 << this.bits) - 1;
        int index = 0;
        int offset = this.packedStart;
        while (index < 4096) {
            long packed = this.source.getLong(offset);
            int end = Math.min(4096, index + this.elementsPerLong);
            while (index < end) {
                consumer.accept(index++, this.palette[(int) packed & mask]);
                packed >>>= this.bits;
            }
            offset += Long.BYTES;
        }
    }

    @Override
    protected void writeBlockStates(FriendlyByteBuf output) {
        output.writeByte(this.bits);
        output.writeVarInt(this.palette.length);
        for (int state : this.palette) {
            output.writeVarInt(this.blockStateMapper == null ? state : this.blockStateMapper[state]);
        }
        if (this.hasArrayLength) output.writeVarInt(this.packedLength / Long.BYTES);
        output.writeBytes(this.source, this.packedStart, this.packedLength);
    }
}
