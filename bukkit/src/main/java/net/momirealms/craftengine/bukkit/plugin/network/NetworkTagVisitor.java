package net.momirealms.craftengine.bukkit.plugin.network;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.sparrow.nbt.*;

import java.util.Collections;
import java.util.Map;

public final class NetworkTagVisitor implements TagVisitor {
    private final LazyReference<Map<String, ComponentProvider>> map = LazyReference.oneTime(Object2ObjectOpenHashMap::new);

    public Map<String, ComponentProvider> visit(Tag element) {
        element.accept(this);
        return this.map.initialized() ? this.map.get() : Collections.emptyMap();
    }

    @Override
    public void visitString(StringTag element) {
        Map<String, ComponentProvider> map = BukkitNetworkManager.instance().matchNetworkTags(element.value());
        if (map != null && !map.isEmpty()) {
            this.map.get().putAll(map);
        }
    }

    @Override
    public void visitByte(ByteTag element) {
    }

    @Override
    public void visitShort(ShortTag element) {
    }

    @Override
    public void visitInt(IntTag element) {
    }

    @Override
    public void visitLong(LongTag element) {
    }

    @Override
    public void visitFloat(FloatTag element) {
    }

    @Override
    public void visitDouble(DoubleTag element) {
    }

    @Override
    public void visitByteArray(ByteArrayTag element) {
    }

    @Override
    public void visitIntArray(IntArrayTag element) {
    }

    @Override
    public void visitLongArray(LongArrayTag element) {
    }

    @Override
    public void visitList(ListTag element) {
        for (Tag tag : element) {
            tag.accept(this);
        }
    }

    @Override
    public void visitCompound(CompoundTag compound) {
        for (Map.Entry<String, Tag> entry : compound.entrySet()) {
            entry.getValue().accept(this);
        }
    }

    @Override
    public void visitEnd(EndTag element) {
    }
}
