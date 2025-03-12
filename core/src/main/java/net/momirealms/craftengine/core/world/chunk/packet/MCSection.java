package net.momirealms.craftengine.core.world.chunk.packet;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.IndexedIterable;
import net.momirealms.craftengine.core.world.chunk.PalettedContainer;
import net.momirealms.craftengine.core.world.chunk.ReadableContainer;

public class MCSection {
    private short nonEmptyBlockCount;
    private final PalettedContainer<Integer> blockStateContainer;
    private ReadableContainer<Integer> biomeContainer;

    public MCSection(IndexedIterable<Integer> blockStateList, IndexedIterable<Integer> biomeList) {
        this.blockStateContainer = new PalettedContainer<>(blockStateList, 0, PalettedContainer.PaletteProvider.BLOCK_STATE);
        this.biomeContainer = new PalettedContainer<>(biomeList, 0, PalettedContainer.PaletteProvider.BIOME);
    }

    public void readPacket(FriendlyByteBuf buf) {
        this.nonEmptyBlockCount = buf.readShort();
        this.blockStateContainer.readPacket(buf);
        PalettedContainer<Integer> palettedContainer = this.biomeContainer.slice();
        palettedContainer.readPacket(buf);
        this.biomeContainer = palettedContainer;
    }

    public void writePacket(FriendlyByteBuf buf) {
        buf.writeShort(this.nonEmptyBlockCount);
        this.blockStateContainer.writePacket(buf);
        this.biomeContainer.writePacket(buf);
    }

    public void setBlockState(int x, int y, int z, int state) {
        this.blockStateContainer.set(x, y, z, state);
    }

    public int getBlockState(int x, int y, int z) {
        return this.blockStateContainer.get(x, y, z);
    }

    public PalettedContainer<Integer> blockStateContainer() {
        return blockStateContainer;
    }
}
