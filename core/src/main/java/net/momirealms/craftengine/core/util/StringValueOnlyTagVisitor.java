package net.momirealms.craftengine.core.util;

import net.momirealms.sparrow.nbt.*;

import java.util.Map;

public final class StringValueOnlyTagVisitor implements TagVisitor  {
    private final StringBuilder builder = new StringBuilder();

    public String visit(Tag element) {
        element.accept(this);
        return this.builder.toString();
    }

    @Override
    public void visitString(StringTag element) {
        this.builder.append(element.getAsString());
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
            this.builder.append((new StringValueOnlyTagVisitor()).visit(tag));
        }
    }

    @Override
    public void visitCompound(CompoundTag compound) {
        for (Map.Entry<String, Tag> entry : compound.entrySet()) {
            this.builder.append((new StringValueOnlyTagVisitor()).visit(entry.getValue()));
        }
    }

    @Override
    public void visitEnd(EndTag element) {
    }
}
