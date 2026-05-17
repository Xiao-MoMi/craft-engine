package net.momirealms.craftengine.core.item.network;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.StringUtils;
import net.momirealms.craftengine.core.util.TriConsumer;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.ByteTag;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

public interface NetworkItemHandler {
    Operation[] BY_INDEX = new Operation[] {Operation.ADD, Operation.REMOVE, Operation.RESET};
    String NETWORK_ITEM_TAG = "craftengine:network_data";
    String NETWORK_OPERATION = "type";
    String NETWORK_VALUE = "value";

    Optional<Item> s2c(Item itemStack, @Nullable Player player);

    Optional<Item> c2s(Item itemStack);

    static CompoundTag pack(Operation operation, @Nullable Tag value) {
        if (value == null) {
            return new CompoundTag(Map.of(NETWORK_OPERATION, operation.tag()));
        } else {
            return new CompoundTag(Map.of(NETWORK_OPERATION, operation.tag(), NETWORK_VALUE, value));
        }
    }

    static CompoundTag pack(Operation operation) {
        return new CompoundTag(Map.of(NETWORK_OPERATION, operation.tag()));
    }

    static void apply(String tagPath, CompoundTag networkData, Item item) {
        byte index = networkData.getByte(NETWORK_OPERATION);
        Operation operation = BY_INDEX[index];
        operation.consumer.accept(item, tagPath, operation == Operation.ADD ? networkData.get(NETWORK_VALUE) : null);
    }

    enum Operation {
        ADD(0, Item::setSparrowTagComponent, (i, s, t) -> i.setTag(t, (Object[]) StringUtils.splitByDot(s))),
        REMOVE(1, (i, s, t) -> i.removeComponent(s), (i, s, t) -> i.removeTag((Object[]) StringUtils.splitByDot(s))),
        RESET(2, (i, s, t) -> i.resetComponent(s), (i, s, t) -> i.removeTag((Object[]) StringUtils.splitByDot(s)));

        private final int id;
        private final ByteTag tag;
        private final TriConsumer<Item, String, Tag> consumer;

        Operation(int id, TriConsumer<Item, String, Tag> componentConsumer, TriConsumer<Item, String, Tag> nbtConsumer) {
            this.id = id;
            this.tag = new ByteTag((byte) id);
            this.consumer = VersionHelper.isOrAbove1_20_5 ? componentConsumer : nbtConsumer;
        }

        public int id() {
            return this.id;
        }

        public ByteTag tag() {
            return tag;
        }
    }
}
