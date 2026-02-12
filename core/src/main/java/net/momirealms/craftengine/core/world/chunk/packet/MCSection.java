package net.momirealms.craftengine.core.world.chunk.packet;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.IndexedIterable;
import net.momirealms.craftengine.core.world.chunk.PalettedContainer;

public final class MCSection {
    private short nonEmptyBlockCount;
    private final PalettedContainer<Integer> serverBlockStateContainer;
    private final IndexedIterable<Integer> clientBlockStateList;
    private PalettedContainer<Integer> biomeContainer;

    public MCSection(IndexedIterable<Integer> clientBlockStateList, IndexedIterable<Integer> serverBlockStateList, IndexedIterable<Integer> biomeList) {
        this.serverBlockStateContainer = new PalettedContainer<>(serverBlockStateList, 0, PalettedContainer.PaletteProvider.BLOCK_STATE);
        this.biomeContainer = new PalettedContainer<>(biomeList, 0, PalettedContainer.PaletteProvider.BIOME);
        this.clientBlockStateList = clientBlockStateList;
    }

    public void readPacket(FriendlyByteBuf buf) {
        this.nonEmptyBlockCount = buf.readShort();
        this.serverBlockStateContainer.readPacket(buf);
        PalettedContainer<Integer> palettedContainer = this.biomeContainer.slice();
        palettedContainer.readPacket(buf);
        this.biomeContainer = palettedContainer;
    }

    public void writePacket(FriendlyByteBuf buf) {
        buf.writeShort(this.nonEmptyBlockCount);
        this.serverBlockStateContainer.getClientCompatiblePalettedContainer(this.clientBlockStateList).writePacket(buf);
        this.biomeContainer.writePacket(buf);
    }

    public void setBlockState(int x, int y, int z, int state) {
        this.serverBlockStateContainer.set(x, y, z, state);
    }

    public int getBlockState(int x, int y, int z) {
        return this.serverBlockStateContainer.get(x, y, z);
    }

    public PalettedContainer<Integer> blockStateContainer() {
        return serverBlockStateContainer;
    }

    public PalettedContainer<Integer> biomeContainer() {
        return biomeContainer;
    }
}
