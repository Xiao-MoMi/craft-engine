package net.momirealms.craftengine.core.util;

import net.momirealms.sparrow.nbt.*;

import java.util.Map;

public final class StringValueOnlyTagVisitor implements TagVisitor {
    private StringBuilder builder;

    public String visit(Tag element) {
        element.accept(this);
        return this.builder == null ? "" : this.builder.toString();
    }

    @Override
    public void visitString(StringTag element) {
        if (this.builder == null) {
            this.builder = new StringBuilder();
        }
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
