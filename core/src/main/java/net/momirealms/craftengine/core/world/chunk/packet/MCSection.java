package net.momirealms.craftengine.core.world.chunk.packet;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.IndexedIterable;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.chunk.PalettedContainer;

public final class MCSection {
    private short nonEmptyBlockCount;
    private short fluidCount;
    private final PalettedContainer<Integer> serverBlockStateContainer;
    private final IndexedIterable<Integer> clientBlockStateList;
    private PalettedContainer<Integer> biomeContainer;

    public MCSection(IndexedIterable<Integer> clientBlockStateList, IndexedIterable<Integer> serverBlockStateList, IndexedIterable<Integer> biomeList) {
        this.serverBlockStateContainer = new PalettedContainer<>(serverBlockStateList, 0, PalettedContainer.PaletteProvider.BLOCK_STATE);
        this.biomeContainer = new PalettedContainer<>(biomeList, 0, PalettedContainer.PaletteProvider.BIOME);
        this.clientBlockStateList = clientBlockStateList;
    }

    public void readPacket(FriendlyByteBuf buf) {
        readPacket(buf, false);
    }

    /**
     * @param deferStorage 方块容器的打包数据先不解码, 只在真的需要逐方块访问时才解码, 写回时没被解码过
     *                     就原样拷贝。延迟存储会引用底层网络缓冲, 因此只在本包处理期间有效。
     *                     <p>
     *                     生物群系容器<b>不</b>延迟: 它会通过公开的 BiomeRemapper 接口交给第三方,
     *                     那里没法保证不会被留到本包之后。而且它只有64个格子, 延迟省不下多少,
     *                     不值得给一个公开扩展点加上这种生命周期约束。
     */
    public void readPacket(FriendlyByteBuf buf, boolean deferStorage) {
        this.nonEmptyBlockCount = buf.readShort();
        if (VersionHelper.isOrAbove26_1) this.fluidCount = buf.readShort();
        this.serverBlockStateContainer.readPacket(buf, deferStorage);
        PalettedContainer<Integer> palettedContainer = this.biomeContainer.slice();
        palettedContainer.readPacket(buf);
        this.biomeContainer = palettedContainer;
    }

    public void writePacket(FriendlyByteBuf buf) {
        buf.writeShort(this.nonEmptyBlockCount);
        if (VersionHelper.isOrAbove26_1) buf.writeShort(this.fluidCount);
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
        return this.serverBlockStateContainer;
    }

    public PalettedContainer<Integer> biomeContainer() {
        return this.biomeContainer;
    }
}
