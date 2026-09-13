package net.momirealms.craftengine.core.world.chunk.packet;

import io.netty.handler.codec.DecoderException;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.IntIdentityList;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.chunk.PalettedContainer;

/**
 * A section view valid while its source packet buffer remains readable.
 * Block accessors return source IDs; remapping only affects serialization.
 */
public abstract sealed class PacketSection permits SingleValueSection, LocalPaletteSection, GlobalPaletteSection {
    protected final FriendlyByteBuf source;
    protected final boolean hasArrayLength;
    private final int start;
    private final int headerLength;
    private final IntIdentityList biomeList;
    protected int[] blockStateMapper;
    private int biomeStart;
    private int biomeLength;
    private PalettedContainer<Integer> biomeContainer;
    private boolean biomesChanged;

    protected PacketSection(FriendlyByteBuf source, IntIdentityList biomeList, int headerLength, boolean hasArrayLength) {
        this.source = source;
        this.biomeList = biomeList;
        this.start = source.readerIndex();
        this.headerLength = headerLength;
        this.hasArrayLength = hasArrayLength;
        source.skipBytes(headerLength);
    }

    public static PacketSection readPacket(FriendlyByteBuf source, IntIdentityList serverBlockList, IntIdentityList clientBlockList, IntIdentityList biomeList) {
        return readPacket(source, serverBlockList, clientBlockList, biomeList, VersionHelper.isOrAbove26_1, !VersionHelper.isOrAbove1_21_5);
    }

    static PacketSection readPacket(FriendlyByteBuf source, IntIdentityList serverBlockList, IntIdentityList clientBlockList, IntIdentityList biomeList,
                                    boolean hasFluidCount, boolean hasArrayLength) {
        int headerLength = hasFluidCount ? 4 : 2;
        int bits = source.getUnsignedByte(source.readerIndex() + headerLength);
        PacketSection section;
        if (bits == 0) {
            section = new SingleValueSection(source, biomeList, headerLength, hasArrayLength);
        } else if (bits <= 8) {
            section = new LocalPaletteSection(source, biomeList, headerLength, hasArrayLength);
        } else {
            section = new GlobalPaletteSection(source, biomeList, headerLength, hasArrayLength,
                    MiscUtils.ceilLog2(serverBlockList.size()), MiscUtils.ceilLog2(clientBlockList.size()));
        }
        section.readBiomes();
        return section;
    }

    protected final int readStorageLength(int bits, int size) {
        int longs = bits == 0 ? 0 : (size + 64 / bits - 1) / (64 / bits);
        if (this.hasArrayLength && this.source.readVarInt() != longs) {
            throw new DecoderException("Invalid packed storage length for " + bits + " bits");
        }
        return longs * Long.BYTES;
    }

    private void readBiomes() {
        this.biomeStart = this.source.readerIndex();
        int bits = this.source.readUnsignedByte();
        if (bits == 0) {
            this.source.readVarInt();
        } else if (bits <= 3) {
            int size = this.source.readVarInt();
            for (int i = 0; i < size; i++) {
                this.source.readVarInt();
            }
        } else {
            bits = MiscUtils.ceilLog2(this.biomeList.size());
        }
        this.source.skipBytes(this.readStorageLength(bits, 64));
        this.biomeLength = this.source.readerIndex() - this.biomeStart;
    }

    // Call markBiomesChanged() after modifying the returned container.
    public final PalettedContainer<Integer> biomeContainer() {
        if (this.biomeContainer == null) {
            this.biomeContainer = new PalettedContainer<>(this.biomeList, 0, PalettedContainer.PaletteProvider.BIOME);
            this.biomeContainer.readPacket(new FriendlyByteBuf(this.source.slice(this.biomeStart, this.biomeLength)));
        }
        return this.biomeContainer;
    }

    public final void markBiomesChanged() {
        this.biomesChanged = true;
    }

    public final boolean remap(int[] mappings) {
        boolean changed = this.hasRemappedBlockStates(mappings);
        this.blockStateMapper = changed ? mappings : null;
        return changed;
    }

    public abstract int sourceBlockState(int index);

    public abstract void forEachBlockState(BlockStateConsumer consumer);

    protected abstract boolean hasRemappedBlockStates(int[] mappings);

    protected abstract void writeBlockStates(FriendlyByteBuf output);

    protected boolean needsBlockStateRewrite() {
        return this.blockStateMapper != null;
    }

    public final void writePacket(FriendlyByteBuf output) {
        if (!this.biomesChanged && !this.needsBlockStateRewrite()) {
            output.writeBytes(this.source, this.start, this.biomeStart + this.biomeLength - this.start);
            return;
        }
        output.writeBytes(this.source, this.start, this.headerLength);
        this.writeBlockStates(output);
        if (!this.biomesChanged) {
            output.writeBytes(this.source, this.biomeStart, this.biomeLength);
        } else {
            this.biomeContainer.writePacket(output);
        }
    }

    @FunctionalInterface
    public interface BlockStateConsumer {
        void accept(int index, int state);
    }
}
