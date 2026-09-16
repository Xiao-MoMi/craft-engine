package net.momirealms.craftengine.core.world.chunk.packet;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.IntIdentityList;

public final class SingleValueSection extends PacketSection {
    private final int state;

    SingleValueSection(FriendlyByteBuf source, IntIdentityList biomeList, int headerLength, boolean hasArrayLength) {
        super(source, biomeList, headerLength, hasArrayLength);
        source.readByte();
        this.state = source.readVarInt();
        this.readStorageLength(0, 4096);
    }

    @Override
    public int sourceBlockState(int index) {
        return this.state;
    }

    @Override
    protected boolean hasRemappedBlockStates(int[] mappings) {
        return this.state != mappings[this.state];
    }

    @Override
    public void forEachBlockState(BlockStateConsumer consumer) {
        for (int i = 0; i < 4096; i++) {
            consumer.accept(i, this.state);
        }
    }

    @Override
    protected void writeBlockStates(FriendlyByteBuf output) {
        output.writeByte(0);
        output.writeVarInt(this.blockStateMapper == null ? this.state : this.blockStateMapper[this.state]);
        if (this.hasArrayLength) output.writeVarInt(0);
    }
}
