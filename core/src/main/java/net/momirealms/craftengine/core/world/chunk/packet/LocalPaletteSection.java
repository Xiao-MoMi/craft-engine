package net.momirealms.craftengine.core.world.chunk.packet;

import io.netty.handler.codec.DecoderException;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.IntIdentityList;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.chunk.PalettedContainer;
import org.jetbrains.annotations.Nullable;

public final class LocalPaletteSection {
    private final FriendlyByteBuf source;
    private final int start;
    private final int headerLength;
    private final int bits;
    private final int[] palette;
    private final int elementsPerLong;
    private final int packedStart;
    private final int packedLength;
    private final int biomeStart;
    private final int biomeLength;
    private final boolean hasArrayLength;
    private final IntIdentityList biomeList;
    private PalettedContainer<Integer> biomeContainer;

    private LocalPaletteSection(FriendlyByteBuf source, IntIdentityList biomeList, int headerLength, boolean hasArrayLength) {
        this.source = source;
        this.biomeList = biomeList;
        this.start = source.readerIndex();
        this.headerLength = headerLength;
        this.hasArrayLength = hasArrayLength;
        source.skipBytes(headerLength);
        int packetBits = source.readUnsignedByte();
        this.bits = packetBits == 0 ? 0 : Math.max(4, packetBits);
        this.elementsPerLong = this.bits == 0 ? 0 : 64 / this.bits;
        int paletteSize = this.bits == 0 ? 1 : source.readVarInt();
        if (paletteSize < 1 || paletteSize > 1 << this.bits) {
            throw new DecoderException("Invalid local block palette size: " + paletteSize);
        }
        this.palette = new int[paletteSize];
        for (int i = 0; i < paletteSize; i++) {
            this.palette[i] = source.readVarInt();
        }
        this.packedLength = readStorageLength(source, this.bits, 4096, hasArrayLength);
        this.packedStart = source.readerIndex();
        source.skipBytes(this.packedLength);

        this.biomeStart = source.readerIndex();
        int biomeBits = source.readUnsignedByte();
        if (biomeBits == 0) {
            source.readVarInt();
        } else if (biomeBits <= 3) {
            int size = source.readVarInt();
            for (int i = 0; i < size; i++) {
                source.readVarInt();
            }
        } else {
            biomeBits = MiscUtils.ceilLog2(biomeList.size());
        }
        source.skipBytes(readStorageLength(source, biomeBits, 64, hasArrayLength));
        this.biomeLength = source.readerIndex() - this.biomeStart;
    }

    @Nullable
    public static LocalPaletteSection tryReadPacket(FriendlyByteBuf source, IntIdentityList biomeList) {
        return tryReadPacket(source, biomeList, VersionHelper.isOrAbove26_1, !VersionHelper.isOrAbove1_21_5);
    }

    @Nullable
    static LocalPaletteSection tryReadPacket(FriendlyByteBuf source, IntIdentityList biomeList, boolean hasFluidCount, boolean hasArrayLength) {
        int headerLength = hasFluidCount ? 4 : 2;
        if (source.getUnsignedByte(source.readerIndex() + headerLength) > 8) {
            return null;
        }
        return new LocalPaletteSection(source, biomeList, headerLength, hasArrayLength);
    }

    private static int readStorageLength(FriendlyByteBuf source, int bits, int size, boolean hasArrayLength) {
        int longs = bits == 0 ? 0 : (size + 64 / bits - 1) / (64 / bits);
        if (hasArrayLength && source.readVarInt() != longs) {
            throw new DecoderException("Invalid packed storage length for " + bits + " bits");
        }
        return longs * Long.BYTES;
    }

    public boolean remap(int[] mappings) {
        boolean changed = false;
        for (int i = 0; i < this.palette.length; i++) {
            int state = this.palette[i];
            int mappedState = mappings[state];
            if (state != mappedState) {
                this.palette[i] = mappedState;
                changed = true;
            }
        }
        return changed;
    }

    public int paletteSize() {
        return this.palette.length;
    }

    public int paletteState(int index) {
        return this.palette[index];
    }

    public int blockState(int index) {
        if (this.bits == 0) return this.palette[0];
        int longIndex = index / this.elementsPerLong;
        int shift = (index - longIndex * this.elementsPerLong) * this.bits;
        long packed = this.source.getLong(this.packedStart + longIndex * Long.BYTES);
        return this.palette[(int) (packed >>> shift) & ((1 << this.bits) - 1)];
    }

    public PalettedContainer<Integer> biomeContainer() {
        if (this.biomeContainer == null) {
            this.biomeContainer = new PalettedContainer<>(this.biomeList, 0, PalettedContainer.PaletteProvider.BIOME);
            this.biomeContainer.readPacket(new FriendlyByteBuf(this.source.slice(this.biomeStart, this.biomeLength)));
        }
        return this.biomeContainer;
    }

    public void writePacket(FriendlyByteBuf output) {
        output.writeBytes(this.source, this.start, this.headerLength);
        output.writeByte(this.bits);
        if (this.bits != 0) output.writeVarInt(this.palette.length);
        for (int state : this.palette) {
            output.writeVarInt(state);
        }
        if (this.hasArrayLength) output.writeVarInt(this.packedLength / Long.BYTES);
        output.writeBytes(this.source, this.packedStart, this.packedLength);
        if (this.biomeContainer == null) {
            output.writeBytes(this.source, this.biomeStart, this.biomeLength);
        } else {
            this.biomeContainer.writePacket(output);
        }
    }
}
