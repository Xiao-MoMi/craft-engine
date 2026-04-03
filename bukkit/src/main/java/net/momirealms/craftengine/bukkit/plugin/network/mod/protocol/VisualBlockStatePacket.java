package net.momirealms.craftengine.bukkit.plugin.network.mod.protocol;

import io.netty.handler.codec.DecoderException;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.mod.ModPacket;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public record VisualBlockStatePacket(int[] data) implements ModPacket {
    public static final ResourceKey<NetworkCodec<FriendlyByteBuf, ? extends ModPacket>> TYPE = ResourceKey.create(
            BuiltInRegistries.MOD_PACKET.key().location(), Key.of("craftengine", "visual_block_state")
    );
    public static final NetworkCodec<FriendlyByteBuf, VisualBlockStatePacket> CODEC = ModPacket.codec(
            VisualBlockStatePacket::encode,
            VisualBlockStatePacket::new
    );
    private static final int RLE_THRESHOLD = 3;
    private static final int RLE_TAG = 0;
    private static final int DELTA_TAG = 1;

    private VisualBlockStatePacket(FriendlyByteBuf buf) {
        this(decode(buf));
    }

    private void encode(FriendlyByteBuf buf) {
        encode(buf, this.data);
    }

    private static void encode(FriendlyByteBuf buf, int[] data) {
        if (data.length == 0) {
            buf.writeVarInt(0);
            return;
        }
        buf.writeVarInt(data.length);
        int i = 0;
        int previousValue = 0;
        while (i < data.length) {
            int currentValue = data[i];
            int repeatCount = 1;
            int j = i + 1;
            while (j < data.length && data[j] == currentValue) {
                repeatCount++;
                j++;
            }
            if (repeatCount >= RLE_THRESHOLD) {
                buf.writeVarInt(RLE_TAG);
                buf.writeVarInt(currentValue);
                buf.writeVarInt(repeatCount);
                i += repeatCount;
                previousValue = currentValue;
            } else {
                buf.writeVarInt(DELTA_TAG);
                int delta = currentValue - previousValue;
                buf.writeVarInt(delta);
                previousValue = currentValue;
                i++;
            }
        }
    }

    private static int[] decode(FriendlyByteBuf buf) {
        int length = buf.readVarInt();
        if (length == 0) return new int[0];
        int[] data = new int[length];
        int previousValue = 0;
        int i = 0;
        while (i < length) {
            int tag = buf.readVarInt();
            if (tag == RLE_TAG) {
                int value = buf.readVarInt();
                int count = buf.readVarInt();
                if (i + count > length) throw new DecoderException("RLE count exceeds array bounds");
                for (int j = 0; j < count; j++) data[i++] = value;
                previousValue = value;
            } else if (tag == DELTA_TAG) {
                int delta = buf.readVarInt();
                int currentValue = previousValue + delta;
                data[i++] = currentValue;
                previousValue = currentValue;
            } else {
                throw new DecoderException("Unknown encoding tag: " + tag);
            }
        }
        if (i != length) throw new DecoderException("Decoded length mismatch");
        return data;
    }

    @Override
    public ResourceKey<NetworkCodec<FriendlyByteBuf, ? extends ModPacket>> type() {
        return TYPE;
    }

    public static VisualBlockStatePacket create() {
        int vanillaBlockStateCount = BlockStateUtils.vanillaBlockStateCount();
        int serverSideBlockCount = Config.serverSideBlocks();
        int[] mappings = new int[serverSideBlockCount];
        for (int i = 0; i < serverSideBlockCount; i++) {
            ImmutableBlockState state = BukkitBlockManager.instance().getImmutableBlockStateUnsafe(i + vanillaBlockStateCount);
            if (state.isEmpty()) continue;
            mappings[state.customBlockState().registryId() - vanillaBlockStateCount] = state.visualBlockState().registryId();
        }
        return new VisualBlockStatePacket(mappings);
    }

}
